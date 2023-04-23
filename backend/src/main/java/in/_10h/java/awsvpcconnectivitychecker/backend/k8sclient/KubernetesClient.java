package in._10h.java.awsvpcconnectivitychecker.backend.k8sclient;

import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ListMeta;
import io.kubernetes.client.openapi.models.V1PodList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Component
public class KubernetesClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClient.class);

    private final CoreV1Api coreV1Client;

    @Autowired
    public KubernetesClient(
            final CoreV1Api coreV1Client
    ) {

        this.coreV1Client = coreV1Client;

    }

    public Flux<V1PodList> listNamespacedPod(final String namespace) {
        return Flux.create(emitter -> {
            // Current requested but not emitted count.
            // `count` may become negative
            final var count = new AtomicLong(0L);
            // continue token for next recursive call
            final var nextToken = new AtomicReference<String>();
            // Is this stream done?
            final var done = new AtomicBoolean(false);
            // Gatekeeper to avoid two or more different recursive fetch call happens concurrently
            final var serializer = new Semaphore(1);

            // Avoid incomplete Consumer object reference, indirectly reference it using AtomicReference
            final var fetchPodListWithToken = new AtomicReference<Consumer<String>>((token) -> {});
            fetchPodListWithToken.set(token -> {
                final ApiCallback<V1PodList> callback = K8sApiCallback.of(
                        (podList, status, headers) -> {
                            nextToken.set(
                                    Optional.of(podList)
                                            .map(V1PodList::getMetadata)
                                            .map(V1ListMeta::getContinue)
                                            .orElse(null)
                            );
                            // When canceled or disposed this Flux, count may set to zero concurrently.
                            // Therefore, `remainingCount` may be negative.
                            final long remainingCount = count.decrementAndGet();
                            emitter.next(podList);
                            if (nextToken.get() != null && remainingCount > 0) {
                                fetchPodListWithToken.get().accept(nextToken.get());
                            } else {
                                if (nextToken.get() == null) {
                                    // `done` must be set *before* `serializer` release
                                    done.set(true);
                                    emitter.complete();
                                }
                                serializer.release();
                            }
                        },
                        (ex, status, headers) -> {
                            emitter.error(ex);
                        }
                );
                try {
                    this.coreV1Client.listNamespacedPodAsync(
                            namespace,
                            null,
                            null,
                            token,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            Boolean.FALSE,
                            callback
                    );
                } catch (final ApiException ex) {
                    emitter.error(ex);
                }
            });

            emitter.onRequest(n -> {
                assert n > 0;
                count.addAndGet(n); // anyone can count up count concurrently

                // When recursion chain completes, we set `done` *before* releasing `serializer`
                // So, we need to acquire *before* checking `done`.
                // To avoid "re-start already completed stream"
                if (serializer.tryAcquire() && !done.get()) {

                    // only one thread start fetchPodListWithToken's recursion
                    fetchPodListWithToken.get().accept(nextToken.get());

                }
            });
            emitter.onCancel(() -> {

                // Interrupt recursion chain
                nextToken.set(null);
                count.set(0); // `decrement` happens *just after* this `set`, `count` may become negative.

                // Set stream statuses completed if possible

                // When recursion chain completes, we set `done` *before* releasing `serializer`
                // So, we need to acquire *before* checking `done`.
                // To avoid "re-start already completed stream"
                if (serializer.tryAcquire() && !done.get()) {
                    done.set(true);
                    serializer.release();
                }

            });
            emitter.onDispose(() -> {

                // Interrupt recursion chain
                nextToken.set(null);
                count.set(0); // `decrement` happens *just after* this `set`, `count` may become negative.

                // Set stream statuses completed if possible

                // When recursion chain completes, we set `done` *before* releasing `serializer`
                // So, we need to acquire *before* checking `done`.
                // To avoid "re-start already completed stream"
                if (serializer.tryAcquire() && !done.get()) {
                    done.set(true);
                    serializer.release();
                }
            });
        });
    }
    public static class ErrorStatusException extends RuntimeException {
        private final int status;
        public ErrorStatusException(final int status) {
            super();
            this.status = status;
        }
        public ErrorStatusException(final int status, final String message) {
            super(message);
            this.status = status;
        }
        public ErrorStatusException(final int status, final Throwable cause) {
            super(cause);
            this.status = status;
        }
        public ErrorStatusException(final int status, final String message, final Throwable cause) {
            super(message, cause);
            this.status = status;
        }
        protected ErrorStatusException(
                final int status,
                final String message,
                final Throwable cause,
                final boolean enableSuppression,
                final boolean enableWritableStackTrace
        ) {
            super(message, cause, enableSuppression, enableWritableStackTrace);
            this.status = status;
        }
    }
}
