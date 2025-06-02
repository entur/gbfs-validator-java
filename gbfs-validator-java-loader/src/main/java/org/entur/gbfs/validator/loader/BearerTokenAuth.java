package org.entur.gbfs.validator.loader;

public class BearerTokenAuth implements Authentication {
    private final String token;

    public BearerTokenAuth(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.BEARER_TOKEN;
    }
}
