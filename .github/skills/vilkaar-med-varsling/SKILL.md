---
name: vilkaar-med-varsling
description: "Implementer et nytt vilkår i aktivitetspenger med faktaavklaring av saksbehandler, varsling av bruker via Etterlysning, håndtering av brukerens uttalelse, og automatisk vilkårsvurdering. USE FOR: opprette faktaavklaring-grunnlag, Etterlysning-type, OppgaveType i ung-brukerdialog-api, Bekreftelse-subtype i k9-format, aksjonspunkt, steg med etterlysningslogikk, auto-vurdering basert på grunnlag. DO NOT USE FOR: vilkår som ikke trenger faktaavklaring fra saksbehandler (bruk new-grunnlag), inntektskontroll (bruk inntektskontroll-skillen)."
---

# Vilkår med faktaavklaring, varsling og automatisk vurdering

Dette mønsteret brukes når et vilkår krever:
1. **Faktaavklaring** — saksbehandler registrerer fakta per vilkårsperiode (f.eks. bor bruker i Trondheim?)
2. **Varsling** — bruker varsles via `Etterlysning` → `OppgaveType` i ung-brukerdialog-api
3. **Uttalelse** — bruker kan svare med kommentar (eller ikke svare innen frist)
4. **Automatisk vilkårsvurdering** — vilkåret vurderes automatisk basert på grunnlag og svar

**Referanseimplementasjon:** BOSTEDSVILKÅR — se `VurderBosattSteg`, `BostedsGrunnlag*`, `VurderBostedOppdaterer`, `BostedOppgaveOppretter`.

---

## Steg 0 — Samle inn detaljer (bruk ask_user)

Ikke anta verdier. Still disse spørsmålene:

1. **Vilkårsnavn** — eks. `BOSTEDSVILKÅR`, `ALDER_VILKÅR`
2. **Faktaspørsmål** — hva skal saksbehandler svare på? (eks. "Er bruker bosatt i Trondheim?")
3. **Felt i grunnlag** — hva lagres per skjæringstidspunkt? (eks. `erBosattITrondheim: Boolean`)
4. **Auto-avslags-logikk** — hvilken feltverdi gir avslag + hvilken `Avslagsårsak`?
5. **Autopunkt-kode** — neste ledige `70xx`-kode (sjekk `AksjonspunktKodeDefinisjon.java`)
6. **Manuelt aksjonspunkt** — gjenbruk eksisterende (f.eks. `5140 VURDER_BOSTED`) eller ny kode?

---

## Steg 1 — Kodeverk (`kodeverk/`)

### `EtterlysningType.java`
```java
UTTALELSE_<VILKÅR>("UTTALELSE_<VILKÅR>"),
```
Oppdater **begge** switch-metoder:
- `tilAutopunktDefinisjon()` → `AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_<VILKÅR>UTTALELSE`
- `mapTilVenteårsak()` → `Venteårsak.VENTER_PÅ_ETTERLYST_<VILKÅR>UTTALELSE`

### `EndringType.java`
```java
AVKLAR_<VILKÅR>,
```

### `Venteårsak.java`
```java
VENTER_PÅ_ETTERLYST_<VILKÅR>UTTALELSE("<VILKÅR>UTTALELSE", "Venter på uttalelse fra bruker om <vilkår>"),
```

### `AksjonspunktKodeDefinisjon.java`
```java
public static final String AUTO_SATT_PÅ_VENT_ETTERLYST_<VILKÅR>_UTTALELSE_KODE = "<70xx>";
```

### `AksjonspunktDefinisjon.java`
```java
AUTO_SATT_PÅ_VENT_ETTERLYST_<VILKÅR>UTTALELSE(
    new AksjonspunktData(AUTO_SATT_PÅ_VENT_ETTERLYST_<VILKÅR>_UTTALELSE_KODE,
        AksjonspunktType.AUTOPUNKT, BehandlingStegType.VURDER_<VILKÅR>,
        VurderingspunktType.UT, Venteårsak.VENTER_PÅ_ETTERLYST_<VILKÅR>UTTALELSE,
        Duration.ofWeeks(2))),
```

