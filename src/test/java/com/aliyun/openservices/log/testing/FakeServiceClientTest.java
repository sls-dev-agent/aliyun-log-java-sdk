package com.aliyun.openservices.log.testing;

import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.http.client.HttpMethod;
import com.aliyun.openservices.log.http.comm.RequestMessage;
import com.aliyun.openservices.log.http.comm.ResponseMessage;
import com.aliyun.openservices.log.http.comm.ServiceClient;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Sanity checks for the {@link FakeServiceClient} mock infrastructure itself —
 * verify that stubs match by method/path, that requests are recorded with body
 * bytes intact, and that an unmatched request raises a clear error.
 */
public class FakeServiceClientTest {

    @Test
    public void firstMatchingStubWins() throws Exception {
        FakeServiceClient fake = new FakeServiceClient();
        ResponseMessage first = Responses.ok("first");
        ResponseMessage second = Responses.ok("second");
        fake.stub(RequestMatcher.method("POST").path("/logstores/foo"), first);
        fake.stub(RequestMatcher.any(), second);

        RequestMessage req = postRequest("/logstores/foo");
        ResponseMessage resp = fake.sendRequest(req, "UTF-8");

        assertEquals(200, resp.getStatusCode());
        assertEquals("first", new String(resp.GetRawBody(), "UTF-8"));
        assertEquals(1, fake.getReceivedRequests().size());
    }

    @Test
    public void unmatchedRequestRaisesMockNotFound() {
        FakeServiceClient fake = new FakeServiceClient();
        try {
            fake.sendRequest(postRequest("/nope"), "UTF-8");
            fail("expected MockNotFound");
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            assertTrue(cause instanceof LogException);
            assertEquals("MockNotFound", ((LogException) cause).GetErrorCode());
        }
    }

    @Test
    public void recordsBodyBytes() throws Exception {
        FakeServiceClient fake = new FakeServiceClient();
        fake.stub(RequestMatcher.any(), Responses.ok(new byte[0]));
        RequestMessage req = postRequest("/echo");
        byte[] body = "hello".getBytes("UTF-8");
        req.setContent(new ByteArrayInputStream(body));
        req.setContentLength(body.length);

        fake.sendRequest(req, "UTF-8");

        assertEquals(1, fake.getReceivedRequests().size());
        assertEquals("hello", new String(fake.getReceivedBody(0), "UTF-8"));
    }

    @Test
    public void matcherInspectsQueryAndHeaders() {
        ServiceClient.Request request = new ServiceClient.Request();
        request.setMethod(HttpMethod.GET);
        request.setUrl("https://h/api?a=1&b=2");
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-Stub", "yes");
        request.setHeaders(headers);

        assertTrue(RequestMatcher.method("GET").path("/api").matches(request));
        assertTrue(RequestMatcher.method("GET").query("a", "1").matches(request));
        assertTrue(RequestMatcher.method("GET").header("X-Stub", "yes").matches(request));
        assertEquals(false, RequestMatcher.method("POST").matches(request));
        assertEquals(false, RequestMatcher.method("GET").query("a", "9").matches(request));
    }

    @Test
    public void supplierCanVaryResponse() throws Exception {
        final ResponseMessage[] toServe = new ResponseMessage[]{Responses.ok("a"), Responses.ok("b")};
        final int[] idx = {0};
        FakeServiceClient fake = new FakeServiceClient();
        fake.stub(RequestMatcher.any(), new FakeServiceClient.ResponseSupplier() {
            @Override
            public ResponseMessage get(ServiceClient.Request req) {
                return toServe[idx[0]++];
            }
        });

        ResponseMessage r1 = fake.sendRequest(postRequest("/x"), "UTF-8");
        ResponseMessage r2 = fake.sendRequest(postRequest("/x"), "UTF-8");
        assertNotNull(r1);
        assertNotNull(r2);
        assertEquals("a", new String(r1.GetRawBody(), "UTF-8"));
        assertEquals("b", new String(r2.GetRawBody(), "UTF-8"));
    }

    @Test
    public void clientFactoryReturnsBoundClient() {
        FakeServiceClient fake = new FakeServiceClient();
        // Shouldn't throw or hit network.
        assertSame(TestClients.MOCK_ENDPOINT, TestClients.MOCK_ENDPOINT);
        assertNotNull(TestClients.newMockedClient(fake));
    }

    private static RequestMessage postRequest(String resourcePath) {
        RequestMessage msg = new RequestMessage();
        msg.setMethod(HttpMethod.POST);
        // No trailing slash on endpoint: ServiceClient.buildRequest concatenates
        // endpoint + resourcePath verbatim if the resourcePath already starts
        // with '/', so a trailing slash on endpoint would produce '//<path>'.
        msg.setEndpoint(URI.create("http://example"));
        msg.setResourcePath(resourcePath);
        msg.setContent(new ByteArrayInputStream(new byte[0]));
        msg.setContentLength(0);
        return msg;
    }
}
