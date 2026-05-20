package com.aliyun.openservices.log.testing;

import com.aliyun.openservices.log.http.client.HttpMethod;
import com.aliyun.openservices.log.http.comm.ServiceClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent matcher used by {@link FakeServiceClient} to decide whether a captured
 * {@link ServiceClient.Request} should be served by a particular stub.
 *
 * <p>All criteria are AND-ed together. Unspecified criteria are not checked.
 * Path matching is prefix-based on the URI path component (host/scheme/query
 * are ignored unless the corresponding methods are called). Header keys are
 * matched case-insensitively.</p>
 *
 * <pre>
 *   RequestMatcher.method("POST").path("/logstores/foo/shards/lb")
 * </pre>
 *
 * <p>This class is part of the test-only "testing" toolkit and is intended to
 * be copy-paste-reused in sibling SDK projects (java-producer, consumer-java).
 * The public API is therefore intentionally minimal and stable.</p>
 */
public final class RequestMatcher {

    private HttpMethod method;
    private String pathPrefix;
    private final Map<String, String> queryParams = new LinkedHashMap<String, String>();
    private final Map<String, String> headers = new LinkedHashMap<String, String>();
    private boolean matchAny;

    private RequestMatcher() {
    }

    /** Matcher that accepts every request. Useful as a default-fallback stub. */
    public static RequestMatcher any() {
        RequestMatcher matcher = new RequestMatcher();
        matcher.matchAny = true;
        return matcher;
    }

    /** New matcher requiring the given HTTP method (case-insensitive). */
    public static RequestMatcher method(String method) {
        RequestMatcher matcher = new RequestMatcher();
        matcher.method = HttpMethod.valueOf(method.toUpperCase());
        return matcher;
    }

    /** Restrict the URI path (matched as a prefix). */
    public RequestMatcher path(String prefix) {
        this.pathPrefix = prefix;
        return this;
    }

    /** Require query parameter {@code key=value}. */
    public RequestMatcher query(String key, String value) {
        this.queryParams.put(key, value);
        return this;
    }

    /** Require header {@code key: value} (case-insensitive key). */
    public RequestMatcher header(String key, String value) {
        this.headers.put(key.toLowerCase(), value);
        return this;
    }

    /** Returns {@code true} when the supplied request satisfies all criteria. */
    public boolean matches(ServiceClient.Request request) {
        if (matchAny) {
            return true;
        }
        if (method != null && method != request.getMethod()) {
            return false;
        }
        URI uri;
        try {
            uri = new URI(request.getUri());
        } catch (URISyntaxException ex) {
            return false;
        }
        if (pathPrefix != null) {
            String path = uri.getRawPath();
            if (path == null || !path.startsWith(pathPrefix)) {
                return false;
            }
        }
        if (!queryParams.isEmpty()) {
            Map<String, String> actual = parseQuery(uri.getRawQuery());
            for (Map.Entry<String, String> e : queryParams.entrySet()) {
                if (!e.getValue().equals(actual.get(e.getKey()))) {
                    return false;
                }
            }
        }
        if (!headers.isEmpty()) {
            Map<String, String> reqHeaders = lowerCaseKeys(request.getHeaders());
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (!e.getValue().equals(reqHeaders.get(e.getKey()))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> out = new HashMap<String, String>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq >= 0) {
                out.put(pair.substring(0, eq), pair.substring(eq + 1));
            } else {
                out.put(pair, "");
            }
        }
        return out;
    }

    private static Map<String, String> lowerCaseKeys(Map<String, String> in) {
        Map<String, String> out = new HashMap<String, String>();
        if (in == null) {
            return out;
        }
        for (Map.Entry<String, String> e : in.entrySet()) {
            out.put(e.getKey().toLowerCase(), e.getValue());
        }
        return out;
    }

    @Override
    public String toString() {
        if (matchAny) {
            return "RequestMatcher{any}";
        }
        StringBuilder sb = new StringBuilder("RequestMatcher{");
        if (method != null) {
            sb.append("method=").append(method).append(' ');
        }
        if (pathPrefix != null) {
            sb.append("path^=").append(pathPrefix).append(' ');
        }
        if (!queryParams.isEmpty()) {
            sb.append("query=").append(queryParams).append(' ');
        }
        if (!headers.isEmpty()) {
            sb.append("headers=").append(headers).append(' ');
        }
        return sb.append('}').toString();
    }
}
