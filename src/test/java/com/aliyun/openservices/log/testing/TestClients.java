package com.aliyun.openservices.log.testing;

import com.aliyun.openservices.log.Client;

/**
 * Helpers that produce an SLS {@link Client} bound to a {@link FakeServiceClient}
 * for in-process unit testing.
 */
public final class TestClients {

    /** Endpoint used by {@link #newMockedClient(FakeServiceClient)}. */
    public static final String MOCK_ENDPOINT = "cn-mock.example.com";

    /** Access key id used by {@link #newMockedClient(FakeServiceClient)}. */
    public static final String MOCK_ACCESS_KEY_ID = "mock-id";

    /** Access key secret used by {@link #newMockedClient(FakeServiceClient)}. */
    public static final String MOCK_ACCESS_KEY_SECRET = "mock-key";

    private TestClients() {
    }

    /**
     * Build an SLS {@link Client} whose underlying transport is the supplied
     * {@link FakeServiceClient}. No real network traffic will leave the JVM.
     */
    public static Client newMockedClient(FakeServiceClient fake) {
        return new Client(MOCK_ENDPOINT, MOCK_ACCESS_KEY_ID, MOCK_ACCESS_KEY_SECRET, fake);
    }
}
