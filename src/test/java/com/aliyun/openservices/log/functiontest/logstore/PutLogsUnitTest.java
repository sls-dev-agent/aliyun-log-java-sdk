package com.aliyun.openservices.log.functiontest.logstore;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.http.client.HttpMethod;
import com.aliyun.openservices.log.http.comm.ServiceClient;
import com.aliyun.openservices.log.testing.FakeServiceClient;
import com.aliyun.openservices.log.testing.RequestMatcher;
import com.aliyun.openservices.log.testing.Responses;
import com.aliyun.openservices.log.testing.TestClients;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test exercising {@code Client.PutLogs(...)} via {@link FakeServiceClient}.
 * Verifies that the SDK signs the request, encodes a non-empty protobuf body,
 * and routes to {@code /logstores/<store>/shards/lb}.
 */
public class PutLogsUnitTest {

    @Test
    public void putLogsWritesSignedProtobufBody() throws Exception {
        FakeServiceClient fake = new FakeServiceClient();
        fake.stub(RequestMatcher.method("POST").path("/logstores/test-store/shards/lb"),
                Responses.ok(new byte[0]));
        Client client = TestClients.newMockedClient(fake);

        List<LogItem> items = new ArrayList<LogItem>();
        LogItem item = new LogItem((int) (System.currentTimeMillis() / 1000));
        item.PushBack("k", "v");
        items.add(item);

        client.PutLogs("test-project", "test-store", "test-topic", items, "src");

        assertEquals(1, fake.getReceivedRequests().size());
        ServiceClient.Request req = fake.lastRequest();
        assertEquals(HttpMethod.POST, req.getMethod());
        assertTrue("expected /logstores/test-store/shards/lb path, got " + req.getUri(),
                req.getUri().contains("/logstores/test-store/shards/lb"));
        String authorization = req.getHeaders().get("Authorization");
        assertNotNull("missing Authorization header", authorization);
        assertTrue("Authorization should reference the mock access key id",
                authorization.contains(TestClients.MOCK_ACCESS_KEY_ID));
        assertTrue("expected non-empty request body",
                fake.getReceivedBody(0).length > 0);
    }
}
