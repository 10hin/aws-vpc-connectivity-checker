package in._10h.java.awsvpcconnectivitychecker.backend.k8sclient;

import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1PodList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

    public Mono<V1PodList> listNamespacedPod(final String namespace) {
        return Mono.create((sink) -> {
            final ApiCallback<V1PodList> callback = K8sApiCallback.of((podList, status, headers) -> {
                if (status / 100 == 2) {
                    sink.success(podList);
                    return;
                }
                sink.error(new ErrorStatusException(status));
            });
            try {
                this.coreV1Client.listNamespacedPodAsync(
                        namespace,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Boolean.FALSE,
                        callback
                );
            } catch (ApiException e) {
                sink.error(e);
            }
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
