# AccountManager

AccountManager owns accounts, login, and RS256 JWT issuance in [Patrick's architecture revision](../../architecture-chooseYourFate-Patrick-revision1.drawio). It will own the MySQL Account vault. The API Gateway and each protected service verify tokens locally.

This scaffold implements public verification key distribution. The gateway and protected services can retrieve the configured key through a versioned SOAP contract during provisioning or a controlled refresh. Account persistence, login, and signing are future work for this service.

| Component | Responsibility |
| --- | --- |
| AccountManager | Owns the Account vault, authenticates users, and signs JWTs in the draft. The scaffold serves the configured RSA public key, issuer, audience, and algorithm. |
| API Gateway | Verifies JWTs, enforces route policy, and applies routing and rate limits. |
| Protected services | Verify JWTs locally and enforce their own domain rules, such as character ownership. |

The browser continues to use the gateway's HTTP APIs. Queue-based game events continue on the broker.

## Run locally

From this directory with Java 21 and Maven, place an X.509/SPKI RSA public key of at least 2048 bits in `.local/account-jwt-public.pem`. Use the `BEGIN PUBLIC KEY` PEM format. For a local smoke run, generate a throwaway key pair outside the repository and copy only its public key to this directory.

| Environment variable | Default |
| --- | --- |
| `JWT_PUBLIC_KEY_FILE` | `./.local/account-jwt-public.pem` |
| `JWT_ISSUER` | `http://localhost:8092` |
| `JWT_AUDIENCE` | `choose-your-fate` |
| `ACCOUNT_MANAGER_BIND_ADDRESS` | `127.0.0.1` |
| `ACCOUNT_MANAGER_PORT` | `8092` |

```powershell
mvn test
mvn spring-boot:run
```

The tests generate their own key and exercise the contract over HTTP. The service loads the configured public key at startup and exits if it cannot read or validate it.

## Run with Docker Compose

With Docker Desktop running, build the JAR and start Compose from this directory. Compose publishes port `8092` on the host's loopback interface and mounts the public key read-only. Place the key at `.local/account-jwt-public.pem` or set `JWT_PUBLIC_KEY_FILE` to another host path. The file must be readable by the container's non-root user.

```powershell
mvn package
docker compose up --build
```

The root `docker-compose.yml` also includes AccountManager. After building its JAR and provisioning the key, run `docker compose up --build` from the repository root to start it with the default stack. Use `--profile prod` to also enable the profiled applications after preparing their build artifacts and settings. If AccountManager and RecapManager use the same public-key file, set `JWT_PUBLIC_KEY_FILE` to its absolute host path so both mounts resolve to the same file.

## SOAP v1

Import [the running WSDL](http://127.0.0.1:8092/ws/account-v1.wsdl) from `GET /ws/account-v1.wsdl`. Spring Web Services generates it from [the v1 XSD](src/main/resources/contracts/account-v1.xsd). The `GetVerificationKey` operation accepts an empty request and returns `issuer`, `audience`, `algorithm` (RS256), and `publicKey` (base64-encoded DER SubjectPublicKeyInfo). A Java client can load the decoded bytes using `X509EncodedKeySpec` and an RSA `KeyFactory`.

Send this envelope to `POST /ws` with `Content-Type: text/xml; charset=utf-8` and `SOAPAction: ""`.

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:acc="urn:choose-your-fate:account:v1">
  <soap:Body>
    <acc:GetVerificationKeyRequest/>
  </soap:Body>
</soap:Envelope>
```

## Integration steps

1. Move account persistence and login into AccountManager with its own MySQL Account vault. Issue RS256 tokens with matching `iss` and `aud`, a stable account identifier in `sub`, and an expiry. RecapManager already expects RS256 and recognizes `ROLE_USER` and `ROLE_ADMIN` in a single `role` claim. The legacy backend currently signs HMAC tokens, so plan the issuer migration before connecting consumers.
2. Provision the public verification key and configure the issuer and audience to match the tokens AccountManager will issue. Deploy the SOAP endpoint on an internal network with authenticated TLS and access controls at the ingress or service mesh. The scaffold serves public metadata over HTTP and relies on deployment to enforce transport and caller identity.
3. Wire the gateway first. Generate a SOAP client from the WSDL and call it during provisioning or refresh. Configure the trusted endpoint, expected issuer and audience, and RS256 in the consumer. Retain the last trusted key through a transient outage and require a trusted key before accepting requests. RecapManager can initially keep using its configured PEM file.
4. Before enabling automatic rotation, extend the contract to return multiple keys with `kid` values, then test the overlap window against token expiry and consumer refresh timing. This outline serves one configured key per process lifetime.