---

## Steg 2 — k9-format (eksternt repo)

**Fil:** `Bekreftelse.java` — legg til i `Type` enum + `@JsonSubTypes`:
```java
UNG_<VILKÅR>_AVKLARING("<kode>"),
@JsonSubTypes.Type(value = <Vilkår>AvklaringBekreftelse.class, name = "<kode>")
```

**Ny fil:** `<Vilkår>AvklaringBekreftelse.java`:
```java
public class <Vilkår>AvklaringBekreftelse extends Bekreftelse {
    private UUID oppgaveReferanse;
    private boolean harUttalelse;
    private String uttalelseFraBruker;
    // @JsonCreator constructor + getters
}
```

Installer: `cd k9-format && mvn install -DskipTests`  
Oppdater versjon i `ung-sak/pom.xml`: `<k9format.version>X.X.X-SNAPSHOT</k9format.version>`

---

## Steg 3 — ung-brukerdialog-api (eksternt repo)

**`OppgaveType.java`:** legg til `BEKREFT_<VILKÅR>`

**Ny fil:** `Bekreft<Vilkår>OppgavetypeDataDto.java`:
```java
public record Bekreft<Vilkår>OppgavetypeDataDto(
    LocalDate fraOgMed, LocalDate tilOgMed, Boolean <faktafelt>
) implements OppgavetypeDataDto {}
```

Installer: `cd ung-brukerdialog-api && mvn install -DskipTests`  
Oppdater i `ung-sak/pom.xml`: `<ung-brukerdialog-api.version>X.X.X-SNAPSHOT</ung-brukerdialog-api.version>`

---

## Steg 4 — Domeneentiteter (`behandlingslager/domene`)

Pakke: `no.nav.ung.sak.behandlingslager.<vilkaar>/`  
**Viktig:** bruk `no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet`, IKKE `no.nav.ung.sak.typer`.

| Klasse | Annotasjoner | Innhold |
|--------|-------------|---------|
| `<Vilkår>Avklaring` | `@Entity @Immutable` | `skjæringstidspunkt: LocalDate`, `<faktafelt>: Boolean` — **ingen** `holderId`-felt (styres av `@JoinColumn` i holder) |
| `<Vilkår>AvklaringHolder` | `@Entity` | `@OneToMany(cascade=ALL) @JoinColumn(name="<vilkaar>_avklaring_holder_id") Set<<Vilkår>Avklaring>` + `equals()` på settet |
| `<Vilkår>Grunnlag` | `@Entity` | `behandlingId`, `aktiv=true`, `grunnlagsreferanse=UUID`, `@ManyToOne foreslåttHolder` (NOT NULL), `@ManyToOne fastsattHolder` (nullable) |

> **Foreslått vs fastsatt:** Grunnlaget har to holders:
> - `foreslåttHolder` — saksbehandlers registrering; lagres ved `lagreAvklaringer`
> - `fastsattHolder` — bekreftet vurdering; kopieres fra foreslåttHolder ved UTLØPT/svar uten uttalelse; brukes til automatisk vilkårsvurdering
>
> Dette skillet gjør at saksbehandler **ikke** overskriver fastsatt vurdering ved re-vurdering etter uttalelse.

**`<Vilkår>GrunnlagRepository`:**
- `hentGrunnlagHvisEksisterer(behandlingId)` → `Optional<<Vilkår>Grunnlag>`
- `lagreAvklaringer(behandlingId, avklaringer)` — lagrer til `foreslåttHolder`; `fastsattHolder=null`; deaktiver gammelt grunnlag kun ved endring
- `fastsettForeslåtteAvklaringer(behandlingId, skjæringstidspunkter)` — kopierer angitte perioder fra `foreslåttHolder` → ny `fastsattHolder`; beholder `grunnlagsreferanse`
- `kopierGrunnlagFraEksisterendeBehandling(gammel, ny)` — pek til samme holders (ingen kopi)

