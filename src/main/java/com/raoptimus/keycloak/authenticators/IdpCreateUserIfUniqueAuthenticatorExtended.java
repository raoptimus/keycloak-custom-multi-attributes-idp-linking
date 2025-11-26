package com.raoptimus.keycloak.authenticators;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;

public class IdpCreateUserIfUniqueAuthenticatorExtended extends IdpCreateUserIfUniqueAuthenticator {
    private static final Logger logger = Logger.getLogger(IdpCreateUserIfUniqueAuthenticator.class);

    @Override
    protected ExistingUserInfo checkExistingUser(
            AuthenticationFlowContext context,
            String username,
            SerializedBrokeredIdentityContext serializedCtx,
            BrokeredIdentityContext brokerContext
    ) {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();

        AuthenticatorConfigModel config = validateConfig(context);
        String attributePhoneNumber = IdpCreateUserIfUniqueAuthenticatorExtendedFactory.ATTRIBUTE_DEFAULT_VALUE_PHONE;
        if ( config != null && config.getConfig() != null) {
            attributePhoneNumber = config.getConfig().get(IdpCreateUserIfUniqueAuthenticatorExtendedFactory.CONFIG_LOOKUP_ATTRIBUTE_PHONE);
        }

        logger.info("Available attributes: " + brokerContext.getContextData().keySet());
        logger.info("Phone from getUserAttribute: " + brokerContext.getUserAttribute(attributePhoneNumber));
        logger.info("Phone from getContextData: " + brokerContext.getContextData().get(attributePhoneNumber));

        var phoneNumber = brokerContext.getUserAttribute(attributePhoneNumber);
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            List<UserModel> usersByPhone = session.users().
                    searchForUserByUserAttributeStream(realm, attributePhoneNumber, phoneNumber).
                    toList();
            if (usersByPhone.size() == 1) {
                UserModel existingUser = usersByPhone.get(0);

                return new ExistingUserInfo(existingUser.getId(), attributePhoneNumber, phoneNumber);
            }

            if (phoneNumber.startsWith("+")) {
                phoneNumber = phoneNumber.substring(1);
            } else {
                phoneNumber = "+" + phoneNumber;
            }

            usersByPhone = session.users().
                    searchForUserByUserAttributeStream(realm, attributePhoneNumber, phoneNumber).
                    toList();
            if (usersByPhone.size() == 1) {
                UserModel existingUser = usersByPhone.get(0);

                return new ExistingUserInfo(existingUser.getId(), attributePhoneNumber, phoneNumber);
            }
        }

        if (brokerContext.getEmail() != null && !context.getRealm().isDuplicateEmailsAllowed()) {
            UserModel existingUser = context.getSession().users().getUserByEmail(context.getRealm(), brokerContext.getEmail());
            if (existingUser != null) {
                return new ExistingUserInfo(existingUser.getId(), UserModel.EMAIL, existingUser.getEmail());
            }
        }

        UserModel existingUser = context.getSession().users().getUserByUsername(context.getRealm(), username);
        if (existingUser != null) {

            return new ExistingUserInfo(existingUser.getId(), UserModel.USERNAME, existingUser.getUsername());
        }

        return null;
    }

    private AuthenticatorConfigModel validateConfig(AuthenticationFlowContext context) {
        if (context.getAuthenticatorConfig() == null) {
            logger.warn("Config must not be empty.");

            return null;
        }
        if (context.getAuthenticatorConfig().getConfig() == null) {
            logger.warn("Config must not be empty.");

            return null;
        }

        return context.getAuthenticatorConfig();
    }
}
