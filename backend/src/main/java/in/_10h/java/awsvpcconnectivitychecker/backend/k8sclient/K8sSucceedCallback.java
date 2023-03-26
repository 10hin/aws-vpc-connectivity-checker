package in._10h.java.awsvpcconnectivitychecker.backend.k8sclient;

import java.util.List;
import java.util.Map;

public interface K8sSucceedCallback<T> {
    void onSuccess(T result, int statusCode, Map<String, List<String>> responseHeader);
}
