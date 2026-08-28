---
name: Testing Standards
description: "Testprinsipper for ung-sak (Java/JUnit 5/Mockito/AssertJ)."
applyTo: "**/*Test.java"
excludeAgent: "code-review"
---

# Testing (ung-sak)

## Test Stack

- Bruk JUnit 5, Mockito og AssertJ i traad med eksisterende tester.
- Foelg navnekonvensjon `*Test.java` i `src/test/java`.

## Teststil

- Test observerbar adferd, ikke intern implementasjon.
- Bruk arrange/act/assert-struktur.
- Hold tester deterministiske; unngaa tid, random og delt global state uten kontroll.
- Bruk eksisterende testutil/buildere der de finnes (saerlig i `behandlingslager/testutil`).
- Beskrivende testnavn som forklarer forventet adferd, f.eks. `skal_avslaa_vilkaar_naar_periode_er_utenfor_maksdato` — ikke `test1` eller `testValidering`.

## Test Strategy

Velg testniva basert paa hva som skal verifiseres:

| Hva som testes | Testniva | Verktoy |
|---|---|---|
| Rene funksjoner, utledere, mapping | Enhetstest | JUnit 5 + AssertJ |
| Tjeneste med avhengigheter mocket | Enhetstest | JUnit 5 + Mockito |
| Repository + SQL | Integrasjonstest mot Postgres | JUnit 5 + `Databaseskjemainitialisering` |
| Steg/prosess (behandlingsprosess) | Integrasjonstest | JUnit 5 + repositories |
| REST-endepunkt (`web`) | Integrasjonstest | Jersey test-rammeverk |

## Databasenaere tester

- Ved behov for database: bruk etablerte repo-moenstre og init-flyt i prosjektet.
- Husk at lokale tester krever Postgres/schema (`ung_sak_unit`) satt opp.

## What To Validate

- Happy path + relevante feilstier.
- Grenseverdier for perioder/datoer/satser.
- Sideeffekter som repository-endringer, prosess-stegutfall og publisering av task/hendelser.
- For API-lag: statuskode, payload og valideringsfeil.

## Boundaries

### ✅ Always

- Skriv tester for ny/endret kode foer commit.
- Test baade suksess- og feilstier.
- Kjoer relevante tester lokalt foer push.

### ⚠️ Ask First

- Endring av testrammeverk eller delt teststruktur/testutil.
- Deaktivering eller `@Disabled` av eksisterende tester.

### 🚫 Never

- Committe feilende tester.
- Teste intern implementasjon fremfor observerbar adferd.
- Dele mutbar tilstand mellom tester.
