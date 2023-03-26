package in._10h.java.awsvpcconnectivitychecker.backend;

import in._10h.java.awsvpcconnectivitychecker.backend.awsclient.AWSClient;
import in._10h.java.awsvpcconnectivitychecker.backend.k8sclient.KubernetesClient;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;


@RestController
public class MainController {

    private final KubernetesClient kubernetesClient;
    private final AWSClient awsClient;

    @Autowired
    public MainController(
            final KubernetesClient kubernetesClient,
            final AWSClient awsClient
    ) {

        this.kubernetesClient = kubernetesClient;
        this.awsClient = awsClient;

    }

    public record PodInfo(String namespace, String name, String ipAddress, String eniID) {}

    @GetMapping("/eks/namespaces/{namespace}/pods")
    public Flux<PodInfo> listPodInfo(
            @RequestParam("namespace")
            final String namespace
    ) {

        return this.kubernetesClient.listNamespacedPod(namespace)
                .flatMap(this::toPodInfo);

    }

    private Flux<PodInfo> toPodInfo(final V1Pod baseInfo) {

        var metadata = Objects.requireNonNull(baseInfo.getMetadata());
        var namespace = Objects.requireNonNull(metadata.getNamespace());
        var name = Objects.requireNonNull(metadata.getName());

        var status = Objects.requireNonNull(baseInfo.getStatus());
        var ipAddress = Objects.requireNonNull(status.getPodIP());

        return this.awsClient.listENIIDsByIPAddress(ipAddress)
                .map(eniID -> new PodInfo(namespace, name, ipAddress, eniID));

    }

}
