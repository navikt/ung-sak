# Legg til en ny oppgavetype

Denne guiden beskriver alle steg som må gjennomføres for å legge til en ny oppgavetype i `brukerdialog-oppgave`.

Som eksempel brukes den eksisterende typen `BEKREFT_ENDRET_STARTDATO` i pakken
`no.nav.ung.sak.oppgave.typer.varsel.typer.endretstartdato`.

---

## Oversikt over steg

1. [Legg til enum-verdi i `OppgaveType`](#1-legg-til-enum-verdi-i-oppgavetype)
2. [Opprett kontrakt-DTO i `kontrakt`-modulen](#2-opprett-kontrakt-dto-i-kontrakt-modulen)
3. [Opprett JPA-entitet](#3-opprett-jpa-entitet)
4. [Opprett databasemigrering](#4-opprett-databasemigrering)
5. [Implementer `OppgaveDataMapperFraDtoTilEntitet`](#5-implementer-oppgavedatamapperfradtotilentitet)
6. [Implementer `OppgaveDataMapperFraEntitetTilDto`](#6-implementer-oppgavedatamapperfraentitettildto)
7. [Implementer `OppgavelInnholdUtleder`](#7-implementer-oppgavelinnholdutleder)

---

## 1. Legg til enum-verdi i `OppgaveType`

Filen ligger i `kontrakt`-modulen.

```java
// no.nav.ung.sak.kontrakt.oppgaver.OppgaveType
public enum OppgaveType implements Kodeverdi {
    // ...eksisterende verdier...
    MIN_NYE_OPPGAVETYPE("MIN_NYE_OPPGAVETYPE");
}
```

---

## 2. Opprett kontrakt-DTO i `kontrakt`-modulen

Opprett en ny record som implementerer `OppgavetypeDataDto` og registrer den som `@JsonSubTypes.Type`
i `OppgavetypeDataDto`-interfacet.

**Pakke:** `no.nav.ung.sak.kontrakt.oppgaver.typer.<ny-type>/`

```java
// MinNyeOppgavetypeDataDto.java
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(...)
public record MinNyeOppgavetypeDataDto(
    @JsonProperty("mittFelt") LocalDate mittFelt
) implements OppgavetypeDataDto {

    @Override
    public OppgaveType oppgavetype() {
        return OppgaveType.MIN_NYE_OPPGAVETYPE;
    }
}
```

Legg til subtype i `OppgavetypeDataDto`:

```java
@JsonSubTypes({
    // ...eksisterende...
    @JsonSubTypes.Type(value = MinNyeOppgavetypeDataDto.class, name = "MIN_NYE_OPPGAVETYPE"),
})
public interface OppgavetypeDataDto { ... }
```

---

## 3. Opprett JPA-entitet

Opprett en klasse som utvider `OppgaveDataEntitet`. Alle felter lagres i en dedikert tabell.

**Pakke:** `no.nav.ung.sak.oppgave.typer.<varsel|oppgave>.typer.<ny-type>/`

> - Legg typen under `varsel/typer/` dersom oppgaven er et vedtaksvarsel som forventer et svar av typen `SvarPåVarselDTO` fra bruker.
> - Legg typen under `oppgave/` dersom det er en annen type oppgave (f.eks. ren rapportering eller søknad).

Annoteringen `@OppgaveTypeRef` er valgfri på entiteten, men anbefales for å tydliggjøre hvilken
oppgavetype klassen tilhører.

```java
@Entity(name = "MinNyeOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_MIN_NYE_TYPE")
@Access(AccessType.FIELD)
@OppgaveTypeRef(OppgaveType.MIN_NYE_OPPGAVETYPE)
public class MinNyeOppgaveDataEntitet extends OppgaveDataEntitet {

    @Column(name = "mitt_felt", nullable = false, updatable = false)
    private LocalDate mittFelt;

    protected MinNyeOppgaveDataEntitet() { }

    public MinNyeOppgaveDataEntitet(LocalDate mittFelt) {
        this.mittFelt = mittFelt;
    }

    public LocalDate getMittFelt() { return mittFelt; }
}
```

---

## 4. Opprett databasemigrering

Opprett en ny Flyway-migreringsfil i
`migreringer/src/main/resources/db/postgres/defaultDS/1.0/`.

Filen skal ha neste ledige versjonsnummer (f.eks. `V1.0_072__...sql`).

Husk å inkludere:
- `bd_oppgave_id` som FK til `BD_OPPGAVE(id)` — slik at entiteten knyttes til oppgaven
- `id` generert fra den felles sekvensen `SEQ_BD_OPPGAVE_DATA`
- Audit-kolonner (`opprettet_av`, `opprettet_tid`, `endret_av`, `endret_tid`)

```sql
create table BD_OPPGAVE_DATA_MIN_NYE_TYPE
(
    id              bigint      not null primary key,
    bd_oppgave_id   bigint      not null references BD_OPPGAVE (id),
    mitt_felt       date        not null,
    opprettet_av    varchar(20) not null default 'VL',
    opprettet_tid   timestamp   not null default current_timestamp,
    endret_av       varchar(20),
    endret_tid      timestamp
);

comment on table  BD_OPPGAVE_DATA_MIN_NYE_TYPE               is 'Oppgavedata for type MIN_NYE_OPPGAVETYPE.';
comment on column BD_OPPGAVE_DATA_MIN_NYE_TYPE.id            is 'Primary key.';
comment on column BD_OPPGAVE_DATA_MIN_NYE_TYPE.bd_oppgave_id is 'FK til BD_OPPGAVE.id.';
comment on column BD_OPPGAVE_DATA_MIN_NYE_TYPE.mitt_felt     is 'Beskrivelse av feltet.';
```

---

## 5. Implementer `OppgaveDataMapperFraDtoTilEntitet`

Mapper en innkommende DTO til JPA-entiteten. Brukes når en oppgave opprettes.

```java
@ApplicationScoped
@OppgaveTypeRef(OppgaveType.MIN_NYE_OPPGAVETYPE)
public class MinNyeOppgaveDataMapperFraDtoTilEntitet implements OppgaveDataMapperFraDtoTilEntitet {

    protected MinNyeOppgaveDataMapperFraDtoTilEntitet() { }

    @Override
    public OppgaveDataEntitet map(OppgavetypeDataDto data) {
        var dto = (MinNyeOppgavetypeDataDto) data;
        return new MinNyeOppgaveDataEntitet(dto.mittFelt());
    }
}
```

---

## 6. Implementer `OppgaveDataMapperFraEntitetTilDto`

Mapper en JPA-entitet tilbake til DTO. Brukes når en oppgave hentes og skal returneres til klient.

```java
@ApplicationScoped
@OppgaveTypeRef(OppgaveType.MIN_NYE_OPPGAVETYPE)
public class MinNyeOppgaveDataMapperFraEntitetTilDto implements OppgaveDataMapperFraEntitetTilDto {

    protected MinNyeOppgaveDataMapperFraEntitetTilDto() { }

    @Override
    public OppgavetypeDataDto tilDto(OppgaveDataEntitet entitet) {
        var e = (MinNyeOppgaveDataEntitet) entitet;
        return new MinNyeOppgavetypeDataDto(e.getMittFelt());
    }
}
```

---

## 7. Implementer `OppgavelInnholdUtleder`

Bestemmer hvilken tekst og lenke som publiseres som Min Side-varsel når oppgaven opprettes.

> Dersom oppgavetypen **ikke** skal utløse et Min Side-varsel kan dette steget hoppes over,
> men da vil opprettelse feile dersom `OppgaveLivssyklusTjeneste.opprettOppgave` kalles.
> Vurder i så fall å håndtere dette særskilt.

```java
@ApplicationScoped
@OppgaveTypeRef(OppgaveType.MIN_NYE_OPPGAVETYPE)
public class MinNyeOppgavelInnholdUtleder implements OppgavelInnholdUtleder {

    private final String baseUrl;

    @Inject
    public MinNyeOppgavelInnholdUtleder(
        @KonfigVerdi("UNGDOMPROGRAMSYTELSEN_DELTAKER_BASE_URL") String baseUrl
    ) {
        this.baseUrl = baseUrl;
    }

    protected MinNyeOppgavelInnholdUtleder() { }

    @Override
    public String utledVarselTekst(BrukerdialogOppgaveEntitet oppgave) {
        return "Tekst som vises på Min Side for denne oppgavetypen";
    }

    @Override
    public String utledVarselLenke(BrukerdialogOppgaveEntitet oppgave) {
        return baseUrl + "/oppgave/" + oppgave.getOppgavereferanse();
    }
}
```

---

## Sammendrag — sjekkliste

| Steg | Hva | Modul |
|------|-----|-------|
| 1 | Legg til verdi i `OppgaveType` | `kontrakt` |
| 2 | Opprett `*DataDto` record + registrer i `OppgavetypeDataDto` | `kontrakt` |
| 3 | Opprett `*DataEntitet` som utvider `OppgaveDataEntitet` (gjerne med `@OppgaveTypeRef`) | `brukerdialog-oppgave/tjeneste` |
| 4 | Opprett Flyway-migrering med ny tabell og `bd_oppgave_id` FK | `migreringer` |
| 5 | Implementer `OppgaveDataMapperFraDtoTilEntitet` + `@OppgaveTypeRef` | `brukerdialog-oppgave/tjeneste` |
| 6 | Implementer `OppgaveDataMapperFraEntitetTilDto` + `@OppgaveTypeRef` | `brukerdialog-oppgave/tjeneste` |
| 7 | Implementer `OppgavelInnholdUtleder` + `@OppgaveTypeRef` | `brukerdialog-oppgave/tjeneste` |
