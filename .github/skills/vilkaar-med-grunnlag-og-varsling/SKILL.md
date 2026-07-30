---
name: vilkaar-med-grunnlag-og-varsling
description: "Komplett implementasjon av et nytt vilkår i aktivitetspenger — dekker alt fra enkelt vilkår med aksjonspunkt til fullt vilkår med faktaavklaring-grunnlag, bruker-varsling via Etterlysning, håndtering av brukerens uttalelse og automatisk/manuell vilkårsvurdering. USE FOR: alle nye vilkår i aktivitetspenger, uavhengig av kompleksitetsnivå — enkel variant (aksjonspunkt + vilkår), eller varslings-variant (grunnlag + Etterlysning + to behandlingssteg). DO NOT USE FOR: aksjonspunkt uten vilkår, inntektskontroll (bruk inntektskontroll-skillen), formidling/brev, frontend."
---

# Komplett vilkår for Aktivitetspenger

Denne skillen dekker hele spekteret for å opprette et nytt vilkår i aktivitetspenger — fra enkelt aksjonspunkt-drevet vilkår til fullt vilkår med faktaavklaring, bruker-varsling og to behandlingssteg.

**Referanseimplementasjoner:**
- **Enkel variant:** `BistandsvilkårSteg`, `VurderBehovForBistandOppdaterer`
- **Varslings-variant:** `VurderFaktaBostedSteg`, `VurderBosattVilkårSteg`, `BostedsGrunnlag*`, `VurderFaktaOmBostedOppdaterer`, `ManuellVurderingBostedsvilkårOppdaterer`, `BostedOppgaveOppretter`

---

## Beslutningstrekk — enkel vs. varslings-variant

```
Trenger vilkåret faktaavklaring med grunnlag og bruker-varsling?
│
├── NEI  →  Enkel variant
│           Aksjonspunkt + steg + DTO + oppdaterer + vilkår
│           Relevante steg: 0, 1, 2, 6 (enkel), 7 (ett steg), 8, 9 (enkel), 12 (enkel)
│
└── JA   →  Varslings-variant
            Grunnlag + Etterlysning + k9-format + ung-brukerdialog-api +
            faktasteg + vilkårssteg + oppdaterere + OppgaveOppretter + kopiering
            Relevante steg: 0–13 (alle)
```

Steg markert med **(alltid)** gjelder begge varianter.
Steg markert med **(kun ved varsling)** hoppes over ved enkel variant.
Steg markert med **(betinget)** har en eksplisitt betingelse angitt.

**Sentrale prinsipper for varslings-variant:**
- Faktagrunnlaget har én holder med `Kilde`-felt (`SØKNAD` / `SAKSBEHANDLER`)
- Det er **ett** aksjonspunkt for faktaregistrering — ikke et separat fastsett-steg
- Fakta fra søknad settes automatisk — de trenger aldri saksbehandlerbekreftelse
- Vilkårsvurdering er alltid manuell dersom: kilde=SØKNAD, bruker har avgitt uttalelse, eller vilkår-spesifikk årsak krever det
- Saksbehandler kan ved faktaregistrering velge å ikke varsle bruker hvis det foreligger åpenbar grunn

---

## Steg 0 — Samle inn detaljer (alltid)

Bruk `ask_user` (eller `vscode_askQuestions`) for å stille spørsmålene nedenfor. Ikke anta verdier — vent på svar.

### Overordnet valg

```
1. header: "Variant"
   question: "Trenger vilkåret faktaavklaring med grunnlag og bruker-varsling via Etterlysning?"
   options: [
     "Nei — enkel variant (aksjonspunkt + vilkår)",
     "Ja — varslings-variant (grunnlag + Etterlysning + to behandlingssteg)"
   ]
```

### Aksjonspunkt og steg (alltid)

```
2. header: "Aksjonspunktnavn"
   question: "Hva skal aksjonspunktet hete? (f.eks. 'Avklar om bruker oppfyller vilkåret')"

3. header: "Aksjonspunktkode"
   question: "Hvilken kode skal aksjonspunktet ha? (sjekk neste ledige i AksjonspunktKodeDefinisjon.java, 5xxx for manuell, 7xxx for auto)"

4. header: "Aksjonspunkttype"
   question: "Skal aksjonspunktet løses av lokalkontor eller sentral saksbehandler?"
   options: ["Lokal (LOKALKONTOR_MANUELL)", "Sentral (MANUELL)"]

5. header: "Steg"
   question: "Skal aksjonspunktet opprettes i et nytt steg eller legges til i et eksisterende steg?"
   options: ["Nytt steg", "Eksisterende steg"]

6. header: "Stegnavn"
   question: "Hvis nytt steg: hva skal det hete? Hvis eksisterende: hvilket steg skal det legges i? (sjekk BehandlingStegType.java og ProsessModell.java)"

7. header: "Totrinn"
   question: "Skal aksjonspunktet kreve totrinnskontroll?"
   options: ["Ja (TOTRINN)", "Nei (ENTRINN)"]

8. header: "Skjermlenke"
   question: "Hva skal skjermlenken hete i frontend? Svar 'ingen' hvis ikke relevant."

9. header: "Automatisk vurdering"
   question: "Kan steget løse seg automatisk i noen tilfeller, eller skal det alltid opprette aksjonspunkt?"
   options: ["Alltid aksjonspunkt", "Automatisk med fallback til aksjonspunkt", "Alltid automatisk (ingen aksjonspunkt)"]

10. header: "DTO-felter"
    question: "Hvilke felter trenger DTO-en utover begrunnelse? (f.eks. 'erVilkarOk: Boolean, avslagsårsak: enum'). Svar 'ingen' for kun begrunnelse."
```

