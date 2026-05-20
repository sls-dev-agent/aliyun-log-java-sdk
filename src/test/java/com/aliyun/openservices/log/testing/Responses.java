package com.aliyun.openservices.log.testing;

import com.aliyun.openservices.log.common.Consts;
import com.aliyun.openservices.log.http.comm.ResponseMessage;
import com.google.protobuf.Message;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Factory of canned {@link ResponseMessage} instances for use with
 * {@link FakeServiceClient}.
 *
 * <p>Every helper returns a fresh {@link ResponseMessage} so a stub can be
 * served multiple times safely. Use {@link FakeServiceClient.ResponseSupplier}
 * directly when the response must vary across invocations.</p>
 */
public final class Responses {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private Responses() {
    }

    /** 200 OK with the given raw bytes as body. */
    public static ResponseMessage ok(byte[] body) {
        return bytes(200, body, defaultHeaders());
    }

    /** 200 OK with a UTF-8 JSON body and {@code Content-Type: application/json}. */
    public static ResponseMessage ok(String json) {
        Map<String, String> headers = defaultHeaders();
        headers.put(Consts.CONST_CONTENT_TYPE, "application/json");
        return bytes(200, json == null ? new byte[0] : json.getBytes(UTF8), headers);
    }

    /** 200 OK with a serialized protobuf body and {@code Content-Type: application/x-protobuf}. */
    public static ResponseMessage okProto(Message proto) {
        Map<String, String> headers = defaultHeaders();
        headers.put(Consts.CONST_CONTENT_TYPE, "application/x-protobuf");
        return bytes(200, proto.toByteArray(), headers);
    }

    /**
     * Build an SLS-style error envelope: {@code {"errorCode":..,"errorMessage":..}}.
     * The Client unwraps this and throws {@code LogException} with the matching
     * error code / message.
     */
    public static ResponseMessage error(int statusCode, String errorCode, String errorMessage) {
        String body = "{\"errorCode\":\"" + escape(errorCode)
                + "\",\"errorMessage\":\"" + escape(errorMessage) + "\"}";
        Map<String, String> headers = defaultHeaders();
        headers.put(Consts.CONST_CONTENT_TYPE, "application/json");
        return bytes(statusCode, body.getBytes(UTF8), headers);
    }

    /**
     * Generic response builder. The supplied {@code headers} map is copied.
     *
     * <p>Sets both the streamed content (so tests calling {@code Client.*} go
     * through the normal parse path) <em>and</em> the raw body bytes (so tests
     * calling {@code FakeServiceClient.sendRequest} directly can read the body
     * via {@link ResponseMessage#GetRawBody()} without having to consume the
     * stream themselves).</p>
     */
    public static ResponseMessage bytes(int status, byte[] body, Map<String, String> headers) {
        ResponseMessage resp = new ResponseMessage();
        resp.setStatusCode(status);
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                resp.addHeader(e.getKey(), e.getValue());
            }
        }
        byte[] safe = body == null ? new byte[0] : body;
        resp.setContent(new ByteArrayInputStream(safe));
        resp.setContentLength(safe.length);
        resp.SetBody(safe);
        return resp;
    }

    private static Map<String, String> defaultHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(Consts.CONST_X_SLS_REQUESTID, UUID.randomUUID().toString());
        return headers;
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
