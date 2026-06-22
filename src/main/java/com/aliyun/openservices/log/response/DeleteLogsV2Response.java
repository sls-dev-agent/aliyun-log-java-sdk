package com.aliyun.openservices.log.response;

import java.util.Map;

/**
 * Response for the DeleteLogs V2 API.
 */
public class DeleteLogsV2Response extends Response {

    private static final long serialVersionUID = -7997683404046156396L;

    private long affectedRows;

    public DeleteLogsV2Response(Map<String, String> headers, long affectedRows) {
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
