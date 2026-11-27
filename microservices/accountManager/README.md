# AccountManager

Keycloak handles sign-in and issues OpenID Connect tokens for the first OAuth integration. AccountManager is a Spring Security resource server. It fetches Keycloak's JSON Web Key Set (JWKS), caches the verification keys, and checks RS256 signatures, issuer, audience, and token timestamps locally. The earlier SOAP public-key endpoint has been retired in favor of the provider's JWKS.

## Run locally

With Java 21, Maven, and Docker running, use these commands from the repository root:

```powershell
mvn -f microservices/accountManager/pom.xml clean verify
docker compose up --build -d keycloak account-manager
```

Keycloak starts in development mode at `http://localhost:8082`. For a new Keycloak data volume, set `KC_BOOTSTRAP_ADMIN_USERNAME` and `KC_BOOTSTRAP_ADMIN_PASSWORD` in your shell before starting Compose to create the initial admin. Use credentials unique to your local environment. The root Compose file imports the `choose-your-fate` realm on first start and persists it in `keycloak-data`. Keycloak skips the import when the realm already exists.

In the Keycloak admin console, create a test user in the `choose-your-fate` realm and set its password. The imported public client `choose-your-fate-web` allows Authorization Code with PKCE (S256). Its registered callback is `http://localhost:5173/auth/callback`. Use a client that handles that callback to sign in and obtain an access token. The project frontend still needs this client integration.

| Setting | Local value |
| --- | --- |
| Issuer | `http://localhost:8082/realms/choose-your-fate` |
| Public client ID | `choose-your-fate-web` |
| AccountManager audience | `account-manager` |
| Discovery | `http://localhost:8082/realms/choose-your-fate/.well-known/openid-configuration` |

The realm mapper adds the AccountManager audience to access tokens. Requests use a bearer **access token**:

```powershell
curl.exe --fail --header "Authorization: Bearer $accessToken" http://localhost:8092/api/accounts/me
```

A valid token returns the issuer, subject, and preferred username from its claims. Missing or rejected tokens return HTTP 401. This endpoint is the starting point for a profile API. When adding persistence, use the issuer and subject together to link a profile to the identity provider.

## Configuration

Set `OIDC_ISSUER_URI`, `OIDC_JWKS_URI`, and `OIDC_AUDIENCE` to use another provider or environment. Compose uses Keycloak's browser-visible issuer and fetches keys over the internal network at `http://keycloak:8080`. If you run AccountManager directly with `mvn spring-boot:run`, it uses the issuer URL to build the default JWKS URL. If you run only the AccountManager Compose file, supply a JWKS URL reachable from its container.

The included Keycloak configuration is for local development. A deployed provider needs its own HTTPS issuer, exact redirect URIs, and durable managed storage.

## Remaining integration

- Connect the frontend to Keycloak using Authorization Code with PKCE and route AccountManager traffic through the gateway.
- Add AccountManager's profile database and map existing account IDs to Keycloak identities before migrating ownership checks.
- Move RecapManager and other services to provider JWKS after deciding their audience and role mapping. RecapManager currently uses the PEM-based configuration and role claims from its existing setup.
