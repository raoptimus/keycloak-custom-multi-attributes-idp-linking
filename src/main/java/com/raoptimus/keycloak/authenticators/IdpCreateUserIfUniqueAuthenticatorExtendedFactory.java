package com.raoptimus.keycloak.authenticators;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class IdpCreateUserIfUniqueAuthenticatorExtendedFactory implements AuthenticatorFactory {
    public static final String REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION = "require.password.update.after.registration";
    public static final String PROVIDER_ID = "idp-create-user-if-unique-extended";
    static IdpCreateUserIfUniqueAuthenticatorExtended SINGLETON = new IdpCreateUserIfUniqueAuthenticatorExtended();
    public static final String CONFIG_LOOKUP_ATTRIBUTE_PHONE = "lcuiue-lookup-attribute-phone";
    public static final String ATTRIBUTE_DEFAULT_VALUE_PHONE = "phoneNumber";

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getReferenceCategory() {
        return "createUserIfUnique";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getDisplayType() {
        return "Create User If Unique Extended";
    }

    @Override
    public String getHelpText() {
        return "Detect if there is existing Keycloak account with same phone numeber or email like identity provider." +
                " If no, create new user";
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION);
        property.setLabel("Require Password Update After Registration");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("If this option is true and new user is successfully imported " +
                "from Identity Provider to Keycloak (there is no duplicated " +
                "phone number or email or username detected in Keycloak DB), " +
                "then this user is required to update his password");
        configProperties.add(property);

        var property2 = new ProviderConfigProperty();
        property2.setName(CONFIG_LOOKUP_ATTRIBUTE_PHONE);
        property2.setType(ProviderConfigProperty.STRING_TYPE);
        property2.setLabel("Lookup attribute phone number");
        property2.setHelpText("User attribute with phone number used to compare to identity provider attribute.");
        property2.setDefaultValue(ATTRIBUTE_DEFAULT_VALUE_PHONE);
        configProperties.add(property2);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
}
