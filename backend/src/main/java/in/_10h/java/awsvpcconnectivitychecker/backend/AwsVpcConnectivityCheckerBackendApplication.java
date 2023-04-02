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
import software.amazon.awssdk.services.elasticache.ElastiCacheAsyncClient;
import software.amazon.awssdk.services.rds.RdsAsyncClient;

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

}
