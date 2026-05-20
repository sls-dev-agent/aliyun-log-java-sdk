# Testing aliyun-log-java-sdk

Tests in this repo are split into two classes:

- **Unit tests** — class names end in `*Test`. Run by `mvn test` via
  `maven-surefire-plugin`. They never touch the network and are safe to run in
  any environment, including CI on every push.
- **Integration tests** — class names end in `*IT`. Run by `mvn verify` via
  `maven-failsafe-plugin`. They require a real Alibaba Cloud SLS endpoint and
  access keys. Without those, every IT is reported as *Skipped* (not Failed).

## Running unit tests

```bash
mvn -B test
```

No environment variables required. All HTTP traffic in unit tests is served by
`FakeServiceClient`.

## Running integration tests

Set credentials, then run `mvn verify`:

```bash
export LOG_TEST_ENDPOINT=cn-hangzhou.log.aliyuncs.com
export LOG_TEST_ACCESS_KEY_ID=...
export LOG_TEST_ACCESS_KEY_SECRET=...
# Optional
export LOG_TEST_PROJECT=my-test-project
export LOG_TEST_REGION=cn-hangzhou
export LOG_TEST_ALIUID=...

mvn -B verify
```

You may instead drop a `~/sh_stg.json` file:

```json
{
  "endpoint": "cn-hangzhou.log.aliyuncs.com",
  "accessKeyId": "...",
  "accessKey": "...",
  "aliuid": "0",
  "region": "cn-hangzhou"
}
```

When neither source provides credentials,
`FunctionTest.setUpCredentials()` calls `Assume.assumeTrue(...)`, so each IT
reports as *Skipped* rather than failing.

## Mock infrastructure (`src/test/java/.../testing/`)

A small in-process toolkit for writing unit tests that exercise the HTTP
boundary without leaving the JVM.

### Building a mocked client

```java
import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.testing.FakeServiceClient;
import com.aliyun.openservices.log.testing.RequestMatcher;
import com.aliyun.openservices.log.testing.Responses;
import com.aliyun.openservices.log.testing.TestClients;

FakeServiceClient fake = new FakeServiceClient();
fake.stub(RequestMatcher.method("POST").path("/logstores/foo/shards/lb"),
          Responses.ok(new byte[0]));

Client client = TestClients.newMockedClient(fake);
client.PutLogs("project", "foo", "topic", logItems, "src");

// Verify what the SDK actually sent.
assertEquals(1, fake.getReceivedRequests().size());
```

`FakeServiceClient` extends `com.aliyun.openservices.log.http.comm.ServiceClient`,
so the SDK's signing, retry, and parse pipeline runs end-to-end before the
mocked transport returns a canned response.

`RequestMatcher` filters by HTTP method, path prefix, query parameters, and
headers. `Responses` builds canned `ResponseMessage` instances:

- `Responses.ok(String json)` — 200 with JSON body.
- `Responses.ok(byte[] body)` — 200 with raw bytes.
- `Responses.okProto(Message proto)` — 200 with serialized protobuf body.
- `Responses.error(int status, String code, String message)` — SLS error envelope.

### Mockito

`mockito-core` is on the test classpath for cases where you want behavioral
assertions ("called once with X") rather than wire-level stubs. Prefer
`FakeServiceClient` for HTTP-shaped assertions; reach for Mockito for
collaborator interactions inside the SDK.

### Skipping when no real endpoint is configured

Subclasses of `FunctionTest` automatically inherit the `@BeforeClass` that
calls `Credentials.loadOrSkip()` — no extra code needed. For standalone ITs
that don't extend `FunctionTest`, call `Credentials.loadOrSkip()` from your own
`@BeforeClass`.

`@AfterClass` hooks still run after a `@BeforeClass` assumption violation, so
guard their bodies with `if (!HAS_REAL_CREDENTIALS) return;` (or use
`safeDeleteProjectWithoutSleep`, which already does this) to avoid failing
tear-down attempts against the placeholder host.

## Naming convention

- `*Test.java` — unit test, runs by default with surefire.
- `*IT.java` — integration test, runs only with `mvn verify` and requires
  real SLS credentials (otherwise skipped).

When extracting a unit suite from an existing IT, leave the integration
behavior in `FooIT.java` and put the new mock-based unit suite in a sibling
`FooUnitTest.java` (see `functiontest/logstore/PutLogsUnitTest.java`).

## CI

`.github/workflows/maven.yml` defines three jobs:

- `build` — `mvn package -DskipTests`, every push/PR.
- `unit-test` — `mvn test`, every push/PR. Network-free.
- `integration-test` — `mvn verify`, **manual only** (`workflow_dispatch`).
  Consumes `LOG_TEST_*` from repository secrets.

## Testing toolkit reuse

The `com.aliyun.openservices.log.testing` package is intentionally kept under
`src/test/java/` (not packaged with the SDK jar). Sibling SDK projects
(`aliyun-log-java-producer`, `aliyun-log-consumer-java`) maintain their own
copies of `FakeServiceClient` / `RequestMatcher` / `Responses` rather than
depending on a `tests-jar` from this module. Update those copies in lockstep
when you change the public API of these helpers.
