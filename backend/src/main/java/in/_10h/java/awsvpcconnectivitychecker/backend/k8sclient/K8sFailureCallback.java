package in._10h.java.awsvpcconnectivitychecker.backend.k8sclient;

import io.kubernetes.client.openapi.ApiException;

import java.util.List;
import java.util.Map;

public interface K8sFailureCallback {
    void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeader);
}
