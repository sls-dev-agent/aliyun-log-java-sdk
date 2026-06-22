package com.aliyun.openservices.log.response;

import java.util.Map;

/**
 * Response for the UpdateLogs API.
 */
public class UpdateLogsResponse extends Response {

    private static final long serialVersionUID = 5034159327400589698L;

    private long affectedRows;

    public UpdateLogsResponse(Map<String, String> headers, long affectedRows) {
        super(headers);
        this.affectedRows = affectedRows;
    }

    public long getAffectedRows() {
        return affectedRows;
    }

    public void setAffectedRows(long affectedRows) {
        this.affectedRows = affectedRows;
    }
}