### Vilkår (alltid)

```
11. header: "Vilkårnavn"
    question: "Hva skal vilkåret hete? (f.eks. 'Forutgående medlemskapsvilkåret')"

12. header: "Vilkårkode"
    question: "Hvilken kode skal vilkåret ha? (format AKT_VK_N, sjekk neste ledige i VilkårType.java)"

13. header: "Lovreferanse"
    question: "Hvilken lovreferanse gjelder for vilkåret? (f.eks. 'Forskrift om aktivitetspenger § X')"

14. header: "Avslagsårsaker"
    question: "Hvilke avslagsårsaker skal vilkåret ha? (oppgi navn og kode for hver, f.eks. 'SØKER_ER_IKKE_MEDLEM / 4001')"

15. header: "IKKE_RELEVANT-håndtering"
    question: "Skal vilkåret settes til IKKE_RELEVANT for perioder som allerede er avslått av andre vilkår?"
    options: ["Ja — filtrer bort perioder avslått av andre vilkår", "Nei — vilkåret vurderes uavhengig"]
```

### Varsling og grunnlag (kun ved varslings-variant)

```
16. header: "Faktaspørsmål"
    question: "Hva skal saksbehandler svare på? (eks. 'Er bruker bosatt i kommunen?')"

17. header: "Felt i grunnlag"
    question: "Hva lagres per skjæringstidspunkt? (eks. 'erBosattIKommune: Boolean')"

18. header: "Auto-avslags-logikk"
    question: "Hvilken feltverdi gir avslag, og hvilken Avslagsårsak brukes?"

19. header: "Autopunkt-kode"
    question: "Neste ledige 70xx-kode for autopunkt (vent på etterlysning). Sjekk AksjonspunktKodeDefinisjon.java."

20. header: "Manuelt vilkårsvurdering-AP-kode"
    question: "Kode for manuelt vilkårsvurdering-aksjonspunkt. Gjenbruk eksisterende eller ny 51xx-kode?"

21. header: "Manuell vilkårsbetingelse"
    question: "Finnes det vilkår-spesifikke årsaker utover kilde=SØKNAD og uttalelse som utløser manuell vurdering?"
```

Bruk svarene til å fylle inn konkrete verdier i alle steg under. Ikke bruk placeholder-navn.

---

## Steg 1 — Kodeverk (alltid)

Alle endringer i `kodeverk/`-modulen.

### 1a. BehandlingStegType (betinget: kun ved nytt steg)

Fil: `kodeverk/src/main/java/no/nav/ung/kodeverk/behandling/BehandlingStegType.java`

```java
MITT_STEG("MITT_STEG", "Beskrivelse av steget", BehandlingStatus.UTREDES),
```

> **Varslings-variant:** Legg til to stegtyper — ett for faktavurdering og ett for vilkårsvurdering:
> ```java
> VURDER_FAKTA_OM_MITT_VILKÅR("VURDER_FAKTA_OM_MITT_VILKÅR", "Vurder fakta om <vilkår>", BehandlingStatus.UTREDES),
> VURDER_MITT_VILKÅR("VURDER_MITT_VILKÅR", "Vurder <vilkår>", BehandlingStatus.UTREDES),
> ```

### 1b. AksjonspunktKodeDefinisjon

Fil: `kodeverk/src/main/java/no/nav/ung/kodeverk/behandling/aksjonspunkt/AksjonspunktKodeDefinisjon.java`

**Enkel variant:**
```java
public static final String MITT_AKSJONSPUNKT_KODE = "5XXX";
```

**Varslings-variant (to koder):**
```java
public static final String AUTO_SATT_PÅ_VENT_ETTERLYST_MITT_VILKÅR_UTTALELSE_KODE = "70XX";
public static final String MANUELL_VURDERING_MITT_VILKÅR_KODE = "51XX";
```

### 1c. SkjermlenkeType (betinget: kun hvis skjermlenke oppgitt)

Fil: `kodeverk/src/main/java/no/nav/ung/kodeverk/behandling/aksjonspunkt/SkjermlenkeType.java`

```java
MITT_AKSJONSPUNKT("MITT_AKSJONSPUNKT", "Min skjermlenke"),
```

Bruk `UTEN_SKJERMLENKE` i `AksjonspunktDefinisjon` hvis ikke relevant.

### 1d. AksjonspunktDefinisjon

Fil: `kodeverk/src/main/java/no/nav/ung/kodeverk/behandling/aksjonspunkt/AksjonspunktDefinisjon.java`

**Enkel variant — lokal (LOKALKONTOR_MANUELL):**
```java
MITT_AKSJONSPUNKT(AksjonspunktKodeDefinisjon.MITT_AKSJONSPUNKT_KODE,
    AksjonspunktType.LOKALKONTOR_MANUELL, "Beskrivelse",
    BehandlingStatus.UTREDES, BehandlingStegType.MITT_STEG,
    VilkårType.MITT_VILKÅR, SkjermlenkeType.MITT_AKSJONSPUNKT,
    TOTRINN, TILBAKE, null, AVVENTER_SAKSBEHANDLER),
```

