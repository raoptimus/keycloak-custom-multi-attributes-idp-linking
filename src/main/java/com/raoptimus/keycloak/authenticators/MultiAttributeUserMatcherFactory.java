package com.raoptimus.keycloak.authenticators;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.models.KeycloakSession;

public class MultiAttributeUserMatcherFactory extends IdpCreateUserIfUniqueAuthenticatorFactory {

    public static final String PROVIDER_ID = "multi-attribute-user-matcher";
    static MultiAttributeUserMatcher SINGLETON = new MultiAttributeUserMatcher();

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Match User by Phone or Email";
    }
}