**ORM-registrering** — opprett `META-INF/pu-default.<vilkaar>.orm.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence/orm https://jakarta.ee/xml/ns/persistence/orm/orm_3_2.xsd"
                 version="3.2">
    <sequence-generator name="SEQ_<VILKAAR>_AVKLARING_HOLDER" allocation-size="50" sequence-name="SEQ_<VILKAAR>_AVKLARING_HOLDER"/>
    <sequence-generator name="SEQ_<VILKAAR>_AVKLARING" allocation-size="50" sequence-name="SEQ_<VILKAAR>_AVKLARING"/>
    <sequence-generator name="SEQ_GR_<VILKAAR>_AVKLARING" allocation-size="50" sequence-name="SEQ_GR_<VILKAAR>_AVKLARING"/>
    <entity class="no.nav.ung.sak.behandlingslager.<vilkaar>.<Vilkår>AvklaringHolder"/>
    <entity class="no.nav.ung.sak.behandlingslager.<vilkaar>.<Vilkår>Avklaring"/>
    <entity class="no.nav.ung.sak.behandlingslager.<vilkaar>.<Vilkår>Grunnlag"/>
</entity-mappings>
```
Se `pu-default.bosatt.orm.xml` og `pu-default.etterlysning.orm.xml` som referanser.

---

## Steg 5 — Flyway-migrering

Neste versjonsnummer: sjekk siste fil i `migreringer/src/main/resources/db/postgres/defaultDS/1.0/`.

```sql
create sequence seq_<vilkaar>_avklaring_holder increment by 50 start with 1000000;
create sequence seq_<vilkaar>_avklaring increment by 50 start with 1000000;
create sequence seq_gr_<vilkaar>_avklaring increment by 50 start with 1000000;

create table <vilkaar>_avklaring_holder (
    id bigint primary key,
    versjon bigint not null default 0,
    opprettet_av varchar(20) not null default 'VL',
    opprettet_tid timestamp(3) not null default current_timestamp,
    endret_av varchar(20), endret_tid timestamp(3)
);
create table <vilkaar>_avklaring (
    id bigint primary key,
    <vilkaar>_avklaring_holder_id bigint not null references <vilkaar>_avklaring_holder(id),
    skaeringstidspunkt date not null,
    <faktafelt> boolean not null,
    opprettet_av varchar(20) not null default 'VL',
    opprettet_tid timestamp(3) not null default current_timestamp
);
create table gr_<vilkaar>_avklaring (
    id bigint primary key,
    behandling_id bigint not null references behandling(id),
    grunnlagsreferanse uuid not null,
    foreslatt_avklaring_holder_id bigint not null references <vilkaar>_avklaring_holder(id),
    fastsatt_avklaring_holder_id bigint null references <vilkaar>_avklaring_holder(id),
    aktiv boolean not null default true,
    versjon bigint not null default 0,
    opprettet_av varchar(20) not null default 'VL',
    opprettet_tid timestamp(3) not null default current_timestamp,
    endret_av varchar(20), endret_tid timestamp(3)
);
```

> **Viktig:** FK-kolonnen i `<vilkaar>_avklaring` heter `<vilkaar>_avklaring_holder_id` (ikke bare `holder_id`). Det er dette `@JoinColumn`-navnet matcher. Ikke legg til et separat `holderId`-felt i entiteten — det vil gi `Duplicate column`-feil fra Hibernate.

---

## Steg 6 — Kontrakt DTO + Oppdaterer