**Enkel variant — sentral (MANUELL):**
```java
MITT_AKSJONSPUNKT(AksjonspunktKodeDefinisjon.MITT_AKSJONSPUNKT_KODE,
    AksjonspunktType.MANUELL, "Beskrivelse",
    BehandlingStatus.UTREDES, BehandlingStegType.MITT_STEG,
    VilkårType.MITT_VILKÅR, SkjermlenkeType.MITT_AKSJONSPUNKT,
    TOTRINN, AVVENTER_SAKSBEHANDLER),
```

**Varslings-variant (to aksjonspunkter):**
```java
AUTO_SATT_PÅ_VENT_ETTERLYST_MITT_VILKÅR_UTTALELSE(
    new AksjonspunktData(AUTO_SATT_PÅ_VENT_ETTERLYST_MITT_VILKÅR_UTTALELSE_KODE,
        AksjonspunktType.AUTOPUNKT, BehandlingStegType.VURDER_FAKTA_OM_MITT_VILKÅR,
        VurderingspunktType.UT, Venteårsak.VENTER_PÅ_ETTERLYST_MITT_VILKÅR_UTTALELSE,
        Duration.ofWeeks(2))),
MANUELL_VURDERING_MITT_VILKÅR(
    new AksjonspunktData(MANUELL_VURDERING_MITT_VILKÅR_KODE,
        AksjonspunktType.MANUELL, BehandlingStegType.VURDER_MITT_VILKÅR,
        VurderingspunktType.UT)),
```

### 1e. EtterlysningType, EndringType, Venteårsak (kun ved varsling)

**`EtterlysningType.java`:**
```java
UTTALELSE_MITT_VILKÅR("UTTALELSE_MITT_VILKÅR"),
```
Oppdater **begge** switch-metoder i samme fil:
- `tilAutopunktDefinisjon()` → `AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_MITT_VILKÅR_UTTALELSE`
- `mapTilVenteårsak()` → `Venteårsak.VENTER_PÅ_ETTERLYST_MITT_VILKÅR_UTTALELSE`

**`EndringType.java`:**
```java
AVKLAR_MITT_VILKÅR,
```

**`Venteårsak.java`:**
```java
VENTER_PÅ_ETTERLYST_MITT_VILKÅR_UTTALELSE("MITT_VILKÅR_UTTALELSE", "Venter på uttalelse fra bruker om <vilkår>"),
```

> **Viktig:** `EtterlysningType`-switch finnes i minst 4 filer — søk etter eksisterende `UTTALELSE_*` for å finne alle steder som må oppdateres.

---

## Steg 2 — VilkårType, Avslagsårsak og Vilkårregistrering (alltid)

### 2a. Avslagsårsak

Fil: `kodeverk/src/main/java/no/nav/ung/kodeverk/vilkår/Avslagsårsak.java`

```java
AVSLAGSÅRSAK_NAVN("KODE", "Beskrivelse",
    Map.of(FagsakYtelseType.AKTIVITETSPENGER, "Lovreferanse")),
```

### 2b. VilkårType

Fil: `kodeverk/src/main/java/no/nav/ung/kodeverk/vilkår/VilkårType.java`

```java
MITT_VILKÅR("AKT_VK_N", "Vilkårnavn",
    Map.of(FagsakYtelseType.AKTIVITETSPENGER, "Lovreferanse"),
    Avslagsårsak.MIN_AVSLAGSÅRSAK),
```

### 2c. Vilkårregistrering

Fil: `domenetjenester/perioder/src/main/java/no/nav/ung/sak/vilkår/AktivitetspengerInngangsvilkårUtleder.java`

Legg til det nye `VilkårType` i `YTELSE_VILKÅR`-listen:
```java
private static final List<VilkårType> YTELSE_VILKÅR = asList(
    ALDERSVILKÅR,
    BOSTEDSVILKÅR,
    FORUTGÅENDE_MEDLEMSKAPSVILKÅRET,
    MITT_VILKÅR   // <-- ny
);
```

---

## Steg 3 — Domeneentiteter og grunnlag (kun ved varsling)

Pakke: `no.nav.ung.sak.behandlingslager.<vilkaar>/`
**Viktig:** bruk `no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet`, IKKE `no.nav.ung.sak.typer`.

### Faktagrunnlag — enkelt holder med Kilde

| Klasse | Annotasjoner | Innhold |
|--------|-------------|---------|
| `<Vilkår>Avklaring` | `@Entity @Immutable` | `skaeringstidspunkt: LocalDate`, `<faktafelt>: Boolean`, `kilde: Kilde` — **ingen** `holderId`-felt (styres av `@JoinColumn` i holder) |
| `<Vilkår>AvklaringHolder` | `@Entity` | `@OneToMany(cascade=ALL) @JoinColumn(name="<vilkaar>_avklaring_holder_id") Set<<Vilkår>Avklaring>` + `equals()` på settet |
| `<Vilkår>Grunnlag` | `@Entity` | `behandlingId`, `aktiv=true`, `grunnlagsreferanse=UUID`, `@ManyToOne holder` (NOT NULL) |

> - `kilde = SØKNAD` — fakta satt automatisk fra brukerens søknad; utløser alltid manuell vilkårsvurdering
> - `kilde = SAKSBEHANDLER` — fakta registrert manuelt av saksbehandler
> - Det er **ikke** separate foreslåttHolder/fastsattHolder. All fakta lagres i ett holder.

### Separat søknadsaggregat

Søknadsdata skal ligge i et **eget aggregat** uavhengig av vilkårsperiode/skjæringstidspunkt:

