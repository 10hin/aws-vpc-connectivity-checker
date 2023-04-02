package in._10h.java.awsvpcconnectivitychecker.backend.awsclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.elasticache.ElastiCacheAsyncClient;
import software.amazon.awssdk.services.elasticache.model.*;
import software.amazon.awssdk.services.rds.RdsAsyncClient;
import software.amazon.awssdk.services.rds.model.DBCluster;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersResponse;
import software.amazon.awssdk.services.rds.model.VpcSecurityGroupMembership;

import java.net.InetAddress;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class AWSClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AWSClient.class);

    private final Ec2AsyncClient ec2Client;
    private final RdsAsyncClient rdsClient;
    private final ElastiCacheAsyncClient elastiCacheClient;

    @Autowired
    public AWSClient(
            final Ec2AsyncClient ec2Client,
            final RdsAsyncClient rdsClient,
            final ElastiCacheAsyncClient elastiCacheClient
    ) {

        this.ec2Client = ec2Client;
        this.rdsClient = rdsClient;
        this.elastiCacheClient = elastiCacheClient;

    }

    public Flux<String> listENIIDsByIPAddress(final String ipAddress) {
        var req = DescribeNetworkInterfacesRequest.builder()
                .filters(
                        Filter.builder()
                                .name("addresses.private-ip-address").values(List.of(ipAddress))
                                .build()
                )
                .build();
        var stream = this.ec2Client.describeNetworkInterfacesPaginator(req)
                .flatMapIterable(DescribeNetworkInterfacesResponse::networkInterfaces)
                .map(NetworkInterface::networkInterfaceId);
        return Flux.from(stream);
    }

    public Flux<String> listENIIDsByRDSDBClusterIdentifier(final String dbClusterIdentifier) {
        var req = DescribeDbClustersRequest.builder()
                .dbClusterIdentifier(dbClusterIdentifier)
                .build();
        return Flux.from(this.rdsClient.describeDBClustersPaginator(req))
                .flatMapIterable(AWSClient::extractSecurityGroupIDs)
                .collectList()
                .flatMapMany(this::listENIIDsBySecurityGroupIDs);

    }

    public Flux<String> listENIIDsByReplicationGroupID(final String replicationGroupID) {
        var req = DescribeReplicationGroupsRequest.builder()
                .replicationGroupId(replicationGroupID)
                .build();
        return Flux.from(this.elastiCacheClient.describeReplicationGroupsPaginator(req))
                .flatMapIterable(AWSClient::extractCacheClusterNames)
                .map(cacheClusterName -> DescribeCacheClustersRequest.builder().cacheClusterId(cacheClusterName).build())
                .flatMap(this.elastiCacheClient::describeCacheClustersPaginator)
                .flatMapIterable(AWSClient::extractSecurityGroupIDs)
                .collectList()
                .flatMapMany(this::listENIIDsBySecurityGroupIDs);

    }

    public Flux<String> listNetworkInsightID(final List<String> fromENIIDs, final List<String> toENIIDs) {
        var req = DescribeNetworkInsightsPathsRequest.builder()
                .filters(
                        Filter.builder().name("source").values(fromENIIDs).build(),
                        Filter.builder().name("destination").values(toENIIDs).build()
                )
                .build();
        return Flux.from(this.ec2Client.describeNetworkInsightsPathsPaginator(req))
                .flatMapIterable(DescribeNetworkInsightsPathsResponse::networkInsightsPaths)
                .map(NetworkInsightsPath::networkInsightsPathId);

    }

    private Flux<String> listENIIDsBySecurityGroupIDs(final List<String> securityGroupIDs) {
        var req = DescribeNetworkInterfacesRequest.builder()
                .filters(Filter.builder().name("group-id").values(securityGroupIDs).build())
                .build();
        return Flux.from(this.ec2Client.describeNetworkInterfacesPaginator(req))
                .flatMapIterable(DescribeNetworkInterfacesResponse::networkInterfaces)
                .map(NetworkInterface::networkInterfaceId);
    }

    private static List<String> extractSecurityGroupIDs(final DescribeDbClustersResponse resp) {
        return resp.dbClusters().stream()
                .map(DBCluster::vpcSecurityGroups)
                .flatMap(List::stream)
                .map(VpcSecurityGroupMembership::vpcSecurityGroupId)
                .collect(Collectors.toList());
    }

    private static List<String> extractSecurityGroupIDs(final DescribeCacheClustersResponse resp) {
        return resp.cacheClusters().stream()
                .map(CacheCluster::securityGroups)
                .flatMap(List::stream)
                .map(SecurityGroupMembership::securityGroupId)
                .collect(Collectors.toList());
    }

    private static List<String> extractCacheClusterNames(final DescribeReplicationGroupsResponse resp) {
        return resp.replicationGroups().stream()
                .map(ReplicationGroup::memberClusters)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

}
