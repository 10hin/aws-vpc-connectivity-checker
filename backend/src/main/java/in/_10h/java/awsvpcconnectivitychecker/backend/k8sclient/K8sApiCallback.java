package in._10h.java.awsvpcconnectivitychecker.backend.k8sclient;

import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class K8sApiCallback<T> implements ApiCallback<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(K8sApiCallback.class);

    private final K8sSucceedCallback<T> successCallback;
    private final K8sFailureCallback failureCallback;

    private K8sApiCallback(
            final K8sSucceedCallback<T> successCallback,
            final K8sFailureCallback failureCallback
    ) {

        this.successCallback = successCallback;
        this.failureCallback = failureCallback;

    }

    @Override
    public void onSuccess(final T result, final int statusCode, final Map<String, List<String>> responseHeaders) {
        this.successCallback.onSuccess(result, statusCode, responseHeaders);
    }

    @Override
    public void onFailure(final ApiException e, final int statusCode, final Map<String, List<String>> responseHeaders) {
        this.failureCallback.onFailure(e, statusCode, responseHeaders);
    }

    @Override
    public void onDownloadProgress(final long bytesRead, final long contentLength, final boolean done) {
        LOGGER.trace("donwload progress... (bytesRead: {}, contentLength: {}, progress percentage: {}, done: {})", bytesRead, contentLength, bytesRead * 100 / contentLength, done);
    }

    @Override
    public void onUploadProgress(final long bytesWritten, final long contentLength, final boolean done) {
        LOGGER.trace("upload progress... (bytesWritten: {}, contentLength: {}, progress percentage: {}, done: {})", bytesWritten, contentLength, bytesWritten * 100 / contentLength, done);
    }

    public static <T> K8sApiCallback<T> of(final K8sSucceedCallback<T> successCallback) {
        return K8sApiCallback.of(successCallback, (e, statusCode, responseHeaders) -> { throw new InternalError("unhandled failure callback", e); });
    }

    public static <T> K8sApiCallback<T> of(final K8sSucceedCallback<T> successCallback, final K8sFailureCallback failureCallback) {
        return new K8sApiCallback<>(successCallback, failureCallback);
    }

}
