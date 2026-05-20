package com.aliyun.openservices.log.functiontest.logstore;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.LogStore;
import com.aliyun.openservices.log.http.client.HttpMethod;
import com.aliyun.openservices.log.http.comm.ServiceClient;
import com.aliyun.openservices.log.response.CreateLogStoreResponse;
import com.aliyun.openservices.log.testing.FakeServiceClient;
import com.aliyun.openservices.log.testing.RequestMatcher;
import com.aliyun.openservices.log.testing.Responses;
import com.aliyun.openservices.log.testing.TestClients;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test exercising {@code Client.CreateLogStore(...)} via {@link FakeServiceClient}.
 * Verifies the SDK posts to {@code /logstores} and parses the empty 200 response.
 */
public class CreateLogStoreUnitTest {

    @Test
    public void createLogStoreSucceedsOnEmpty200() throws Exception {
        FakeServiceClient fake = new FakeServiceClient();
        fake.stub(RequestMatcher.method("POST").path("/logstores"),
                Responses.ok(new byte[0]));
        Client client = TestClients.newMockedClient(fake);

        LogStore store = new LogStore("test-store", 30, 2);
        CreateLogStoreResponse resp = client.CreateLogStore("test-project", store);

        assertNotNull(resp);
        ServiceClient.Request req = fake.lastRequest();
        assertEquals(HttpMethod.POST, req.getMethod());
        assertEquals("Content-Type should be SLS JSON",
                "application/json", req.getHeaders().get("Content-Type"));
        assertNotNull(req.getHeaders().get("Authorization"));
    }
}
