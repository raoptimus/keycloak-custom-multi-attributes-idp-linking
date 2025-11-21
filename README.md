# Keycloak Custom Multi Attributes IDP Linking

# Keycloak Phone or Email User Matcher

Keycloak authenticator for linking external identity provider users to existing local accounts using phone number or email with priority-based matching.

## Overview

The standard Keycloak First Broker Login flow only matches users by username and email. This extension extends the matching logic to support:

- **Priority-based matching**: Phone number first, then email
- **No automatic user creation**: Only links to existing users
- **Flexible configuration**: Works with any external Identity Provider (OIDC, SAML, etc.)

This is useful when:
- Your external IdP provides phone numbers in addition to email
- You want phone number to take precedence for user matching
- You need to prevent automatic user creation and only allow linking to pre-existing accounts

## Features

✅ Searches for users by phone number attribute (priority 1)  
✅ Falls back to email matching (priority 2)  
✅ Prevents creation of duplicate users  
✅ Works with standard Keycloak Handle Existing Account flow  
✅ Configurable through Keycloak Authentication Flow UI

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

Tested on Keycloak `22.0.3.

### Keycloak >= v22.0.3

After Packaging the project with,

```sh
mvn package -f "./pom.xml"
```

deploy the `keycloak-custom-multi-attributes-idp-linking-{version}.jar` to `/opt/keycloak/providers` and rebuild keycloak to bring this provider in.

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
sudo /opt/keycloak/bin/kc.sh start --auto-build;
```

**build only:**

```shell
/opt/keycloak/bin/kc.sh build
```

## Configuration

### 1. Configure Identity Provider Mappers

Ensure your external Identity Provider maps the phone number claim to a user attribute.

**Example for OIDC Provider:**

Navigate to: `Identity Providers → [Your Provider] → Mappers`

Create a new mapper:
- **Mapper Type**: Attribute Importer
- **Claim**: `phone_number` (or `phone` depending on your IdP)
- **User Attribute**: `phoneNumber`
- **Sync Mode Override**: Force

Don't forget to add the `phone` scope to your Identity Provider:

Advanced → Default Scopes: openid profile email phone

### 2. Create Custom Authentication Flow

Navigate to: `Authentication → Flows`

1. Click **Copy** on the **First Broker Login** flow
2. Name it **Phone or Email Linking**
3. Delete or disable **Create User If Unique** (to prevent user creation)
4. Add execution: **Phone or Email User Matcher** (from this provider)
5. Set it as **REQUIRED** or **ALTERNATIVE** depending on your needs:
    - **REQUIRED**: Shows error if user not found
    - **ALTERNATIVE**: Passes to next step (e.g., Handle Existing Account)

**Example Flow Structure (Auto-linking):**

Phone or Email Linking
├── Phone or Email User Matcher (ALTERNATIVE)
└── Handle Existing Account (ALTERNATIVE)
└── Automatically Set Existing User (REQUIRED)

**Example Flow Structure (No user creation):**

Phone or Email Linking
└── Phone or Email User Matcher (REQUIRED)

### 3. Configure Identity Provider

Navigate to: `Identity Providers → [Your Provider] → Advanced`

Set **First Login Flow Override** to: `Phone or Email Linking`

### 4. Verify Configuration

1. Ensure existing users in Keycloak have the `phoneNumber` attribute populated
2. Test login with external IdP
3. Check that users are correctly linked based on phone or email

## How It Works

### Matching Priority

1. **Phone Number** (priority 1)
    - Searches for users with matching `phoneNumber` attribute
    - If exactly one user found → link account

2. **Email** (priority 2)
    - If phone not found or no match, searches by email
    - If user found → link account

3. **No Match**
    - If configured as REQUIRED → Shows error, prevents login
    - If configured as ALTERNATIVE → Passes to next authenticator

### Username Handling

If the external IdP doesn't provide a username, the authenticator triggers the **Review Profile** screen where the user can enter missing information.

## Troubleshooting

### User not found

**Problem**: External IdP user cannot log in

**Solution**:
- Verify the user exists in Keycloak with correct `phoneNumber` or `email` attribute
- Check IdP mapper configuration
- Verify `phone` scope is requested from external IdP

### Phone number not matched

**Problem**: Matching by email works, but not by phone

**Solution**:
- Ensure the claim name in IdP is `phone_number` or `phone`
- Verify the mapper imports to `phoneNumber` attribute (case-sensitive)
- Check that existing users have `phoneNumber` attribute set

### Duplicate users created

**Problem**: New users are created instead of linking

**Solution**:
- Ensure **Create User If Unique** is disabled in your flow
- Set **Phone or Email User Matcher** before any user creation steps
- Use **REQUIRED** execution requirement to prevent fallback