```java
// <Vilkår>SøknadGrunnlag
// Inneholder List<<Vilkår>informasjonFraSøknad> med:
//   journalpostId, fomDato, <faktafelt> (eller tilsvarende)
```

### `Kilde`-enum (kodeverk/)

```java
public enum Kilde {
    SØKNAD,       // Fakta satt automatisk fra brukerens søknad
    SAKSBEHANDLER // Fakta registrert manuelt av saksbehandler
}
```

### `<Vilkår>GrunnlagRepository`

- `hentGrunnlagHvisEksisterer(behandlingId)` → `Optional<<Vilkår>Grunnlag>`
- `lagreAvklaringer(behandlingId, Map<LocalDate, AvklaringData>)` — kilde fra data; deaktiver gammelt grunnlag ved endring
- `lagreAvklaringerFraSøknad(behandlingId, Map<LocalDate, Boolean>)` — kilde=SØKNAD automatisk
- `kopierGrunnlagFraEksisterendeBehandling(gammel, ny)` — pek til samme holder (ingen kopi)

### ORM-registrering

Opprett `META-INF/pu-default.<vilkaar>.orm.xml`:

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

## Steg 4 — Flyway-migrering (kun ved varsling)

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

> - FK-kolonnen i `<vilkaar>_avklaring` heter `<vilkaar>_avklaring_holder_id` — dette matcher `@JoinColumn`-navnet i holder. Ikke legg til et separat `holderId`-felt i entiteten — det gir `Duplicate column`-feil fra Hibernate.
> - `kilde`-kolonnen settes til `SAKSBEHANDLER` som default; `lagreAvklaringerFraSøknad()` setter `SØKNAD` eksplisitt.
> - `gr_<vilkaar>_avklaring` har kun én holder-kolonne.

---

## Steg 5 — Eksterne repos (kun ved varsling)

### k9-format

**Fil:** `Bekreftelse.java` — legg til i `Type` enum og `@JsonSubTypes`:
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

Installer og oppdater versjon:
```bash
cd k9-format && mvn install -DskipTests
# Oppdater <k9format.version>X.X.X-SNAPSHOT</k9format.version> i ung-sak/pom.xml
```

### ung-brukerdialog-api

**`OppgaveType.java`:** legg til `BEKREFT_<VILKÅR>`

**Ny fil:** `Bekreft<Vilkår>OppgavetypeDataDto.java`:
```java
public record Bekreft<Vilkår>OppgavetypeDataDto(
    LocalDate fraOgMed, LocalDate tilOgMed, Boolean <faktafelt>
) implements OppgavetypeDataDto {}
```

Installer og oppdater versjon:
```bash
cd ung-brukerdialog-api && mvn install -DskipTests
# Oppdater <ung-brukerdialog-api.version>X.X.X-SNAPSHOT</ung-brukerdialog-api.version> i ung-sak/pom.xml
```

---

## Steg 6 — Kontrakt DTO

Plassering: `kontrakt/src/main/java/no/nav/ung/sak/kontrakt/aktivitetspenger/`

### Enkel variant (alltid)

Klassen skal:
- Utvide `BekreftetAksjonspunktDto`
- Annoteres med `@JsonTypeName(AksjonspunktKodeDefinisjon.MITT_AKSJONSPUNKT_KODE)`
- Inneholde felt for saksbehandlers beslutning
- Ha valideringslogikk med `@AssertTrue` om nødvendig

```java
@JsonTypeName(AksjonspunktKodeDefinisjon.MITT_AKSJONSPUNKT_KODE)
public class MittAksjonspunktDto extends BekreftetAksjonspunktDto {
    @NotNull
    private Boolean erVilkarOk;
    private Avslagsårsak avslagsårsak;

    @AssertTrue(message = "Avslagsårsak må oppgis ved avslag")
    public boolean isAvslagsårsakGyldig() {
        return Boolean.TRUE.equals(erVilkarOk) || avslagsårsak != null;
    }
}
```

**Referansefiler:**
- Enkel (kun begrunnelse): `kontrakt/.../aktivitetspenger/VurderBehovForBistandDto.java`
- Med validering: `kontrakt/.../aktivitetspenger/BekreftErMedlemVurderingDto.java`

### Varslings-variant (kun ved varsling)

**Fakta-DTO (aksjonspunkt `VURDER_FAKTA_OM_<VILKÅR>`):**
```java
// <Vilkår>AvklaringPeriodeDto.java
public record <Vilkår>AvklaringPeriodeDto(
    Periode periode,
    Boolean <faktafelt>,
    String begrunnelse,
    boolean ikkeVarsle   // saksbehandler velger å ikke sende etterlysning
) {}

// Vurder<Vilkår>Dto.java
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FAKTA_OM_<VILKÅR>_KODE)
public class Vurder<Vilkår>Dto extends BekreftetAksjonspunktDto {
    @NotNull @Size(min=1)
    private List<<Vilkår>AvklaringPeriodeDto> avklaringer;
}
```

**Manuell vilkårsvurdering-DTO (aksjonspunkt `MANUELL_VURDERING_<VILKÅR>`):**
```java
@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_MITT_VILKÅR_KODE)
public class Manuell<Vilkår>VilkårDto extends BekreftetAksjonspunktDto {
    @NotNull @Size(min=1, max=100)
    private List<@Valid VilkårPeriodeVurderingDto> vurdertePerioder;
}
```

