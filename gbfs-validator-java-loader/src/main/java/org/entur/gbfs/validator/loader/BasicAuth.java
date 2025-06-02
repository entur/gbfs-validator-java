package org.entur.gbfs.validator.loader;

public class BasicAuth implements Authentication {
    private final String username;
    private final String password;

    public BasicAuth(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.BASIC;
    }
}
