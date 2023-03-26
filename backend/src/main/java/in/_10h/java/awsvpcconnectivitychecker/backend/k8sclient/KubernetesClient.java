package in._10h.java.awsvpcconnectivitychecker.backend.k8sclient;

import in._10h.java.awsvpcconnectivitychecker.backend.k8sclient.K8sApiCallback;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

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

    public Flux<V1Pod> listNamespacedPod(final String namespace) {
        return Flux.create((sink) -> {
            final ApiCallback<V1PodList> callback = K8sApiCallback.of((podList, status, headers) -> {
                for (var pod : podList.getItems()) {
                    sink.next(pod);
                }
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
}
