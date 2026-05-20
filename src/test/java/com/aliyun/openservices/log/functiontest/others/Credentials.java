package com.aliyun.openservices.log.functiontest.others;

import com.alibaba.fastjson.JSONObject;
import org.junit.Assume;

import java.io.File;
import java.util.Scanner;


public final class Credentials {
    private static final String CONFIG_FILE = "sh_stg.json";

    private static final String ENV_ENDPOINT = "LOG_TEST_ENDPOINT";
    private static final String ENV_ACCESS_KEY_ID = "LOG_TEST_ACCESS_KEY_ID";
    private static final String ENV_ACCESS_KEY_SECRET = "LOG_TEST_ACCESS_KEY_SECRET";
    private static final String ENV_PROJECT = "LOG_TEST_PROJECT";
    private static final String ENV_REGION = "LOG_TEST_REGION";
    private static final String ENV_ALIUID = "LOG_TEST_ALIUID";

    private String endpoint;
    private String accessKeyId;
    private String accessKey;
    private String aliuid;
    private String region;

    public Credentials(String endpoint, String accessKeyId, String accessKey, String aliuid) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKey = accessKey;
        this.aliuid = aliuid;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getAliuid() {
        return aliuid;
    }

    public void setAliuid(String aliuid) {
        this.aliuid = aliuid;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * Load test credentials from {@code ~/sh_stg.json}, falling back to the
     * {@code LOG_TEST_*} environment variables if the file is absent or
     * unreadable. Returns {@code null} when neither source provides a complete
     * credential set, so that tests can {@code Assume.assumeTrue(...)} skip.
     */
    public static Credentials load() {
        Credentials fromFile = loadFromFile();
        if (fromFile != null) {
            return fromFile;
        }
        return loadFromEnv();
    }

    /**
     * Like {@link #load()} but raises a JUnit {@link Assume} skip when no
     * credentials are configured, so the surrounding test is reported as
     * ignored instead of failing. Never returns {@code null}.
     *
     * <p>Call this from {@code @BeforeClass} or {@code @Before} so the
     * {@link org.junit.AssumptionViolatedException} is converted into a clean
     * "ignored" result. Calling it from a static initializer turns the JVM's
     * class-load failure into an {@code ExceptionInInitializerError}, which
     * is not what we want.</p>
     */
    public static Credentials loadOrSkip() {
        Credentials credentials = load();
        Assume.assumeTrue(
                "SKIP: requires ~/" + CONFIG_FILE + " or " + ENV_ENDPOINT + " env",
                credentials != null);
        return credentials;
    }

    /**
     * Returns {@link #load()} when credentials are configured, otherwise a
     * non-null placeholder with dummy values. Designed to keep static
     * initializers in test classes safe to load even when running on a
     * developer machine with no credentials. Tests should additionally call
     * {@link #loadOrSkip()} from {@code @BeforeClass} so the missing-credential
     * case is reported as ignored.
     */
    public static Credentials loadOrPlaceholder() {
        Credentials credentials = load();
        if (credentials != null) {
            return credentials;
        }
        Credentials placeholder = new Credentials(
                "placeholder.example.com",
                "placeholder-id",
                "placeholder-key",
                "0");
        placeholder.setRegion("");
        return placeholder;
    }

    private static Credentials loadFromFile() {
        final File file = new File(System.getProperty("user.home"), CONFIG_FILE);
        if (!file.exists()) {
            return null;
        }
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            final String text = scanner.useDelimiter("\\A").next();
            JSONObject object = JSONObject.parseObject(text);
            String endpoint = object.getString("endpoint");
            String accessKeyId = object.getString("accessKeyId");
            String accessKey = object.getString("accessKey");
            String aliuid = object.getString("aliuid");
            if (isBlank(endpoint) || isBlank(accessKeyId) || isBlank(accessKey)) {
                return null;
            }
            Credentials credentials = new Credentials(endpoint, accessKeyId, accessKey, aliuid);
            credentials.setRegion(object.getString("region"));
            return credentials;
        } catch (Exception ex) {
            return null;
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private static Credentials loadFromEnv() {
        String endpoint = System.getenv(ENV_ENDPOINT);
        String accessKeyId = System.getenv(ENV_ACCESS_KEY_ID);
        String accessKey = System.getenv(ENV_ACCESS_KEY_SECRET);
        if (isBlank(endpoint) || isBlank(accessKeyId) || isBlank(accessKey)) {
            return null;
        }
        Credentials credentials = new Credentials(endpoint, accessKeyId, accessKey,
                System.getenv(ENV_ALIUID));
        credentials.setRegion(System.getenv(ENV_REGION));
        return credentials;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }
}
