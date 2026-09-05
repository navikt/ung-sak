---
applyTo: "**"
---

# Lokal kjøring av ung-sak

Gjelder når du skal kjøre ung-sak lokalt — typisk for å teste egne endringer mot
verdikjedetestene i `k9-verdikjede`.

## To måter

**Docker:** bygg image (`mvn clean install -DskipTests && docker build -t ung-sak:latest .`) og
pek `.env` i `k9-verdikjede/saksbehandling` dit med `./local-versions.sh ung-sak`. Tester det
byggede imaget.

**På host (raskere iterasjon):** kjør `JettyDevServer` mot støttetjenester i Docker. Tester
arbeidstreet direkte. Se `.github/skills/run-tests/SKILL.md` i `k9-verdikjede` for stack-oppsettet.

## JettyDevServer

Port **8901** — den samme som containeren bruker, så verdikjeden trenger ingen omkonfigurering.

```
Main class:        no.nav.ung.sak.web.server.jetty.JettyDevServer
Working directory: <repo>/web
```

Working directory er det som avgjør konfigurasjonen: `PropertiesUtils` laster
`app-local.properties` og `app-vtp.properties` fra **working directory**, ikke fra classpath.
Er den feil, starter serveren med tomme properties og feiler på en måte som ikke peker mot
årsaken. `JettyDevServer.main` tar ingen argumenter.

### Forutsetninger

- Postgres fra `k9-verdikjede/saksbehandling` (`docker compose up -d postgres`).
  Lokal app bruker `ung_sak`, enhetstester bruker `ung_sak_unit`.
- `~/.modig/{keystore,truststore}.jks` — lages av `k9-verdikjede/keystore/make-dummy-keystore.sh`.
  Kan overstyres med `-Djavax.net.ssl.keyStore` / `-Djavax.net.ssl.trustStore` (passord `vtpvtp`).
  Sertifikatet må være **det samme som vtp bruker**, ellers feiler TLS mot `https://localhost:8063`.
- `UNG_BRUKERDIALOG_API_URL` må **ikke** være satt i skallet — den overstyrer
  `ung.brukerdialog.api.url` fra `app-vtp.properties`.

### Kjøring utenfor IDE

Bygg klassesti med `dependency:build-classpath`, men **kjør full `mvn install` først**:
`-pl web` alene resolverer søskenmoduler fra `~/.m2`, ikke fra reaktoren, så du risikerer å kjøre
gammel kode uten å merke det.

`formidling-pdfgen-templates/target/classes` må ligge **først** på classpath.
`PdfGenKlient.getResource` gjør `Path.of(url.toURI())` og takler ikke `jar:`-URL-er. Uten dette
feiler prosesstasken `formidling.vedtak.brevbestilling` med *«Fant ikke pdfgen-ressurser»*, og
verdikjedetester henger i `ventPåXDokumenterMedBrevkode`. Symptomet ser ut som en brev-feil,
men er en classpath-feil.

## Kontraktversjon mot ung-brukerdialog-api

`ung-brukerdialog-api.version` i `pom.xml` må peke på en versjon som faktisk finnes i `~/.m2`
med de klassene du forventer. Under utvikling brukes SNAPSHOT installert lokalt fra
`ung-brukerdialog-api`:

```bash
cd ../ung-brukerdialog-api
mvn install -DskipTests -Drevision=<versjon> -Dchangelist=-SNAPSHOT
```

Bruk samme versjon som `k9-verdikjede/verdikjede/pom.xml`, ellers kompilerer de to mot ulike
kontrakter. En stale jar gir BUILD SUCCESS lokalt og brekker i CI — verifiser innholdet:

```bash
unzip -l ~/.m2/repository/no/nav/ung/brukerdialog/kontrakt/<v>/kontrakt-<v>.jar | grep <Klasse>
```

**SNAPSHOT-versjoner skal ikke committes.** Bump til faktisk releaseversjon før merge, og husk
at kontrakten må releases før ung-sak kan bygge i CI.
