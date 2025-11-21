package com.raoptimus.keycloak.authenticators;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;

import java.util.List;

public class MultiAttributeUserMatcher extends IdpCreateUserIfUniqueAuthenticator {
    private static final Logger logger = Logger.getLogger(IdpCreateUserIfUniqueAuthenticator.class);

    protected void authenticateImpl(
            AuthenticationFlowContext context,
            SerializedBrokeredIdentityContext serializedCtx,
            BrokeredIdentityContext brokerContext
    ) {
        RealmModel realm = context.getRealm();
        if (context.getAuthenticationSession().getAuthNote("EXISTING_USER_INFO") != null) {
            context.attempted();

            return;
        }

        String username = this.getUsername(context, serializedCtx, brokerContext);
        if (username == null) {
            ServicesLogger.LOGGER.resetFlow(realm.isRegistrationEmailAsUsername() ? "Email" : "Username");
            context.getAuthenticationSession().setAuthNote("ENFORCE_UPDATE_PROFILE", "true");
            context.resetFlow();

            return;
        }

        ExistingUserInfo duplication = checkExistingUser(
                context,
                brokerContext.getUsername(),
                serializedCtx,
                brokerContext
        );
        if (duplication == null) {
            // Пользователь не найден - показываем ошибку
            logger.debugf(
                    "No duplication detected. Creating account for user '%s' and linking with identity provider '%s' .",
                    username,
                    brokerContext.getIdpConfig().getAlias()
            );

            if (context.getExecution().isRequired()) {
                context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
                Response challengeResponse = context.form()
                        .setError("User not found. Please contact administrator.")
                        .createErrorPage(Response.Status.UNAUTHORIZED);
                context.failure(AuthenticationFlowError.ACCESS_DENIED, challengeResponse);

                return;
            }

            context.attempted();

            return;
        }

        // Пользователь найден - связываем аккаунт
        logger.debugf(
                "Duplication detected. There is already existing user with %s '%s' .",
                duplication.getDuplicateAttributeName(),
                duplication.getDuplicateAttributeValue()
        );

        context.getAuthenticationSession().setAuthNote("EXISTING_USER_INFO", duplication.serialize());
        if (context.getExecution().isRequired()) {
            Response challengeResponse = context.form().
                    setError(
                            "federatedIdentityExistsMessage",
                            duplication.getDuplicateAttributeName(),
                            duplication.getDuplicateAttributeValue()
                    ).
                    createErrorPage(Response.Status.CONFLICT);
            context.challenge(challengeResponse);
            context.getEvent().
                    user(duplication.getExistingUserId()).
                    detail(
                            "existing_" + duplication.getDuplicateAttributeName(),
                            duplication.getDuplicateAttributeValue()
                    ).
                    removeDetail("auth_method").
                    removeDetail("auth_type").
                    error("federated_identity_account_exists");

            return;
        }

        context.attempted();
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context,
                              SerializedBrokeredIdentityContext serializedCtx,
                              BrokeredIdentityContext brokerContext) {
        String username = this.getUsername(context, serializedCtx, brokerContext);

        ExistingUserInfo duplication = checkExistingUser(
                context,
                brokerContext.getUsername(),
                serializedCtx,
                brokerContext
        );
        if (duplication == null) {
            // Пользователь не найден - показываем ошибку
            logger.debugf(
                    "No duplication detected. Creating account for user '%s' and linking with identity provider '%s' .",
                    username == null ? "null" : username,
                    brokerContext.getIdpConfig().getAlias()
            );
            context.failure(AuthenticationFlowError.ACCESS_DENIED);

            return;
        }

        // Пользователь найден - связываем аккаунт
        logger.debugf(
                "Duplication detected. There is already existing user with %s '%s' .",
                duplication.getDuplicateAttributeName(),
                duplication.getDuplicateAttributeValue()
        );
        UserModel existingUser = context.getSession().users()
                .getUserById(context.getRealm(), duplication.getExistingUserId());

        context.setUser(existingUser);
        context.success();
    }

    @Override
    protected ExistingUserInfo checkExistingUser(
            AuthenticationFlowContext context,
            String username,
            SerializedBrokeredIdentityContext serializedCtx,
            BrokeredIdentityContext brokerContext
    ) {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();

        logger.info("Available attributes: " + brokerContext.getContextData().keySet());
        logger.info("Phone from getUserAttribute: " + brokerContext.getUserAttribute("phone_number"));
        logger.info("Phone from getContextData: " + brokerContext.getContextData().get("phone_number"));

        // Приоритет 1: Поиск по номеру телефона
        var phoneNumber = brokerContext.getUserAttribute("phone_number");
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            List<UserModel> usersByPhone = session.users().
                    searchForUserByUserAttributeStream(realm, "phoneNumber", phoneNumber).
                    toList();

            if (usersByPhone.size() == 1) {
                UserModel existingUser = usersByPhone.get(0);

                return new ExistingUserInfo(existingUser.getId(), "phoneNumber", phoneNumber);
            }
        }

        // Приоритет 2: Поиск по email
        if (brokerContext.getEmail() != null && !realm.isDuplicateEmailsAllowed()) {
            UserModel existingUser = session.users().getUserByEmail(realm, brokerContext.getEmail());
            if (existingUser != null) {
                return new ExistingUserInfo(existingUser.getId(), UserModel.EMAIL, existingUser.getEmail());
            }
        }

        // Пользователь не найден - возвращаем null (создание отключено)
        return null;
    }
}
