# AGENTS.md — ung-sak

## Repository Overview
`ung-sak` er en Maven multi-module monolitt for saksbehandling av ungdomsprogramytelsen.

## Architecture
- `behandlingslager`: persistens og domenelagring (Hibernate/JPA, repositories).
- `domenetjenester`: domene- og forretningslogikk.
- `behandlingsprosess`: orkestrering/tilstandsmaskin for behandling.
- `web`: Jetty/Jersey REST API med OpenAPI-annotasjoner.
- `migreringer`: Flyway-migreringer og databaseskjema-initialisering.
- `formidling` og `formidling-pdfgen-templates`: dokumentgenerering.

## Tech Stack
- Java 25 (hovedkodebase)
- Java 21 (`kodeverk` og `kontrakt`)
- Maven
- PostgreSQL
- Flyway
- Jetty 12 + Jersey

## Build & Test Commands
```bash
mvn clean install
mvn test
mvn clean package -DskipTests
dev/generate-openapi-ts-client.sh
```

## Local Dev Prerequisites
- PostgreSQL maa vaere oppe med schema `ung_sak_unit` for tester.
- Lokal app bruker typisk schema `ung_sak`.
- Start DB via `k9-verdikjede/saksbehandling` (se `README.md`).
- Kjor `no.nav.ung.sak.db.util.Databaseskjemainitialisering` eller `mvn test-compile` ved behov for skjemaendringer.
- Lokal server startes via `JettyDevServer --vtp`.

## Conventions
- Hold dependency-versjoner sentralt i root `pom.xml` med mindre modulspesifikk grunn finnes.
- Unngaa scopes i `dependencyManagement` med mindre eksisterende mønster tilsier det.
- API-endringer i `web` skal oppdatere OpenAPI og ved behov regenerere TypeScript-klient.
- Flyway-migreringer skal følge eksisterende naming/orden i `migreringer/src/main/resources/db/migration`.
- Behold pakke- og navnekonvensjoner under `no.nav.ung.sak.<module>...` (`*Repository`, `*Tjeneste`, `*Steg`).
- Bruk `Range<LocalDate>` med `@Type(PostgreSQLRangeType.class)` og `columnDefinition = "daterange"` for perioder i JPA-entiteter.
- I nye Flyway-migreringer skal SQL skrives med lowercase. Eksisterende migreringer trenger ikke omskrives kun for casing.

## Boundaries
### Always
- Folg etablerte mønstre i modulen du endrer.
- Hold endringer smaaa og fokuserte.
- Kjor relevante tester for berorte moduler.

### Ask First
- Store refaktoreringer paa tvers av modulgrenser.
- Innforing av nye rammeverk eller infrastrukturavhengigheter.
- Endringer i deploy/CI-oppsett uten tydelig behov.

### Never
- Commit hemmeligheter, tokens eller credentials.
- Bryt Java 21-kompatibilitet i `kodeverk`/`kontrakt`.
- Lag migreringer som ikke er Flyway-kompatible.

## Key References
- `README.md`
- `.github/workflows/build.yaml`
- `dokumentasjon/arkitekturbeslutninger.md`
- `migreringer/pom.xml`
- `web/README.md`
