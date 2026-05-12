---
name: vilkaar-med-varsling
description: "Implementer et nytt vilkår i aktivitetspenger med faktaavklaring av saksbehandler, varsling av bruker via Etterlysning, håndtering av brukerens uttalelse, og automatisk eller manuell vilkårsvurdering. USE FOR: opprette faktaavklaring-grunnlag, Etterlysning-type, OppgaveType i ung-brukerdialog-api, Bekreftelse-subtype i k9-format, aksjonspunkt, steg med etterlysningslogikk, auto-vurdering basert på grunnlag. DO NOT USE FOR: vilkår som ikke trenger faktaavklaring fra saksbehandler (bruk new-grunnlag), inntektskontroll (bruk inntektskontroll-skillen)."
---

# Vilkår med faktaavklaring, varsling og automatisk/manuell vurdering

Dette mønsteret brukes når et vilkår krever:
1. **Faktaavklaring** — saksbehandler registrerer fakta per vilkårsperiode (f.eks. bor bruker i Trondheim?), eller fakta settes automatisk fra søknad
2. **Varsling** (valgfritt) — bruker varsles via `Etterlysning` → `OppgaveType` i ung-brukerdialog-api
3. **Uttalelse** — bruker kan svare med kommentar (eller ikke svare innen frist)
4. **Vilkårsvurdering** — automatisk basert på fakta, eller manuell av saksbehandler med begrunnelse

**Referanseimplementasjon:** BOSTEDSVILKÅR — se `VurderBosattSteg`, `BostedsGrunnlag*`, `VurderBostedOppdaterer`, `ManuellVurderingBostedsvilkårOppdaterer`, `BostedOppgaveOppretter`.

---

## Arkitekturmønster

```
Fakta-steg (VURDER_<VILKÅR>)          Vilkår-vurdering
─────────────────────────────          ──────────────────────────────
Søknadsdata → auto-fakta (SØKNAD)  →   auto-vurder → AP MANUELL_VURDERING
Saksbehandler → fakta (SAKSBEHANDLER)
  ├─ varsle bruker (etterlysning)   →   vent → utløpt/svar uten uttalelse → auto-vurder
  └─ ikke varsle (åpenbar grunn)    →   auto-vurder / AP MANUELL_VURDERING
Bruker svarer med uttalelse        →   AP MANUELL_VURDERING_<VILKÅR>
```

**Sentrale prinsipper:**
- Faktagrunnlaget har én holder med `Kilde`-felt (`SØKNAD` / `SAKSBEHANDLER`)
- Det er **ett** aksjonspunkt for faktaregistrering, **ikke** et separat fastsett-steg
- Fakta fra søknad settes automatisk — de trenger aldri saksbehandlerbekreftelse
- Vilkårsvurdering er alltid manuell dersom: kilde=SØKNAD, bruker har avgitt uttalelse, eller vilkår-spesifikk årsak krever det (f.eks. årsak=ANNET)
- Saksbehandler kan ved faktaregistrering velge å ikke varsle bruker dersom det foreligger åpenbar grunn

---

## Steg 0 — Samle inn detaljer (bruk ask_user)

Ikke anta verdier. Still disse spørsmålene:

1. **Vilkårsnavn** — eks. `BOSTEDSVILKÅR`, `ALDER_VILKÅR`
2. **Faktaspørsmål** — hva skal saksbehandler svare på? (eks. "Er bruker bosatt i Trondheim?")
3. **Felt i grunnlag** — hva lagres per skjæringstidspunkt? (eks. `erBosattITrondheim: Boolean`)
4. **Auto-avslags-logikk** — hvilken feltverdi gir avslag + hvilken `Avslagsårsak`?
5. **Autopunkt-kode** — neste ledige `70xx`-kode (sjekk `AksjonspunktKodeDefinisjon.java`)
6. **Manuelt vilkårsvurdering-aksjonspunkt** — gjenbruk eksisterende (f.eks. `5144 MANUELL_VURDERING_BOSTEDSVILKÅR`) eller ny kode?
7. **Manuell vilkårsbetingelse** — finnes det vilkår-spesifikke årsaker utover kilde=SØKNAD og uttalelse som utløser manuell vurdering?

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
public static final String MANUELL_VURDERING_<VILKÅR>VILKÅR_KODE = "<51xx>";
```

### `AksjonspunktDefinisjon.java`
```java
AUTO_SATT_PÅ_VENT_ETTERLYST_<VILKÅR>UTTALELSE(
    new AksjonspunktData(AUTO_SATT_PÅ_VENT_ETTERLYST_<VILKÅR>_UTTALELSE_KODE,
        AksjonspunktType.AUTOPUNKT, BehandlingStegType.VURDER_<VILKÅR>,
        VurderingspunktType.UT, Venteårsak.VENTER_PÅ_ETTERLYST_<VILKÅR>UTTALELSE,
        Duration.ofWeeks(2))),
