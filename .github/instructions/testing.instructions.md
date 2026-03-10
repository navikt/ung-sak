---
applyTo: "**/*Test.java"
---

# Testing (ung-sak)

## Test Stack
- Bruk JUnit 5, Mockito og AssertJ i traaad med eksisterende tester.
- Foelg navnekonvensjon `*Test.java` i `src/test/java`.

## Teststil
- Test observerbar adferd, ikke intern implementasjon.
- Bruk arrange/act/assert-struktur.
- Hold tester deterministiske; unngaa tid, random og delt global state uten kontroll.
- Bruk eksisterende testutil/buildere der de finnes (saerlig i `behandlingslager/testutil`).

## Databasenaere tester
- Ved behov for database: bruk etablerte repo-moenstre og init-flyt i prosjektet.
- Husk at lokale tester krever Postgres/schema (`ung_sak_unit`) satt opp.

## What To Validate
- Happy path + relevante feilstier.
- Grenseverdier for perioder/datoer/satser.
- Sideeffekter som repository-endringer, prosess-stegutfall og publisering av task/hendelser.
- For API-lag: statuskode, payload og valideringsfeil.

## Boundaries
### Always
- Kjoer relevante tester i berorte moduler foer ferdigstillelse.
- Oppdater/legg til tester naaar produksjonskode endres.

### Ask First
- Stor omskriving av eksisterende teststrategi.
- Nye tunge integrasjonstester som paavirker byggetid vesentlig.

### Never
- Commit kode med knekte tester i berorte moduler.
- Fjerne tester for aa faa groent bygg.