`VilkårPeriodeVurderingDto`-felter: `periode (fom/tom)`, `erVilkårOppfylt`, `avslagsårsak`, `begrunnelse`.

---

## Steg 7 — Behandlingssteg

### Enkel variant — ett steg (alltid)

Klasse i `ytelse-aktivitetspenger/src/main/java/no/nav/ung/ytelse/aktivitetspenger/`:

```java
@ApplicationScoped
@BehandlingStegRef(value = MITT_STEG)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class MittSteg implements BehandlingSteg {

    @Inject
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject @Any
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();
        var tjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(
            perioderTilVurderingTjeneste, VilkårType.MITT_VILKÅR);
        var perioderTilVurdering = tjeneste.utled(behandlingId, VilkårType.MITT_VILKÅR);

        // Valgfritt: auto-vurder perioder der data finnes
        // Returnerer aksjonspunkt for resterende perioder
        return BehandleStegResultat.utførtMedAksjonspunkter(
            List.of(AksjonspunktDefinisjon.MITT_AKSJONSPUNKT));
    }
}
```

#### IKKE_RELEVANT-håndtering i steg (betinget: kun hvis valgt i steg 0)

Legg til etter periode-utledning:

```java
var vilkårene = vilkårResultatRepository.hent(behandlingId);
perioderTilVurdering = filtrerBortIkkeRelevantePerioder(perioderTilVurdering,
    vilkårene.getVilkår(VilkårType.MITT_VILKÅR));

var avslåttTidslinje = lagAvslåttTidslinje(vilkårene);
var avslåttePerioder = finnAvslåttePerioder(perioderTilVurdering, avslåttTidslinje);
if (!avslåttePerioder.isEmpty()) {
    vilkårResultatRepository.settPerioderTilIkkeRelevant(
        behandlingId, VilkårType.MITT_VILKÅR, avslåttePerioder);
    perioderTilVurdering.removeAll(avslåttePerioder);
}
```

**Referansefiler:**
- Enkel (alltid aksjonspunkt): `ytelse-aktivitetspenger/.../bistandsvilkår/BistandsvilkårSteg.java`
- Med IKKE_RELEVANT-filtrering: `ytelse-aktivitetspenger/.../medlemskap/ForutgåendeMedlemskapsvilkårSteg.java`
- Auto-vurdert: `ytelse-aktivitetspenger/.../aldersvilkår/VurderAldersvilkåretSteg.java`

---

### Varslings-variant — to steg (kun ved varsling)

**Referanse:** `VurderFaktaBostedSteg.java` og `VurderBosattVilkårSteg.java`.

#### Steg 7a — Faktasteg (`VURDER_FAKTA_OM_<VILKÅR>`)

Klasse: `VurderFakta<Vilkår>Steg implements BehandlingSteg` (ikke `VilkårVurderingSteg`).

```java
@Override
public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
    long behandlingId = kontekst.getBehandlingId();
    var perioderTilVurdering = VilkårsPerioderTilVurderingTjeneste
        .finnTjeneste(...).utled(behandlingId, VilkårType.MITT_VILKÅR);

    // Auto-sett fakta fra søknad for perioder til vurdering
    initierFaktaFraSøknadsdata(behandlingId, perioderTilVurdering);

    // Sjekk om manuell faktavurdering er nødvendig (prosess-trigger)
    var tidslinjeForManuellFaktavurdering =
        finnTidslinjeForManuellFaktavurdering(behandling, behandlingId);
    if (!tidslinjeForManuellFaktavurdering.isEmpty()) {
        return BehandleStegResultat.utførtMedAksjonspunkter(
            List.of(AksjonspunktDefinisjon.VURDER_FAKTA_OM_MITT_VILKÅR));
    }
    return BehandleStegResultat.utførtUtenAksjonspunkter();
}

private LocalDateTimeline<Boolean> finnTidslinjeForManuellFaktavurdering(...) {
    return ProsessTriggerPeriodeUtleder.finnTjeneste(...)
        .utledTidslinje(behandlingId)
        .filterValue(it -> it.contains(BehandlingÅrsakType.ENDRET_MITT_VILKÅR))
        .mapValue(_ -> true);
}
```

#### Steg 7b — Vilkårsvurderingssteg (`VURDER_<VILKÅR>VILKÅR`)

Klasse: `Vurder<Vilkår>VilkårSteg extends VilkårVurderingSteg`.

**Klassifisering med `StegUtfall`-enum:**
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
| Alle andre (utløpt, svar uten uttalelse) | `VILKÅR_VURDERES_AUTOMATISK` |

**Tilstandsmaskin i `utførResten()`:**
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
        List.of(AksjonspunktDefinisjon.MANUELL_VURDERING_MITT_VILKÅR));
}
return BehandleStegResultat.utførtUtenAksjonspunkter();
```

**`autoVurder()` — itererer tidslinje, ikke råperioder:**
```java
private void autoVurder(long behandlingId,
                        LocalDateTimeline<StegUtfall> stegutfallTidslinje,
                        <Vilkår>AvklaringHolder holder) {
    stegutfallTidslinje.filterValue(VILKÅR_VURDERES_AUTOMATISK::equals)
        .toSegments()
        .forEach(s -> {
            var avklaring = holder.getPeriodeAvklaring(s.getFom()).orElseThrow(...);
            var utfall = avklaring.get<Faktafelt>() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            var avslagsårsak = utfall == Utfall.IKKE_OPPFYLT ? Avslagsårsak.MIN_ÅRSAK : null;
            vilkårBuilder.hentBuilderFor(s.getFom(), s.getTom())
                .medUtfall(utfall)
                .medAvslagsårsak(avslagsårsak)
                .medRegelInput(lagRegelInput(avklaring));
        });
}