MANUELL_VURDERING_<VILKÅR>VILKÅR(
    new AksjonspunktData(MANUELL_VURDERING_<VILKÅR>VILKÅR_KODE,
        AksjonspunktType.MANUELL, BehandlingStegType.VURDER_<VILKÅR>,
        VurderingspunktType.UT)),
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

### Faktagrunnlag — enkelt holder med Kilde

| Klasse | Annotasjoner | Innhold |
|--------|-------------|---------|
| `<Vilkår>Avklaring` | `@Entity @Immutable` | `fomDato: LocalDate`, `<faktafelt>: Boolean`, `kilde: Kilde` — **ingen** `holderId`-felt (styres av `@JoinColumn` i holder) |
| `<Vilkår>AvklaringHolder` | `@Entity` | `@OneToMany(cascade=ALL) @JoinColumn(name="<vilkaar>_avklaring_holder_id") Set<<Vilkår>Avklaring>` + `equals()` på settet |
| `<Vilkår>Grunnlag` | `@Entity` | `behandlingId`, `aktiv=true`, `grunnlagsreferanse=UUID`, `@ManyToOne holder` (NOT NULL) |

> **Enkelt holder:** Grunnlaget har ett holder-felt med en `kilde`-kolonne per avklaring:
> - `kilde = SØKNAD` — fakta satt automatisk fra brukerens søknad; alltid manuell vilkårsvurdering
> - `kilde = SAKSBEHANDLER` — fakta registrert manuelt av saksbehandler
>
> Det er **ikke** lenger separate foreslåttHolder/fastsattHolder. All fakta lagres i samme holder og er umiddelbart gjeldende.

### Separat søknadsaggregat

Søknadsdata skal ligge i et **eget aggregat** som ikke er koblet til vilkårsperiode/skjæringstidspunkt. Dette gjør at søknadsopplysninger kan persisteres uavhengig av hvordan vilkårsperiodene ser ut:

```java
// BosattSøknadGrunnlag / <Vilkår>SøknadGrunnlag
// Inneholder List<BostedsinformasjonFraSøknad> med:
//   journalpostId, fomDato, erBosattITrondheim (eller tilsvarende faktafelt)
```

`lagreAvklaringerFraSøknad(behandlingId, Map<LocalDate, Boolean>)` — bruker kilde=SØKNAD, ingen fraflyttingsDato/årsak.

### `Kilde`-enum (`kodeverk/`)
```java
public enum Kilde {
    SØKNAD,       // Fakta satt automatisk fra brukerens søknad
    SAKSBEHANDLER // Fakta registrert manuelt av saksbehandler
}
```

**`<Vilkår>GrunnlagRepository`:**
- `hentGrunnlagHvisEksisterer(behandlingId)` → `Optional<<Vilkår>Grunnlag>`
- `lagreAvklaringer(behandlingId, Map<LocalDate, AvklaringData>)` — lagrer til holder med kilde fra data; deaktiver gammelt grunnlag kun ved endring
- `lagreAvklaringerFraSøknad(behandlingId, Map<LocalDate, Boolean>)` — kilde=SØKNAD automatisk; ingen årsak/fraflyttingsDato
- `kopierGrunnlagFraEksisterendeBehandling(gammel, ny)` — pek til samme holder (ingen kopi)

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
    kilde varchar(50) not null default 'SAKSBEHANDLER',
    opprettet_av varchar(20) not null default 'VL',
    opprettet_tid timestamp(3) not null default current_timestamp
);
create table gr_<vilkaar>_avklaring (
    id bigint primary key,
    behandling_id bigint not null references behandling(id),
    grunnlagsreferanse uuid not null,
    avklaring_holder_id bigint not null references <vilkaar>_avklaring_holder(id),
    aktiv boolean not null default true,
    versjon bigint not null default 0,
    opprettet_av varchar(20) not null default 'VL',
    opprettet_tid timestamp(3) not null default current_timestamp,
    endret_av varchar(20), endret_tid timestamp(3)
);
```

> **Viktige merknader:**
> - FK-kolonnen i `<vilkaar>_avklaring` heter `<vilkaar>_avklaring_holder_id` — det er dette `@JoinColumn`-navnet i holder matcher. Ikke legg til et separat `holderId`-felt i entiteten — det vil gi `Duplicate column`-feil fra Hibernate.
> - `kilde`-kolonnen settes til `SAKSBEHANDLER` som default; `lagreAvklaringerFraSøknad()` setter `SØKNAD` eksplisitt.
> - `gr_<vilkaar>_avklaring` har kun én holder-kolonne (ingen `fastsatt_avklaring_holder_id`).

---

## Steg 6 — Kontrakt DTO + Oppdaterere

### Fakta-DTO (aksjonspunkt `VURDER_<VILKÅR>`)

**Kontrakt (`kontrakt/`, Java 21):**
```java
// <Vilkår>AvklaringPeriodeDto.java
public record <Vilkår>AvklaringPeriodeDto(
    Periode periode,
    Boolean <faktafelt>,
    String begrunnelse,
    boolean ikkeVarsle   // saksbehandler velger å ikke sende etterlysning
) {}

