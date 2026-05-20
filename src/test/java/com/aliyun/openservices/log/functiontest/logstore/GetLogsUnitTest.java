package com.aliyun.openservices.log.functiontest.logstore;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.LogContent;
import com.aliyun.openservices.log.common.QueriedLog;
import com.aliyun.openservices.log.http.comm.ServiceClient;
import com.aliyun.openservices.log.response.GetLogsResponse;
import com.aliyun.openservices.log.testing.FakeServiceClient;
import com.aliyun.openservices.log.testing.RequestMatcher;
import com.aliyun.openservices.log.testing.Responses;
import com.aliyun.openservices.log.testing.TestClients;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test exercising {@code Client.GetLogs(...)} via {@link FakeServiceClient}.
 * Verifies that a JSON-shaped query result returned by the mock is parsed into
 * {@link GetLogsResponse}.
 */
public class GetLogsUnitTest {

    @Test
    public void getLogsParsesJsonBody() throws Exception {
        String body = "{"
                + "\"meta\":{\"progress\":\"Complete\",\"keys\":[\"k\"],\"hasSQL\":false,"
                + "\"processedRows\":1,\"elapsedMillisecond\":2,\"count\":1,"
                + "\"phraseQueryInfo\":{}},"
                + "\"data\":[{\"__time__\":\"100\",\"__source__\":\"127.0.0.1\",\"k\":\"v\"}]"
                + "}";
        FakeServiceClient fake = new FakeServiceClient();
        fake.stub(RequestMatcher.method("POST").path("/logstores/test-store/logs"),
                Responses.ok(body));
        Client client = TestClients.newMockedClient(fake);

        GetLogsResponse resp = client.GetLogs("test-project", "test-store",
                0, 100, "", "*");

        assertNotNull(resp);
        assertEquals(1, resp.GetCount());
        ArrayList<QueriedLog> logs = resp.GetLogs();
        assertEquals(1, logs.size());
        ArrayList<LogContent> contents = logs.get(0).GetLogItem().mContents;
        assertEquals("k", contents.get(0).GetKey());
        assertEquals("v", contents.get(0).GetValue());
        ServiceClient.Request req = fake.lastRequest();
        assertNotNull(req.getHeaders().get("Authorization"));
    }
}
