package com.example.linkup.util;

public class SessionDetails {
    private String sessionId;
    private long loginTimestamp;

    public SessionDetails(String sessionId, long loginTimestamp) {
        this.sessionId = sessionId;
        this.loginTimestamp = loginTimestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getLoginTimestamp() {
        return loginTimestamp;
    }
}
