package in._10h.java.awsvpcconnectivitychecker.backend;

import in._10h.java.awsvpcconnectivitychecker.backend.awsclient.AWSClient;
import in._10h.java.awsvpcconnectivitychecker.backend.k8sclient.KubernetesClient;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
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

    @GetMapping("/eks/namespaces/{namespace}/pods")
    public Flux<PodInfo> listPodInfo(
            @PathVariable("namespace")
            final String namespace
    ) {

        return this.kubernetesClient.listNamespacedPod(namespace)
                .flatMapIterable(V1PodList::getItems)
                .flatMap(this::toPodInfo);

    }

    private Flux<PodInfo> toPodInfo(final V1Pod pod) {
        var metadata = Objects.requireNonNull(pod.getMetadata());
        var namespace = Objects.requireNonNull(metadata.getNamespace());
        var name = Objects.requireNonNull(metadata.getName());

        var status = Objects.requireNonNull(pod.getStatus());
        var ipAddress = Objects.requireNonNull(status.getPodIP());

        return this.awsClient.listENIIDsByIPAddress(ipAddress)
                .map(eniID -> new PodInfo(namespace, name, ipAddress, eniID));
    }

    @GetMapping("/rds/dbcluster/{dbClusterIdentifier}")
    public Mono<DBClusterInfo> getDBCluster(
            @PathVariable("dbClusterIdentifier")
            final String dbClusterIdentifier
    ) {

        return this.awsClient.listENIIDsByRDSDBClusterIdentifier(dbClusterIdentifier)
                .collectList()
                .map(eniIDs -> new DBClusterInfo(dbClusterIdentifier, eniIDs));

    }

    @GetMapping("/elasticache/replicationgroup/{replicationGroupID}")
    public Mono<ReplicationGroupInfo> getReplicationGroup(
            @PathVariable("replicationGroupID")
            final String replicationGroupID
    ) {

        return this.awsClient.listENIIDsByReplicationGroupID(replicationGroupID)
                .collectList()
                .map(eniIDs -> new ReplicationGroupInfo(replicationGroupID, eniIDs));

    }

    @GetMapping("/ec2/networkinsightspath/")
    public Flux<NetworkInsightsPath> listNetworkInsightsPath(
            @RequestParam("src")
            final List<String> sourceENIIDs,
            @RequestParam("dst")
            final List<String>destinationENIIDs
    ) {

        return this.awsClient.listNetworkInsightID(sourceENIIDs, destinationENIIDs)
                .map(NetworkInsightsPath::new);

    }

    public record PodInfo(String namespace, String name, String ipAddress, String eniID) {}
    public record DBClusterInfo(String dbIdentifier, List<String> eniIDs) {}
    public record ReplicationGroupInfo(String replicationGroupID, List<String> eniIDs) {}
    public record NetworkInsightsPath(String networkInsightsPathID) {}

}