**Kontrakt (`kontrakt/`, Java 21):**
```java
// <Vilkår>AvklaringPeriodeDto.java
public record <Vilkår>AvklaringPeriodeDto(Periode periode, Boolean <faktafelt>, String begrunnelse) {}

// Vurder<Vilkår>Dto.java
public class Vurder<Vilkår>Dto extends BekreftetAksjonspunktDto {
    private List<<Vilkår>AvklaringPeriodeDto> avklaringer;
}
```

**`Vurder<Vilkår>Oppdaterer` (`web/`):**
1. Lagre grunnlag via repository — detekter om grunnlag faktisk endret
2. Hent relevante perioder fra `AktivitetspengerVilkårsPerioderTilVurderingTjeneste`
3. Opprett `Etterlysning(UTTALELSE_<VILKÅR>)` per periode + planlegg `OpprettEtterlysningTask`
4. Returner **`rekjørSteg()`** (IKKE bekreft AP — steg håndterer tilstandsmaskin)

---

## Steg 7 — OppgaveOppretter og tjenesteoppdateringer

**Ny klasse** `<Vilkår>OppgaveOppretter` med `@OppgaveTypeRef(UNG_<VILKÅR>_AVKLARING)`:
```java
OppgavetypeDataDto lagOppgaveData(Etterlysning e) {
    var grunnlag = repo.hentGrunnlagHvisEksisterer(e.getBehandlingId())
        .orElseThrow().getHolder().finnAvklaring(e.getPeriode().getFomDato());
    return new Bekreft<Vilkår>OppgavetypeDataDto(fom, tom, grunnlag.get<Faktafelt>());
}
```

**Filer som alltid må oppdateres** ved ny `EtterlysningType`/`EndringType`:

| Fil | Hva legges til |
|-----|---------------|
| `OpprettOppgaveTjeneste` | `case UTTALELSE_<VILKÅR>` |
| `EtterlysningOgUttalelseTjeneste` | `case UTTALELSE_<VILKÅR>` |
| `GenerellOppgaveBekreftelseHåndterer` | `@OppgaveTypeRef` + `case UNG_<VILKÅR>_AVKLARING` i `mapTilEndringsType()` |
| `HistorikkinnslagTjeneste` | `case AVKLAR_<VILKÅR>` |

---

## Steg 8 — Steg (`VurderXxxSteg`)

**Per-periode logikk** (referanse: `VurderBosattSteg.java`):

Etterlysninger matches mot vilkårsperioder på `fom`-dato:
```java
// Hent gjeldende etterlysninger (filtrerer ut AVBRUTT/SKAL_AVBRYTES)
var etterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(
    behandlingId, fagsakId, EtterlysningType.UTTALELSE_<VILKÅR>);

// Map fom → etterlysning
Map<LocalDate, EtterlysningData> etterlysningPerFom = etterlysninger.stream()
    .collect(toMap(e -> e.periode().getFomDato(), Function.identity()));
```

Klassifiser hver periode:

| Tilstand | Handling |
|----------|---------|
| Ingen etterlysning | `trengerSaksbehandler` — returner `AP VURDER_<VILKÅR>` |
| `VENTER` / `OPPRETTET` | `ventende` — sett behandling på vent med autopunkt |
| `UTLØPT` eller `MOTTATT_SVAR` med `harUttalelse=false` | `skalFastsettes` — kall `fastsettForeslåtteAvklaringer` + `autoVurder` |
| `MOTTATT_SVAR` med `harUttalelse=true` | `trengerSaksbehandler` — returner `AP VURDER_<VILKÅR>` (saksbehandler re-vurderer) |

```
// Tilstandsmaskin
if (!ventende.isEmpty()) → returner autopunkt (SETT PÅ VENT)
if (!skalFastsettes.isEmpty()) → fastsettForeslåtteAvklaringer + autoVurder
if (!trengerSaksbehandler.isEmpty()) → returner AP
else → utførtUtenAksjonspunkter
```

