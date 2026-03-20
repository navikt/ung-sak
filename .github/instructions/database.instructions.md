---
applyTo: "migreringer/src/main/resources/db/migration/**/*.sql"
---

# Flyway Migrations (ung-sak)

## Naming
- Bruk `V{versjon}__{beskrivelse}.sql`.
- Beskrivelse skal vaere lowercase med underscore.
- Ikke hopp over versjoner, bruk neste ledige nummer.

## Endringsprinsipper
- Endre aldri eksisterende migreringer som er sjekket inn.
- Opprett ny migrering for alle endringer.
- Bruk expand/contract: legg til og migrer data foerst, fjern senere.
- Endringer skal vaere framoverkompatible under utrulling.

## SQL-praksis
- Legg til indekser for nye foreign keys og tunge lese-sporringer.
- Vaer eksplisitt med `NOT NULL`, defaults og constraints.
- Unngaa destruktive endringer i samme migrering som introduserer ny struktur.

## Verification
- Verifiser lokalt med prosjektets flyt (`mvn test-compile` eller `Databaseskjemainitialisering`).
- Bekreft at migreringer fungerer paa tomt skjema og ved oppgradering.

## Boundaries
### Always
- Hold migreringer smaaa, lesbare og reversible i praksis.
- Foelg eksisterende stil i `migreringer/src/main/resources/db/migration`.

### Ask First
- Dropp av tabeller/kolonner.
- Endringer som krever downtime eller backfill-jobber.
- Omnavning som paavirker mange moduler.

### Never
- Skriv om historiske migreringer.
- Bland store datamigreringer og risikable schemaendringer i ett steg.
