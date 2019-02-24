package com.xlong.xrpc.protocol;

public class RPCResponse {
    private String requestId;
    private String error;
    private Object result;

    public String getRequestId() {
        return requestId;
    }

    public String getError() {
        return error;
    }

    public Object getResult() {
        return result;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "RPCResponse{" +
                "requestId='" + requestId + '\'' +
                ", error='" + error + '\'' +
                ", result=" + result +
                '}';
    }
}
