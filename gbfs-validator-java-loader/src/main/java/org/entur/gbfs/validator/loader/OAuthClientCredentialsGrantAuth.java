package org.entur.gbfs.validator.loader;

public class OAuthClientCredentialsGrantAuth implements Authentication {
    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;

    public OAuthClientCredentialsGrantAuth(String clientId, String clientSecret, String tokenUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.OAUTH_CLIENT_CREDENTIALS;
    }
}