private static String lagRegelInput(<Vilkår>Avklaring avklaring) {
    return VILKAR_JSON_OBJECT_MAPPER.writeValueAsString(
        new RegelInput(avklaring.getReferanse(), avklaring.getSkaeringstidspunkt(),
                       avklaring.get<Faktafelt>(), avklaring.getKilde()));
}
private record RegelInput(UUID referanse, LocalDate skjaeringstidspunkt,
                          boolean <faktafelt>, Kilde kilde) {}
```

---

## Steg 8 — Prosessmodell (betinget: kun ved nytt steg)

Fil: `ytelse-aktivitetspenger/src/main/java/no/nav/ung/ytelse/aktivitetspenger/prosess/ProsessModell.java`

Legg til steget i riktig posisjon. Eksisterende stegkjede (forkortet):

```java
modellBuilder
    .medSteg(BehandlingStegType.ALDERSVILKÅRET)
    .medSteg(BehandlingStegType.VURDER_BOSTED)       // <-- fakta-steg for BOSTEDSVILKÅR
    .medSteg(BehandlingStegType.VURDER_BISTANDSVILKÅR)
    .medSteg(BehandlingStegType.LOKALKONTOR_FORESLÅ_VILKÅR)
    .medSteg(BehandlingStegType.LOKALKONTOR_BESLUTTER_VILKÅR)
    .medSteg(BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR)
    .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
    // ...
```

**Plasseringsregler:**
- **Lokal** (LOKALKONTOR_MANUELL): steget plasseres **før** `LOKALKONTOR_FORESLÅ_VILKÅR`
- **Sentral** (MANUELL): steget plasseres **etter** `LOKALKONTOR_BESLUTTER_VILKÅR`
- **Varslings-variant:** legg til begge steg i rekkefølge — faktasteg **før** vilkårssteg

---

## Steg 9 — Oppdaterer(e)

Plassering: `web/src/main/java/no/nav/ung/sak/web/app/tjenester/behandling/aktivitetspenger/`

### Enkel variant — én oppdaterer (alltid)

```java
@ApplicationScoped
@DtoTilServiceAdapter(dto = MittAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class MittAksjonspunktOppdaterer implements AksjonspunktOppdaterer<MittAksjonspunktDto> {

    @Override
    public OppdateringResultat oppdater(MittAksjonspunktDto dto, AksjonspunktOppdatererParameter param) {
        var resultatBuilder = param.getVilkårResultatBuilder();
        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.MITT_VILKÅR);

        var tjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(
            perioderTilVurderingTjeneste, VilkårType.MITT_VILKÅR);
        var perioderTilVurdering = tjeneste.utled(param.getBehandlingId(), VilkårType.MITT_VILKÅR);

        var utfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        var avslagsårsak = dto.getErVilkarOk() ? null : dto.getAvslagsårsak();

        perioderTilVurdering.stream()
            .map(periode -> vilkårBuilder.hentBuilderFor(periode)
                .medUtfall(utfall)
                .medAvslagsårsak(avslagsårsak)
                .medRegelInput("..."))
            .forEach(vilkårBuilder::leggTil);

        resultatBuilder.leggTil(vilkårBuilder);
        return OppdateringResultat.nyttResultat();
    }
}
```

#### IKKE_RELEVANT-filtrering i oppdaterer (betinget: kun hvis IKKE_RELEVANT-håndtering valgt)

Filtrer bort IKKE_RELEVANT-perioder før vilkårsoppdatering:

```java
var perioderTilVurdering = tjeneste.utled(param.getBehandlingId(), VilkårType.MITT_VILKÅR);
var relevantePerioder = filtrerBortIkkeRelevantePerioder(param.getBehandlingId(), perioderTilVurdering);
// Bruk relevantePerioder videre i stedet for perioderTilVurdering
```

**Referansefiler:**
- Enkel: `web/.../aktivitetspenger/VurderBehovForBistandOppdaterer.java`
- Med IKKE_RELEVANT: `web/.../aktivitetspenger/BekreftErMedlemVurderingOppdaterer.java`

---

### Varslings-variant — to oppdaterere (kun ved varsling)

#### Oppdaterer 1 — Faktaregistrering (`Vurder<Vilkår>Oppdaterer`)

Håndterer aksjonspunkt `VURDER_FAKTA_OM_<VILKÅR>`:
1. Lagre fakta via `lagreAvklaringer()` med `kilde=SAKSBEHANDLER`
2. For perioder der `ikkeVarsle=false` **og** ingen aktiv/besvart etterlysning finnes: opprett `Etterlysning(UTTALELSE_MITT_VILKÅR)` + planlegg `OpprettEtterlysningTask`
3. For perioder der `ikkeVarsle=true`: hopp over etterlysning
4. Sjekk `hentBesvartEtterlysninger()` — ikke send ny etterlysning for perioder som allerede har `MOTTATT_SVAR`
5. **Returner `rekjørSteg()`** — IKKE `nyttResultat()` — steg re-kjøres og setter autopunkt ved ny etterlysning

```java
return OppdateringResultat.rekjørSteg();
```

#### Oppdaterer 2 — Manuell vilkårsvurdering (`Manuell<Vilkår>VilkårOppdaterer`)

Håndterer aksjonspunkt `MANUELL_VURDERING_<VILKÅR>`. Følg `VurderAndreLivsoppholdsytelserOppdaterer`-mønsteret:

```java
for (VilkårPeriodeVurderingDto vurdertPeriode : dto.getVurdertePerioder()) {
    Utfall utfall = vurdertPeriode.erVilkårOppfylt() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
    vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(vurdertPeriode.fom(), vurdertPeriode.tom())
        .medUtfallManuell(utfall)
        .medAvslagsårsak(vurdertPeriode.avslagsårsak())
        .medBegrunnelse(vurdertPeriode.begrunnelse())); // begrunnelse inkluderes i brevet
}
return OppdateringResultat.nyttResultat(); // IKKE rekjørSteg()
```

---

## Steg 10 — OppgaveOppretter og tjenesteoppdateringer (kun ved varsling)

### Ny klasse `<Vilkår>OppgaveOppretter`

```java
@ApplicationScoped
@OppgaveTypeRef(UNG_MITT_VILKÅR_AVKLARING)
public class <Vilkår>OppgaveOppretter implements OppgaveOppretter {

