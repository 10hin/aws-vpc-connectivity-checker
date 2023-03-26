package in._10h.java.awsvpcconnectivitychecker.backend;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;

import java.io.IOException;
import java.net.URI;

@SpringBootApplication
public class AwsVpcConnectivityCheckerBackendApplication {

	public static void main(String[] args) throws IOException {
		final var client = Config.defaultClient();
		Configuration.setDefaultApiClient(client);

		SpringApplication.run(AwsVpcConnectivityCheckerBackendApplication.class, args);
	}

	@Bean
	public CoreV1Api coreV1Client() {
		return new CoreV1Api();
	}

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

}
