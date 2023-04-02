package in._10h.java.awsvpcconnectivitychecker.backend.awsclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.elasticache.ElastiCacheAsyncClient;
import software.amazon.awssdk.services.rds.RdsAsyncClient;

import java.net.URI;

@Configuration
public class AWSConfig {

    @Bean
    public Ec2AsyncClient ec2Client(
            @Value("aws.ec2.region") String region,
            @Value("aws.ec2.endpoint:unset") String endpoint
    ) {
        var httpClient = NettyNioAsyncHttpClient.builder()
                .protocol(Protocol.HTTP2)
                .maxConcurrency(Integer.MAX_VALUE)
                .build();
        var builder = Ec2AsyncClient.builder()
                .region(Region.of(region))
                .httpClient(httpClient);
        if (!"unset".equals(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    @Bean
    public RdsAsyncClient rdsClient(
            @Value("aws.rds.region") String region,
            @Value("aws.rds.endpoint:unset") String endpoint
    ) {
        var httpClient = NettyNioAsyncHttpClient.builder()
                .protocol(Protocol.HTTP2)
                .maxConcurrency(Integer.MAX_VALUE)
                .build();
        var builder = RdsAsyncClient.builder()
                .region(Region.of(region))
                .httpClient(httpClient);
        if (!"unset".equals(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    @Bean
    public ElastiCacheAsyncClient elastiCacheClient(
            @Value("aws.elasticache.region") String region,
            @Value("aws.elasticache.endpoint:unset") String endpoint
    ) {
        var httpClient = NettyNioAsyncHttpClient.builder()
                .protocol(Protocol.HTTP2)
                .maxConcurrency(Integer.MAX_VALUE)
                .build();
        var builder = ElastiCacheAsyncClient.builder()
                .region(Region.of(region))
                .httpClient(httpClient);
        if (!"unset".equals(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

}