    @Override
    public OppgavetypeDataDto lagOppgaveData(Etterlysning e) {
        var avklaring = repo.hentGrunnlagHvisEksisterer(e.getBehandlingId())
            .orElseThrow().getHolder().finnAvklaring(e.getPeriode().getFomDato());
        return new Bekreft<Vilkår>OppgavetypeDataDto(
            e.getPeriode().getFomDato(),
            e.getPeriode().getTomDato(),
            avklaring.get<Faktafelt>());
    }
}
```

### Filer som alltid må oppdateres ved ny `EtterlysningType`/`EndringType`

| Fil | Hva legges til |
|-----|---------------|
| `OpprettOppgaveTjeneste` | `case UTTALELSE_MITT_VILKÅR` |
| `EtterlysningOgUttalelseTjeneste` | `case UTTALELSE_MITT_VILKÅR` |
| `GenerellOppgaveBekreftelseHåndterer` | `@OppgaveTypeRef` + `case UNG_MITT_VILKÅR_AVKLARING` i `mapTilEndringsType()` |
| `HistorikkinnslagTjeneste` | `case AVKLAR_MITT_VILKÅR` |

---

## Steg 11 — Kopiering ved revurdering (kun ved varsling)

Fil: `GrunnlagKopiererAktivitetspenger.java`

```java
@Inject <Vilkår>GrunnlagRepository <vilkaar>GrunnlagRepository;
@Inject <Vilkår>SøknadGrunnlagRepository <vilkaar>SøknadGrunnlagRepository;

// Legg til i begge kopier()-metoder:
<vilkaar>GrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(gammel, ny);
<vilkaar>SøknadGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(gammel, ny);
```

---

## Steg 12 — Test (alltid)

Plassering: `ytelse-aktivitetspenger/src/test/java/` i samme pakkestruktur som steget.

### Oppsett

```java
@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class MittStegTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;
    private MittSteg steg;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        steg = new MittSteg(/* injiser avhengigheter fra repositoryProvider */);
    }
}
```

### Bruk AktivitetspengerTestScenarioBuilder

Bruk **alltid** `AktivitetspengerTestScenarioBuilder` for å bygge testscenarioer.

```java
var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
    .leggTilVilkår(VilkårType.MITT_VILKÅR, Utfall.IKKE_VURDERT, vilkårPeriode)
    .lagre(entityManager);

var kontekst = new BehandlingskontrollKontekst(
    behandling.getFagsakId(), behandling.getAktørId(),
    behandlingRepository.taSkriveLås(behandling.getId()));

var resultat = steg.utførSteg(kontekst);
assertThat(resultat.getAksjonspunktListe())
    .containsExactly(AksjonspunktDefinisjon.MITT_AKSJONSPUNKT);
