package in._10h.java.awsvpcconnectivitychecker.backend.awsclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;

import java.net.InetAddress;
import java.util.List;

@Component
public class AWSClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AWSClient.class);

    private final Ec2AsyncClient ec2Client;

    @Autowired
    public AWSClient(
            final Ec2AsyncClient ec2Client
    ) {

        this.ec2Client = ec2Client;

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

}