**`autoVurder()`:** bruk `fastsattHolder` (ikke `foreslåttHolder`) for å sette vilkårsutfall:
- `<faktafelt>=true` → `OPPFYLT`
- `<faktafelt>=false` → `IKKE_OPPFYLT` + `Avslagsårsak.<ÅRSAK>`

**`VurderXxxOppdaterer` (aksjonspunkt-håndterer):** skiller mellom initial og re-vurdering:
- Perioder med mottatt uttalelse (`harUttalelse=true`): kall `fastsettForeslåtteAvklaringer` direkte — ingen ny etterlysning
- Øvrige perioder: opprett `Etterlysning(UTTALELSE_<VILKÅR>)` + `OpprettEtterlysningTask`
- Returner alltid `rekjørSteg()` (IKKE bekreft AP)

---

## Steg 9 — Kopiering ved revurdering

**`GrunnlagKopiererAktivitetspenger.java`:**
```java
@Inject <Vilkår>GrunnlagRepository <vilkaar>GrunnlagRepository;

// I begge kopier()-metoder:
<vilkaar>GrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(gammel, ny);
```

---

## Steg 10 — k9-verdikjede integrasjonstester

**`LokalkontorSteg.java`:**
- Oppdater `saksbehandlerVurdererOgForeslårVilkår` med nye params: `UngSakFordelingSteg`, `UngdomsprogramDeltaker`, `String søkerIdent`
- Legg til ny metode `sendInn<Vilkår>Bekreftelse(steg, deltaker, søkerIdent)` som poster `Vurder<Vilkår>Dto`
- Fjern `VURDER_<VILKÅR>` fra `LokalkontorBeslutterVilkårAksjonspunktDto` i beslutter-steget (vilkåret er nå auto-vurdert)

**`AktivitetspengerTest.java`** og **`ForutgåendeMedlemskapTest.java`**:
- Legg til `UngdomsprogramDeltaker deltaker` felt
- Pass `søkerIdent + deltaker + fordelingSteg` til `saksbehandlerVurdererOgForeslårVilkår`

---

## Viktige gotchas

| Problem | Løsning |
|---------|---------|
| `DatoIntervallEntitet` ikke funnet | Bruk `no.nav.ung.sak.domene.typer.tid`, IKKE `no.nav.ung.sak.typer` |
| k9-format/brukerdialog-api ikke oppdatert | Sjekk `<k9format.version>` og `<ung-brukerdialog-api.version>` i root `pom.xml` |
| Switch-exhaustiveness-feil | `EtterlysningType`-switch finnes i minst 4 filer — søk etter eksisterende `UTTALELSE_*` |
| Avslag-test bruker ikke `VilkårPeriodeVurderingDto` lenger | Bruk `<Vilkår>AvklaringPeriodeDto` med `<faktafelt>=false` |
| Beslutter skal ikke godkjenne auto-vurderte vilkår | Fjern AP fra `LokalkontorBeslutterVilkårAksjonspunktDto` i beslutter-steget |
| Oppdaterer returnerer `rekjørSteg()`, ikke `bekreftAksjonspunkt()` | Steg re-kjøres og finner ny etterlysning (OPPRETTET) → setter autopunkt |
| `UnknownEntityException: Could not resolve root entity` i tester | Mangler `pu-default.<vilkaar>.orm.xml` — se Steg 4 |
| `Duplicate column '<vilkaar>_avklaring_holder_id'` fra Hibernate | `<Vilkår>Avklaring` har et overflødig `holderId`-felt som kolliderer med `@JoinColumn` i holder — fjern feltet |
| `method does not override` eller `cannot find symbol` i tester | Kjør tester med `-am` slik at avhengige moduler kompileres riktig: `mvn test -pl <modul> -am -Dsurefire.failIfNoSpecifiedTests=false` |
| `getAksjonspunktDefinisjon()` finnes ikke | `getAksjonspunktListe()` returnerer `List<AksjonspunktDefinisjon>` direkte — sammenlign element, ikke kall metode på det |