```

**Viktig:** Hvis det nye aksjonspunktet krever data som builderen ikke støtter, **utvid builderen** med nye metoder i stedet for å bygge testdata manuelt.

Fil: `ytelse-aktivitetspenger/src/test/java/no/nav/ung/ytelse/aktivitetspenger/testdata/AktivitetspengerTestScenarioBuilder.java`

### Testcaser å dekke (alltid)

- Aksjonspunkt-produksjon: steget returnerer riktig aksjonspunkt
- Automatisk løsning: steget løser seg selv når mulig (hvis relevant)
- Vilkåret som allerede er vurdert hoppes over
- Riktig avslagsårsak settes ved avslag
- IKKE_RELEVANT-håndtering (betinget):
  - Perioder avslått av andre vilkår settes til IKKE_RELEVANT
  - Delvis avslåtte perioder skal fortsatt vurderes

**Referansetest:** `ytelse-aktivitetspenger/src/test/java/no/nav/ung/ytelse/aktivitetspenger/medlemskap/ForutgåendeMedlemskapsvilkårStegTest.java`

### Varslings-variant — ekstra testcaser (kun ved varsling)

- Faktasteg oppretter AP for manuell faktavurdering ved ENDRET_MITT_VILKÅR prosess-trigger
- Faktasteg initierer fakta automatisk fra søknad
- Vilkårssteg setter på vent når etterlysning er aktiv
- Vilkårssteg auto-vurderer ved utløpt etterlysning
- Vilkårssteg oppretter manuelt AP ved mottatt uttalelse eller kilde=SØKNAD
- Oppdaterer oppretter etterlysning (ikke ved `ikkeVarsle=true`)
- Oppdaterer oppretter ikke ny etterlysning ved eksisterende `MOTTATT_SVAR`

---

## Steg 13 — Integrasjonstester k9-verdikjede (kun ved varsling)

**`LokalkontorSteg.java`:**
- Oppdater `saksbehandlerVurdererOgForeslårVilkår` med nye params
- Legg til `sendInn<Vilkår>Bekreftelse(steg, deltaker, søkerIdent)` som poster `Vurder<Vilkår>Dto`
- Fjern `VURDER_FAKTA_OM_MITT_VILKÅR` fra `LokalkontorBeslutterVilkårAksjonspunktDto` i beslutter-steget (vilkåret er nå auto-vurdert)

**`AktivitetspengerTest.java`:**
- Legg til `UngdomsprogramDeltaker deltaker`-felt
- Pass `søkerIdent + deltaker + fordelingSteg` til `saksbehandlerVurdererOgForeslårVilkår`

---

## Viktige mønstre

### Lokal vs. sentral aksjonspunkttype

| Type | AksjonspunktType | Prosessmodell-plassering |
|------|-----------------|--------------------------|
| **Lokal** (lokalkontor) | `LOKALKONTOR_MANUELL` | **Før** `LOKALKONTOR_FORESLÅ_VILKÅR` |
| **Sentral** | `MANUELL` | **Etter** `LOKALKONTOR_BESLUTTER_VILKÅR` |

### VilkårResultatBuilder

Bruk alltid builder-mønsteret for å oppdatere vilkår:

```java
var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);
var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.MITT_VILKÅR);
var periodeBuilder = vilkårBuilder.hentBuilderFor(periode)
    .medUtfall(Utfall.OPPFYLT)
    .medAvslagsårsak(null)
    .medRegelInput("...");
vilkårBuilder.leggTil(periodeBuilder);
resultatBuilder.leggTil(vilkårBuilder);
vilkårResultatRepository.lagre(behandlingId, resultatBuilder.build());
```

### CDI-oppdagelse

Alle steg og oppdaterere oppdages automatisk via CDI-annotasjoner. Ingen manuell registrering utover prosessmodellen er nødvendig.

### `rekjørSteg()` vs. `nyttResultat()` i oppdaterer

| Oppdaterer | Retur | Årsak |
|-----------|-------|-------|
| Fakta-oppdaterer (varsling) | `rekjørSteg()` | Steg re-kjøres og finner ny etterlysning → setter autopunkt |
| Manuell vilkårsvurdering (varsling) | `nyttResultat()` | Vilkåret er allerede satt via `param.getVilkårResultatBuilder()` |
| Enkel oppdaterer | `nyttResultat()` | Vilkårsresultat er lagret |

---

## Viktige gotchas

| Problem | Løsning |
|---------|---------|
| `DatoIntervallEntitet` ikke funnet | Bruk `no.nav.ung.sak.domene.typer.tid`, IKKE `no.nav.ung.sak.typer` |
| Switch-exhaustiveness-feil | `EtterlysningType`-switch finnes i minst 4 filer — søk etter eksisterende `UTTALELSE_*` |
| k9-format/brukerdialog-api ikke oppdatert | Sjekk `<k9format.version>` og `<ung-brukerdialog-api.version>` i root `pom.xml` |
| `UnknownEntityException: Could not resolve root entity` i tester | Mangler `pu-default.<vilkaar>.orm.xml` — se Steg 3 |
| `Duplicate column '<vilkaar>_avklaring_holder_id'` fra Hibernate | `<Vilkår>Avklaring` har overflødig `holderId`-felt — fjern det |
| `method does not override` i tester | Kjør med `-am`: `mvn test -pl <modul> -am -Dsurefire.failIfNoSpecifiedTests=false` |
| `getAksjonspunktDefinisjon()` finnes ikke | `getAksjonspunktListe()` returnerer `List<AksjonspunktDefinisjon>` direkte — sammenlign element |
| Lambda-kompileringsfeil: "must be final or effectively final" | Bruk `final`-kopi før lambda: `final Map<...> avklaringLookup = periodeAvklaringPerFom;` |
| Duplikat etterlysning sendt for `MOTTATT_SVAR` | Sjekk `hentBesvartEtterlysninger()` i tillegg til `hentEtterlysningerSomVenterPåSvar()` |
| Beslutte AP for manuell vurdering feil | Bruk `VilkårPeriodeVurderingDto` med `erVilkårOppfylt=false` og `avslagsårsak` for avslag |
| Beslutter godkjenner auto-vurderte vilkår | Fjern AP fra `LokalkontorBeslutterVilkårAksjonspunktDto` i beslutter-steget |
| Søknadsdata trenger uavhengig persistering | Bruk eget søknadsaggregat — ikke koble søknadsdata direkte til holder |

---

## Utenfor scope

Denne skillen dekker **ikke**:
- Aksjonspunkt uten vilkår — bruk `new-aksjonspunkt`-skillen
- Inntektskontroll — bruk `inntektskontroll`-skillen
- Formidling/brev (`formidling-pdfgen-templates`, formidling-moduler)
- Frontend-endringer (ung-sak-web)
- Ungdomsprogramytelsen (annet vilkårsmønster)
- Flyway-migreringer for vilkårsresultat (vilkår lagres dynamisk, ikke som faste DB-tabeller)
