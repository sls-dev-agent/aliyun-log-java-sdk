package com.aliyun.openservices.log.testing;

import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.http.client.ClientConfiguration;
import com.aliyun.openservices.log.http.comm.ResponseMessage;
import com.aliyun.openservices.log.http.comm.RetryStrategy;
import com.aliyun.openservices.log.http.comm.ServiceClient;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link ServiceClient} that records all outbound requests and answers them
 * from a list of pre-registered stubs instead of performing real HTTP I/O.
 *
 * <p>Designed to be the primary mock for SLS Java SDK unit tests. Stubs are
 * matched in registration order; the first stub whose {@link RequestMatcher}
 * accepts the request wins. If no stub matches, {@code sendRequestCore} throws
 * a {@link LogException} with code {@code MockNotFound}.</p>
 *
 * <p>Typical usage:</p>
 * <pre>
 *   FakeServiceClient fake = new FakeServiceClient();
 *   fake.stub(RequestMatcher.method("POST").path("/logstores/foo/shards/lb"),
 *             Responses.ok(new byte[0]));
 *   Client client = TestClients.newMockedClient(fake);
 *   client.PutLogs("project", "foo", "topic", logItems, "");
 *   assertEquals(1, fake.getReceivedRequests().size());
 * </pre>
 *
 * <p>This class is part of the test-only "testing" toolkit and is intended to
 * be copy-paste-reused in sibling SDK projects (java-producer, consumer-java).
 * Do not change method signatures without updating the dependents.</p>
 */
public final class FakeServiceClient extends ServiceClient {

    /** Returns a {@link ResponseMessage} for a recorded request. */
    public interface ResponseSupplier {
        ResponseMessage get(ServiceClient.Request request) throws LogException;
    }

    private static final class Stub {
        final RequestMatcher matcher;
        final ResponseSupplier supplier;

        Stub(RequestMatcher matcher, ResponseSupplier supplier) {
            this.matcher = matcher;
            this.supplier = supplier;
        }
    }

    private final List<Stub> stubs = new ArrayList<Stub>();
    private final List<ServiceClient.Request> received = new ArrayList<ServiceClient.Request>();
    private final List<byte[]> receivedBodies = new ArrayList<byte[]>();

    public FakeServiceClient() {
        super(new ClientConfiguration());
    }

    public FakeServiceClient(ClientConfiguration config) {
        super(config);
    }

    /**
     * Register a stub. The first stub whose matcher accepts the request will
     * supply the response.
     */
    public FakeServiceClient stub(RequestMatcher matcher, ResponseSupplier supplier) {
        stubs.add(new Stub(matcher, supplier));
        return this;
    }

    /** Convenience overload: always return the same response for matching requests. */
    public FakeServiceClient stub(RequestMatcher matcher, final ResponseMessage response) {
        return stub(matcher, new ResponseSupplier() {
            @Override
            public ResponseMessage get(ServiceClient.Request req) {
                return cloneResponse(response);
            }
        });
    }

    /** Replace all currently registered stubs. */
    public FakeServiceClient resetStubs() {
        stubs.clear();
        return this;
    }

    /** Forget all recorded requests. Stubs are kept. */
    public FakeServiceClient resetRequests() {
        received.clear();
        receivedBodies.clear();
        return this;
    }

    /** All requests passed to {@code sendRequestCore} so far, in order. */
    public List<ServiceClient.Request> getReceivedRequests() {
        return Collections.unmodifiableList(received);
    }

    /** The body bytes of the request at index {@code i}. May be empty (never null). */
    public byte[] getReceivedBody(int i) {
        return receivedBodies.get(i);
    }

    /** Convenience: latest received request, or {@code null} if none yet. */
    public ServiceClient.Request lastRequest() {
        return received.isEmpty() ? null : received.get(received.size() - 1);
    }

    @Override
    protected ResponseMessage sendRequestCore(Request request, String charset) throws Exception {
        ServiceClient.Request snapshot = snapshot(request);
        received.add(snapshot);
        for (Stub stub : stubs) {
            if (stub.matcher.matches(snapshot)) {
                ResponseMessage resp = stub.supplier.get(snapshot);
                if (resp == null) {
                    throw new LogException("MockNullResponse",
                            "stub returned null for: " + snapshot.getUri(), "");
                }
                return resp;
            }
        }
        throw new LogException("MockNotFound",
                "no stub matched: " + request.getMethod() + " " + request.getUri(), "");
    }

    @Override
    protected RetryStrategy getDefaultRetryStrategy() {
        return new NeverRetryStrategy();
    }

    @Override
    public void shutdown() {
        // no resources to release
    }

    @Override
    public HttpClientConnectionManager getConnectionManager() {
        return null;
    }

    private ServiceClient.Request snapshot(Request request) throws IOException {
        ServiceClient.Request copy = new ServiceClient.Request();
        copy.setMethod(request.getMethod());
        copy.setUrl(request.getUri());
        Map<String, String> headers = new HashMap<String, String>();
        if (request.getHeaders() != null) {
            headers.putAll(request.getHeaders());
        }
        copy.setHeaders(headers);
        byte[] body = readAll(request.getContent());
        receivedBodies.add(body);
        copy.setContent(new ByteArrayInputStream(body));
        copy.setContentLength(body.length);
        return copy;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static ResponseMessage cloneResponse(ResponseMessage source) {
        ResponseMessage copy = new ResponseMessage();
        copy.setStatusCode(source.getStatusCode());
        if (source.getHeaders() != null) {
            Map<String, String> headers = new HashMap<String, String>(source.getHeaders());
            copy.setHeaders(headers);
        }
        byte[] body = source.GetRawBody();
        if (body == null) {
            try {
                byte[] read = readAll(source.getContent());
                body = read;
            } catch (IOException ignore) {
                body = new byte[0];
            }
        }
        copy.setContent(new ByteArrayInputStream(body));
        copy.setContentLength(body.length);
        copy.SetBody(body);
        return copy;
    }

    private static final class NeverRetryStrategy extends RetryStrategy {
        @Override
        public boolean shouldRetry(Exception ex,
                com.aliyun.openservices.log.http.comm.RequestMessage request, int retries) {
            return false;
        }
    }
}