// Vurder<Vilkår>Dto.java
public class Vurder<Vilkår>Dto extends BekreftetAksjonspunktDto {
    private List<<Vilkår>AvklaringPeriodeDto> avklaringer;
}
```

**`Vurder<Vilkår>Oppdaterer` (`web/`):**
1. Lagre fakta via `lagreAvklaringer()` med `kilde=SAKSBEHANDLER`
2. For perioder der `ikkeVarsle=false` **og** ingen aktiv/besvart etterlysning finnes: opprett `Etterlysning(UTTALELSE_<VILKÅR>)` + planlegg `OpprettEtterlysningTask`
3. For perioder der `ikkeVarsle=true`: hopp over etterlysning
4. Sjekk `hentBesvartEtterlysninger()` — ikke send ny etterlysning for perioder som allerede har `MOTTATT_SVAR`
5. Returner **`rekjørSteg()`** (IKKE bekreft AP — steg håndterer tilstandsmaskin)

### Manuell vilkårsvurdering-DTO (aksjonspunkt `MANUELL_VURDERING_<VILKÅR>VILKÅR`)

```java
// Manuell<Vilkår>VilkårDto.java — bruker VilkårPeriodeVurderingDto
@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_<VILKÅR>VILKÅR_KODE)
public class Manuell<Vilkår>VilkårDto extends BekreftetAksjonspunktDto {
    @NotNull @Size(min=1, max=100)
    private List<@Valid VilkårPeriodeVurderingDto> vurdertePerioder;
}
```

**`Manuell<Vilkår>VilkårOppdaterer` (`web/`):** følg `VurderAndreLivsoppholdsytelserOppdaterer`-mønsteret:
```java
for (VilkårPeriodeVurderingDto vurdertPeriode : dto.getVurdertePerioder()) {
    Utfall utfall = vurdertPeriode.erVilkårOppfylt() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
    vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(fom, tom)
        .medUtfallManuell(utfall)
        .medAvslagsårsak(vurdertPeriode.avslagsårsak())
        .medBegrunnelse(vurdertPeriode.begrunnelse())); // tekst inkluderes i brevet
}
return OppdateringResultat.nyttResultat(); // IKKE rekjørSteg()
```

`VilkårPeriodeVurderingDto`-felter: `periode (fom/tom)`, `erVilkårOppfylt`, `avslagsårsak`, `begrunnelse`.
Begrunnelse-teksten inkluderes i brevet.

---

## Steg 7 — OppgaveOppretter og tjenesteoppdateringer

**Ny klasse** `<Vilkår>OppgaveOppretter` med `@OppgaveTypeRef(UNG_<VILKÅR>_AVKLARING)`:
```java
OppgavetypeDataDto lagOppgaveData(Etterlysning e) {
    var avklaring = repo.hentGrunnlagHvisEksisterer(e.getBehandlingId())
        .orElseThrow().getHolder().finnAvklaring(e.getPeriode().getFomDato());
    return new Bekreft<Vilkår>OppgavetypeDataDto(fom, tom, avklaring.get<Faktafelt>());
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

## Steg 8 — To separate behandlingssteg

Prosessen er delt i **to steg**: et faktasteg og et vilkårsvurderingssteg.
**Referanse:** `VurderFaktaBostedSteg.java` og `VurderBosattVilkårSteg.java`.

---

### Steg 8a — Faktasteg (`VURDER_FAKTA_OM_<VILKÅR>`)

Klasse: `VurderFakta<Vilkår>Steg implements BehandlingSteg` (ikke `VilkårVurderingSteg`).

**Ansvar:** Auto-initiere fakta fra søknad, og returnere AP for manuell faktaregistrering dersom nødvendig.

```java
@Override
public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
    long behandlingId = kontekst.getBehandlingId();
    NavigableSet<DatoIntervallEntitet> perioderTilVurdering =
        VilkårsPerioderTilVurderingTjeneste.finnTjeneste(...)
            .utled(behandlingId, VilkårType.<VILKÅR>);

    // Auto-sett fakta fra søknad for perioder til vurdering
    initierFaktaFraSøknadsdata(behandlingId, perioderTilVurdering);

    // Sjekk om manuell faktavurdering er nødvendig (basert på prosess-trigger)
    LocalDateTimeline<Boolean> tidslinjeForManuellFaktavurdering =
        finnTidslinjeForManuellFaktavurdering(behandling, behandlingId);
    if (!tidslinjeForManuellFaktavurdering.isEmpty()) {
        return BehandleStegResultat.utførtMedAksjonspunkter(
            List.of(AksjonspunktDefinisjon.VURDER_FAKTA_OM_<VILKÅR>));
    }
    return BehandleStegResultat.utførtUtenAksjonspunkter();
}
```

Manuell faktavurdering trigges av prosess-trigger `BehandlingÅrsakType.ENDRET_<VILKÅR>`:
```java
private LocalDateTimeline<Boolean> finnTidslinjeForManuellFaktavurdering(...) {
    return ProsessTriggerPeriodeUtleder.finnTjeneste(...)
        .utledTidslinje(behandlingId)
        .filterValue(it -> it.contains(BehandlingÅrsakType.ENDRET_<VILKÅR>))
        .mapValue(_ -> true);
}
```

**`Vurder<Vilkår>Oppdaterer` (AP `VURDER_FAKTA_OM_<VILKÅR>`):**
1. Lagre fakta med `kilde=SAKSBEHANDLER`
2. Opprette `Etterlysning(UTTALELSE_<VILKÅR>)` per periode — **med mindre** `ikkeVarsle=true` i DTO-en, eller det allerede finnes en aktiv (`OPPRETTET`/`VENTER`) eller besvart (`MOTTATT_SVAR`) etterlysning
3. Returnere `rekjørSteg()` — steg re-kjøres og går videre til neste steg uten AP

---

### Steg 8b — Vilkårsvurderingssteg (`VURDER_<VILKÅR>VILKÅR`)

Klasse: `Vurder<Vilkår>VilkårSteg extends VilkårVurderingSteg`.

**Ansvar:** Lese eksisterende etterlysninger, klassifisere perioder, auto-vurdere, og returnere AP for manuell vurdering.

#### Klassifisering med `StegUtfall`-enum

```java
private enum StegUtfall {
    VILKÅR_VURDERES_AUTOMATISK,
    VILKÅR_VURDERES_MANUELT,
    VENTER_PÅ_UTTALELSE_FRA_BRUKER
}
```

Etterlysninger matches mot perioder på `fom`-dato. Per periode:

| Tilstand | `StegUtfall` |
|----------|-------------|
| Aktiv etterlysning (`OPPRETTET`/`VENTER`) | `VENTER_PÅ_UTTALELSE_FRA_BRUKER` |
| `MOTTATT_SVAR` med `harUttalelse=true` | `VILKÅR_VURDERES_MANUELT` |
| `kilde=SØKNAD` | `VILKÅR_VURDERES_MANUELT` |
| Vilkår-spesifikk årsak (f.eks. `årsak=ANNET`) | `VILKÅR_VURDERES_MANUELT` |
| Alle andre (inkl. utløpt, svar uten uttalelse) | `VILKÅR_VURDERES_AUTOMATISK` |

```java
private LocalDateTimeline<StegUtfall> stegutfallTidslinje =
    tidslinjeTilVurdering.map(segment -> vurder(segment, etterlysningPerFom, holder));
```

#### Tilstandsmaskin i `utførResten()`

```java
// 1. Sett på vent dersom noen perioder venter på svar
if (!stegutfallTidslinje.filterValue(VENTER_PÅ_UTTALELSE_FRA_BRUKER::equals).isEmpty()) {
    return settPåVent(stegutfallTidslinje, etterlysningPerFom);
}
// 2. Auto-vurder alle VILKÅR_VURDERES_AUTOMATISK-perioder
autoVurder(behandlingId, stegutfallTidslinje, holder);
// 3. Manuell vurdering for VILKÅR_VURDERES_MANUELT-perioder
if (!stegutfallTidslinje.filterValue(VILKÅR_VURDERES_MANUELT::equals).isEmpty()) {
    return BehandleStegResultat.utførtMedAksjonspunkter(
        List.of(AksjonspunktDefinisjon.VURDER_<VILKÅR>VILKÅR));
}
return BehandleStegResultat.utførtUtenAksjonspunkter();
```

#### `autoVurder()` — itererer tidslinje, ikke råperioder

```java
private void autoVurder(long behandlingId,
                        LocalDateTimeline<StegUtfall> stegutfallTidslinje,
                        <Vilkår>AvklaringHolder holder) {
    stegutfallTidslinje.filterValue(VILKÅR_VURDERES_AUTOMATISK::equals)
        .toSegments()
        .forEach(s -> {
            var avklaring = holder.getPeriodeAvklaring(s.getFom()).orElseThrow(...);
            // sett OPPFYLT eller IKKE_OPPFYLT basert på faktafelt
            // ved fraflyttingsDato: splitt periode
            // inkluder regelInput (JSON av faktaopplysningene) for sporbarhet
        });
}
```

Legg til `medRegelInput(regelInput)` for sporbarhet — serialiser faktaopplysningene til JSON:
```java
private static String lagRegelInput(<Vilkår>Avklaring avklaring) {
    return VILKAR_JSON_OBJECT_MAPPER.writeValueAsString(new RegelInput(...));
}
private record RegelInput(UUID referanse, LocalDate skjaeringstidspunkt,
                          boolean <faktafelt>, ..., Kilde kilde) {}
```

---

## Steg 9 — Kopiering ved revurdering

**`GrunnlagKopiererAktivitetspenger.java`:**
```java
@Inject <Vilkår>GrunnlagRepository <vilkaar>GrunnlagRepository;
@Inject <Vilkår>SøknadGrunnlagRepository <vilkaar>SøknadGrunnlagRepository;

// I begge kopier()-metoder:
<vilkaar>GrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(gammel, ny);
<vilkaar>SøknadGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(gammel, ny);
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
| Beslutte AP for manuell vurdering feil | Bruk `VilkårPeriodeVurderingDto` med `erVilkårOppfylt=false` og `avslagsårsak` for avslag |
| Beslutter skal ikke godkjenne auto-vurderte vilkår | Fjern AP fra `LokalkontorBeslutterVilkårAksjonspunktDto` i beslutter-steget |
| Oppdaterer for fakta returnerer `rekjørSteg()` | Steg re-kjøres og finner ny etterlysning (OPPRETTET) → setter autopunkt |
| Oppdaterer for manuell vilkårsvurdering returnerer `nyttResultat()` | Ikke `rekjørSteg()` — vilkåret er allerede satt via `param.getVilkårResultatBuilder()` |
| `UnknownEntityException: Could not resolve root entity` i tester | Mangler `pu-default.<vilkaar>.orm.xml` — se Steg 4 |
| `Duplicate column '<vilkaar>_avklaring_holder_id'` fra Hibernate | `<Vilkår>Avklaring` har et overflødig `holderId`-felt som kolliderer med `@JoinColumn` i holder — fjern feltet |
| `method does not override` eller `cannot find symbol` i tester | Kjør tester med `-am` slik at avhengige moduler kompileres riktig: `mvn test -pl <modul> -am -Dsurefire.failIfNoSpecifiedTests=false` |
| `getAksjonspunktDefinisjon()` finnes ikke | `getAksjonspunktListe()` returnerer `List<AksjonspunktDefinisjon>` direkte — sammenlign element, ikke kall metode på det |
| Lambda-kompileringsfeil: "must be final or effectively final" | Variabel re-assignes etter søknads-lagring — bruk `final`-kopi før lambda: `final Map<...> avklaringLookup = periodeAvklaringPerFom;` |
| Duplikat etterlysning sendt for MOTTATT_SVAR | Sjekk `hentBesvartEtterlysninger()` i tillegg til `hentEtterlysningerSomVenterPåSvar()` — ikke send ny etterlysning for perioder som allerede har svar |
| Søknadsdata må persisteres uavhengig av vilkårsperioder | Bruk eget søknadsaggregat (f.eks. `BosattSøknadGrunnlag`) — ikke koble søknadsdata direkte til holder |
