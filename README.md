# IdpCreateUserIfUniqueAuthenticatorExtended

Custom Keycloak authenticator extending the default `IdpCreateUserIfUniqueAuthenticator` to support configurable phone-based user matching (with fallback to email and username) during Identity Provider brokering.

## Overview

By default, Keycloak’s First Broker Login flow matches existing users primarily by username and email.  
`IdpCreateUserIfUniqueAuthenticatorExtended` enhances this behaviour by:

- Allowing lookup by a **configurable phone attribute** (e.g. `phoneNumber`, `mobile`, etc.)
- Supporting **phone normalization** with/without leading `+`
- Falling back to **email** and then **username**
- Reusing the standard user creation/linking logic of the base authenticator
- Providing **detailed logging** to simplify debugging

This is useful when:

- Your external IdP sends a phone number as a primary or more reliable identifier
- Phone numbers may come with or without a leading plus sign
- You want to avoid duplicate users while still using the standard Keycloak brokering flow

## Features

- 🔧 Configurable lookup attribute for phone (via authenticator config)
- 📞 Phone-first matching, then email, then username
- 📐 Phone normalization: handles numbers with and without leading `+`
- 🔁 Fully compatible with standard `IdpCreateUserIfUniqueAuthenticator` behaviour
- 🧩 Can be combined with standard steps like **Detect Existing Broker User** and **Automatically Set Existing User**
- 🪵 Extra logging of received attributes and matching logic

## Development

```shell
mvn clean install
```

```shell
docker-compose up
```

Update Plugin in container by running ```mvn install```.

Attach remote jvm debug session on port 5005 (default).

## Installation

Tested on Keycloak `26.1.3.

### Keycloak >= v26.1.3

After Packaging the project with,

```sh
mvn package -f "./pom.xml"
```

deploy the `keycloak-custom-multi-attributes-idp-linking-v1.1.0.jar` to `/opt/keycloak/providers` and rebuild keycloak to bring this provider in.

#### Deploy the provider

Create providers directory if it doesn't exist
```sh
[ ! -d "/opt/keycloak/providers" ] && sudo mkdir /opt/keycloak/providers;
```

Copy the JAR file
```sh
sudo mv keycloak-phone-email-matcher-{version}.jar /opt/keycloak/providers/;
```

#### Rebuild and Restart Keycloak

**All-in-one (recommended):**

```sh
sudo /opt/keycloak/bin/kc.sh start --auto-build --debug;
```

**build only:**

```shell
/opt/keycloak/bin/kc.sh build
```

## Configuration

### Enable the Authenticator in Flow

- Go to **Authentication → Flows**
- Copy the **First Broker Login** flow and name it (e.g., `Phone First Broker Login`)
- Replace or add the step for user creation/linking:
   - Remove original **Create User If Unique**
   - Add or replace with **IdpCreateUserIfUniqueAuthenticatorExtended**
- Save the flow

### Configure Phone Attribute

- Edit the execution of `IdpCreateUserIfUniqueAuthenticatorExtended` in the flow
- Create or edit its **Authenticator Config**
- Set config key (defined in Factory, e.g., `lookup.attribute.phone`) to the phone attribute name you want to use (`phoneNumber` by default)

### Configure Identity Provider Mappers

- Go to **Identity Providers → [Your Provider] → Mappers**
- Add or edit mapper to import phone number claim:
   - Mapper Type: Attribute Importer
   - Claim: `phone_number` (or as per your IdP)
   - User Attribute: must match the authenticator config, e.g., `phoneNumber`
   - Sync Mode Override: Force

- Ensure the `phone` scope is added in the IdP scopes:
openid profile email phone

### Bind Flow to Identity Provider

- Go to **Identity Providers → [Your Provider] → Advanced**
- Set **First Login Flow Override** to your new flow (e.g., `Phone First Broker Login`)

## Example Flow

Phone First Broker Login

├── Detect Existing Broker User (REQUIRED)

├── IdpCreateUserIfUniqueAuthenticatorExtended (REQUIRED or ALTERNATIVE)

├── Handle Existing Account (REQUIRED or ALTERNATIVE)

└── Automatically Set Existing User (REQUIRED)

This flow:

- Checks for already linked users using Detect Existing Broker User
- Finds user by phone/email/username using extended authenticator
- Automatically sets existing user when found

Disable or remove user creation steps to prevent duplicates.
