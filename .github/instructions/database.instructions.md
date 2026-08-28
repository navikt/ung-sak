---
name: Database Migration Standards
description: "Standarder for databasemigrasjoner med Flyway i ung-sak: navnekonvensjoner, sikre endringer og idempotente skript."
applyTo: "migreringer/src/main/resources/db/**/*.sql"
excludeAgent: "code-review"
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
- Unngaa destruktive endringer (drop column/table) i samme migrering som introduserer ny struktur.

## SQL-stil (foelg eksisterende konvensjon)

- Skriv SQL med lowercase noekkelord og typer i nye migreringer (eksisterende migreringer trenger ikke omskrives kun for casing).
- Primaernoekler: `id bigint not null primary key`, generert fra egen `sequence` (`increment by 50 minvalue 1000000`) — ikke `bigserial`/`uuid` med mindre et etablert unntak finnes i modulen.
- Sporingskolonner paa entitetstabeller: `opprettet_av varchar(20) default 'VL' not null`, `opprettet_tid timestamp(3) default current_timestamp not null`, `endret_av varchar(20)`, `endret_tid timestamp(3)`.
- Bruk `daterange`/`tsrange` for perioder, i traad med `Range<LocalDate>` + `@Type(PostgreSQLRangeType.class)`-mønsteret i JPA-entitetene.
- Dokumenter ikke-opplagte tabeller/kolonner med `comment on table ...` / `comment on column ...` fremfor kommentarer utenfor SQL-en.
- Legg til indekser for nye foreign keys og tunge lese-sporringer, f.eks. `create index idx_<tabell>_<kolonne> on <tabell> (<kolonne>)`.
- Vaer eksplisitt med `not null`, defaults og constraints.

```sql
-- eksempel i stil med eksisterende migreringer
create sequence seq_ny_ting increment by 50 minvalue 1000000;

create table ny_ting
(
    id            bigint                                 not null primary key,
    behandling_id bigint references behandling (id)       not null,
    periode       daterange                               not null,
    opprettet_av  varchar(20)  default 'VL'                not null,
    opprettet_tid timestamp(3) default current_timestamp   not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table ny_ting is 'Kort forklaring av hva tabellen representerer.';

create index idx_ny_ting_behandling on ny_ting (behandling_id);
```

## Verification

- Verifiser lokalt med prosjektets flyt (`mvn test-compile` eller `Databaseskjemainitialisering`).
- Bekreft at migreringer fungerer paa tomt skjema og ved oppgradering.
- Hvis en test feiler med VM crash / checksum mismatch etter endring i en migrering som allerede har kjoert lokalt: nullstill skjemaet med `Databaseskjemainitialisering`.

## Boundaries

### ✅ Always

- Følg `V{versjon}__{beskrivelse}.sql`-navngiving med neste ledige versjonsnummer.
- Legg til indekser for nye foreign keys.
- Test migreringen lokalt (tomt skjema + oppgradering) før commit.

### ⚠️ Ask First

- Skjemaendringer som paavirker flere tabeller/moduler.
- Fjerning av kolonner eller tabeller.
- Endring av primaernoekler.
- Store data-migreringer.

### 🚫 Never

- Endre eksisterende, innsjekkede migreringsfiler.
- Hoppe over versjonsnummer.
- Deploye utestede migreringer.
