---
name: new-aksjonspunkt
description: Legg til eller endre aksjonspunkt, steg, DTO og oppdaterer for aktivitetspenger i ung-sak. USE FOR: opprette nytt aksjonspunkt, endre eksisterende aksjonspunkt, legge til felt i aksjonspunkt-DTO, endre steg-logikk, flytte steg i prosessmodell, endre aksjonspunkttype (lokal/sentral). DO NOT USE FOR: vilkår-spesifikke endringer (bruk new-vilkaar), formidling/brev, frontend.
---

# Nytt aksjonspunkt for Aktivitetspenger

Denne skillen beskriver mønsteret for å legge til et nytt aksjonspunkt i **aktivitetspenger**-ytelsen.
Et aksjonspunkt opprettes i et behandlingssteg og løses av saksbehandler via en oppdaterer.

Aksjonspunktet er ikke nødvendigvis knyttet til et vilkår. Hvis det nye aksjonspunktet skal knyttes til et vilkår, bruk også `new-vilkaar`-skillen etter denne.

## Arbeidsflyt

**Steg 0 — Samle inn detaljer fra bruker**

Før du skriver kode, bruk `vscode_askQuestions` for å stille følgende spørsmål. Ikke anta verdier — vent på svar.

```
Spørsmål å stille (bruk vscode_askQuestions):

1. header: "Aksjonspunktnavn"
   question: "Hva skal aksjonspunktet hete? (f.eks. 'Avklar om bruker har gyldig medlemskap')"

2. header: "Aksjonspunktkode"
   question: "Hvilken kode skal aksjonspunktet ha? (sjekk neste ledige i AksjonspunktKodeDefinisjon.java, 5xxx for manuell, 7xxx for auto)"

3. header: "Aksjonspunkttype"
   question: "Skal aksjonspunktet løses av lokalkontor eller sentral saksbehandler?"
   options: ["Lokal (LOKALKONTOR_MANUELL)", "Sentral (MANUELL)"]

4. header: "Steg"
   question: "Skal aksjonspunktet opprettes i et nytt steg eller legges til i et eksisterende steg?"
   options: ["Nytt steg", "Eksisterende steg"]

5. header: "Stegnavn"
   question: "Hvis nytt steg: hva skal det hete? Hvis eksisterende: hvilket steg skal det legges i? (sjekk BehandlingStegType.java og ProsessModell.java)"

6. header: "Totrinn"
   question: "Skal aksjonspunktet kreve totrinnskontroll?"
   options: ["Ja (TOTRINN)", "Nei (ENTRINN)"]

7. header: "Skjermlenke"
   question: "Hva skal skjermlenken hete i frontend? (f.eks. 'Medlemskap'). Svar 'ingen' hvis ikke relevant."

8. header: "Vilkårtilknytning"
   question: "Er aksjonspunktet knyttet til et vilkår?"
   options: ["Ja — bruk new-vilkaar-skillen etterpå", "Nei (UTEN_VILKÅR)"]

9. header: "Automatisk vurdering"
   question: "Kan steget løse seg automatisk i noen tilfeller, eller skal det alltid opprette aksjonspunkt?"
   options: ["Alltid aksjonspunkt", "Automatisk med fallback til aksjonspunkt", "Alltid automatisk (ingen aksjonspunkt)"]

10. header: "DTO-felter"
    question: "Hvilke felter trenger DTO-en utover begrunnelse? (f.eks. 'erVilkarOk: Boolean, avslagsårsak: enum'). Svar 'ingen' for kun begrunnelse."
```

Bruk svarene til å fylle inn konkrete verdier i alle steg under. Ikke bruk placeholder-navn.

## Lokal vs Sentral aksjonspunkttype

Aktivitetspenger har to typer manuelle aksjonspunkter:

| Type | AksjonspunktType | Eksempel |
|------|-----------------|----------|
| **Lokal** (lokalkontor) | `LOKALKONTOR_MANUELL` | `VURDER_BISTANDSVILKÅR` — løses av lokalkontor-saksbehandler |
| **Sentral** | `MANUELL` | `AVKLAR_GYLDIG_MEDLEMSKAP` — løses av sentral saksbehandler |

Valget påvirker:
- Hvem som ser og løser aksjonspunktet i frontend
- Om aksjonspunktet samles i `LOKALKONTOR_FORESLÅ_VILKÅR`/`LOKALKONTOR_BESLUTTER_VILKÅR`-flyten
- Plasseringen i prosessmodellen (se steg 4)

## Steg-for-steg sjekkliste

### 1. Kodeverk-definisjoner

Alle endringer i `kodeverk/`-modulen.

#### 1a. BehandlingStegType (kun ved nytt steg)

Hopp over dette steget hvis aksjonspunktet legges i et eksisterende steg.

Fil: `kodeverk/src/main/java/no/nav/ung/kodeverk/behandling/BehandlingStegType.java`

Legg til ny stegtype-konstant:
```java
MITT_STEG("MITT_STEG", "Beskrivelse av steget", BehandlingStatus.UTREDES),
```

#### 1b. AksjonspunktKodeDefinisjon

Fil: `kodeverk/src/main/java/no/nav/ung/kodeverk/behandling/aksjonspunkt/AksjonspunktKodeDefinisjon.java`

Legg til ny kode-konstant (velg neste ledige nummer):
```java
public static final String MITT_AKSJONSPUNKT_KODE = "5XXX";
```

#### 1c. SkjermlenkeType (valgfritt)

Fil: `kodeverk/src/main/java/no/nav/ung/kodeverk/behandling/aksjonspunkt/SkjermlenkeType.java`

Legg til ny skjermlenketype hvis aksjonspunktet trenger UI-navigasjon:
```java
MITT_AKSJONSPUNKT("MITT_AKSJONSPUNKT", "Min skjermlenke"),
```

Bruk `UTEN_SKJERMLENKE` i `AksjonspunktDefinisjon` hvis ikke relevant.

#### 1d. AksjonspunktDefinisjon

Fil: `kodeverk/src/main/java/no/nav/ung/kodeverk/behandling/aksjonspunkt/AksjonspunktDefinisjon.java`

Legg til ny enum-konstant. Mønsteret avhenger av aksjonspunkttype:

**Sentral (MANUELL) uten vilkår:**
```java
MITT_AKSJONSPUNKT(AksjonspunktKodeDefinisjon.MITT_AKSJONSPUNKT_KODE,
    AksjonspunktType.MANUELL, "Beskrivelse",
    BehandlingStatus.UTREDES, BehandlingStegType.MITT_STEG,
    UTEN_VILKÅR, SkjermlenkeType.MITT_AKSJONSPUNKT,
    TOTRINN, AVVENTER_SAKSBEHANDLER),
```

**Lokal (LOKALKONTOR_MANUELL) uten vilkår:**
```java
MITT_AKSJONSPUNKT(AksjonspunktKodeDefinisjon.MITT_AKSJONSPUNKT_KODE,
    AksjonspunktType.LOKALKONTOR_MANUELL, "Beskrivelse",
    BehandlingStatus.UTREDES, BehandlingStegType.MITT_STEG,
    UTEN_VILKÅR, SkjermlenkeType.MITT_AKSJONSPUNKT,
    TOTRINN, TILBAKE, null, AVVENTER_SAKSBEHANDLER),
```

**Med vilkår:** Erstatt `UTEN_VILKÅR` med `VilkårType.MITT_VILKÅR` (se `new-vilkaar`-skillen).

### 2. DTO

Opprett ny DTO-klasse i `kontrakt/src/main/java/no/nav/ung/sak/kontrakt/aktivitetspenger/`.

Klassen skal:
- Utvide `BekreftetAksjonspunktDto`
- Annoteres med `@JsonTypeName(AksjonspunktKodeDefinisjon.MITT_AKSJONSPUNKT_KODE)`
- Inneholde felt for saksbehandlers beslutning
- Ha valideringslogikk med `@AssertTrue` om nødvendig

**Referansefiler:**
- Enkel DTO (kun begrunnelse): `kontrakt/src/main/java/no/nav/ung/sak/kontrakt/aktivitetspenger/VurderBehovForBistandDto.java`
- Kompleks DTO med validering: `kontrakt/src/main/java/no/nav/ung/sak/kontrakt/aktivitetspenger/BekreftErMedlemVurderingDto.java`

### 3. Behandlingssteg

**Alternativ A — Nytt steg:**

Opprett ny steg-klasse i `ytelse-aktivitetspenger/src/main/java/no/nav/ung/ytelse/aktivitetspenger/`.

Klassen skal:
- Implementere `BehandlingSteg`
- Ha annotasjoner: `@ApplicationScoped`, `@BehandlingStegRef(value = MITT_STEG)`, `@BehandlingTypeRef`, `@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)`
- Implementere `utførSteg()` som returnerer:
  - `BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.MITT_AKSJONSPUNKT))` — når aksjonspunkt trengs
  - `BehandleStegResultat.utførtUtenAksjonspunkter()` — når steget løser seg automatisk

**Alternativ B — Eksisterende steg:**

Utvid `utførSteg()` i det eksisterende steget til også å returnere det nye aksjonspunktet når betingelsene er oppfylt. Legg til aksjonspunktet i listen som returneres via `BehandleStegResultat.utførtMedAksjonspunkter()`. Husk at `AksjonspunktDefinisjon` må peke på riktig `BehandlingStegType` — det eksisterende stegets type.

**Referansefiler:**
- Enkel (alltid aksjonspunkt): `ytelse-aktivitetspenger/src/main/java/no/nav/ung/ytelse/aktivitetspenger/del1/steg/bistandsvilkår/BistandsvilkårSteg.java`
- Kompleks (auto + aksjonspunkt, IKKE_RELEVANT-filtrering, avslått-tidslinje): `ytelse-aktivitetspenger/src/main/java/no/nav/ung/ytelse/aktivitetspenger/medlemskap/ForutgåendeMedlemskapsvilkårSteg.java`

#### IKKE_RELEVANT-filtrering i steg (valgfritt)

Dette er kun relevant når steget er knyttet til et vilkår og vilkåret kommer etter andre vilkår i prosessmodellen. Sjekk med `new-vilkaar`-skillen om IKKE_RELEVANT-håndtering er valgt.

Når et steg vurderer et vilkår der andre vilkår allerede kan ha avslått perioder, bør steget:

1. **Filtrere bort IKKE_RELEVANT-perioder** — perioder som allerede er satt til IKKE_RELEVANT fra en tidligere kjøring skal ikke vurderes på nytt.
2. **Sette avslåtte perioder til IKKE_RELEVANT** — hvis et annet vilkår har avslått en hel vilkårsperiode, sett den til IKKE_RELEVANT via `vilkårResultatRepository.settPerioderTilIkkeRelevant()`.
3. **Per-periode sjekk** — bruk `disjoint()` for å sjekke om hele perioden er dekket av avslått tidslinje (ikke bare delvis overlapp).

Mønster fra `ForutgåendeMedlemskapsvilkårSteg`:
```java
var vilkårene = vilkårResultatRepository.hent(behandlingId);
periodeTilVurdering = filtrerBortIkkeRelevantePerioder(periodeTilVurdering, vilkårene.getVilkår(VilkårType.MITT_VILKÅR));

var avslåttTidslinje = lagAvslåttTidslinje(vilkårene);
var avslåttePerioder = finnAvslåttePerioder(periodeTilVurdering, avslåttTidslinje);
if (!avslåttePerioder.isEmpty()) {
    vilkårResultatRepository.settPerioderTilIkkeRelevant(behandlingId, VilkårType.MITT_VILKÅR, avslåttePerioder);
    periodeTilVurdering.removeAll(avslåttePerioder);
}
```

### 4. Registrer steg i prosessmodell (kun ved nytt steg)

Hopp over dette steget hvis aksjonspunktet legges i et eksisterende steg.

Fil: `ytelse-aktivitetspenger/src/main/java/no/nav/ung/ytelse/aktivitetspenger/prosess/ProsessModell.java`

Legg til steget i riktig posisjon i stegkjeden. Rekkefølgen bestemmer eksekveringsordenen.

```java
modellBuilder
    .medSteg(BehandlingStegType.START_STEG, StartpunktType.START)
    .medSteg(BehandlingStegType.INIT_PERIODER, StartpunktType.INIT_PERIODER)
    .medSteg(BehandlingStegType.INIT_VILKÅR)
    .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
    .medSteg(BehandlingStegType.ALDERSVILKÅRET)
    .medSteg(BehandlingStegType.VURDER_BOSTED)
    .medSteg(BehandlingStegType.VURDER_BISTANDSVILKÅR)
    .medSteg(BehandlingStegType.LOKALKONTOR_FORESLÅ_VILKÅR)
    .medSteg(BehandlingStegType.LOKALKONTOR_BESLUTTER_VILKÅR)
    .medSteg(BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR)
    .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
    .medSteg(BehandlingStegType.AKTIVITETSPENGER_BEREGNING, StartpunktType.BEREGNING)
    .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
    .medSteg(BehandlingStegType.FATTE_VEDTAK)
    .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
```

**Plasseringsregler:**
- **Lokal** aksjonspunkt (LOKALKONTOR_MANUELL): Steget plasseres **før** `LOKALKONTOR_FORESLÅ_VILKÅR`, da lokale aksjonspunkter samles i foreslå/beslutter-flyten
- **Sentral** aksjonspunkt (MANUELL): Steget plasseres **etter** `LOKALKONTOR_BESLUTTER_VILKÅR`

### 5. Aksjonspunkt-oppdaterer

Opprett ny oppdaterer-klasse i `web/src/main/java/no/nav/ung/sak/web/app/tjenester/behandling/aktivitetspenger/`.

Klassen skal:
- Implementere `AksjonspunktOppdaterer<MittAksjonspunktDto>`
- Ha annotasjoner: `@ApplicationScoped`, `@DtoTilServiceAdapter(dto = MittAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)`
- Implementere `oppdater()` som behandler saksbehandlers beslutning og returnerer `OppdateringResultat.nyttResultat()`

**Referansefiler:**
- Enkel: `web/src/main/java/no/nav/ung/sak/web/app/tjenester/behandling/aktivitetspenger/VurderBehovForBistandOppdaterer.java`
- Med vilkår/avslagsårsak og IKKE_RELEVANT-filtrering: `web/src/main/java/no/nav/ung/sak/web/app/tjenester/behandling/aktivitetspenger/BekreftErMedlemVurderingOppdaterer.java`

#### IKKE_RELEVANT-filtrering i oppdaterer (valgfritt)

Dette er kun relevant hvis IKKE_RELEVANT-håndtering er valgt i `new-vilkaar`-skillen.

Hvis steget setter perioder til IKKE_RELEVANT (fordi andre vilkår har avslått dem), må oppdatereren også filtrere bort disse periodene slik at saksbehandlers vurdering kun gjelder relevante perioder:

```java
var perioderTilVurdering = perioderTilVurderingTjeneste.utled(param.getBehandlingId(), VilkårType.MITT_VILKÅR);
var relevantePerioder = filtrerBortIkkeRelevantePerioder(param.getBehandlingId(), perioderTilVurdering);

// Bruk relevantePerioder i stedet for perioderTilVurdering videre
```

Filtreringsmetoden sjekker vilkårets eksisterende IKKE_RELEVANT-perioder via `VilkårResultatRepository`.

### 6. Test for behandlingssteget

Opprett test i `ytelse-aktivitetspenger/src/test/java/` i samme pakkestruktur som steget.

#### Oppsett

Testen bruker JPA- og CDI-extensions:
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

#### Bruk AktivitetspengerTestScenarioBuilder

Bruk **alltid** `AktivitetspengerTestScenarioBuilder` for å bygge testscenarioer:

```java
var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
    .leggTilVilkår(VilkårType.MITT_VILKÅR, Utfall.IKKE_VURDERT, vilkårPeriode)
    .lagre(entityManager);

var kontekst = new BehandlingskontrollKontekst(
    behandling.getFagsakId(), behandling.getAktørId(),
    behandlingRepository.taSkriveLås(behandling.getId()));

var resultat = steg.utførSteg(kontekst);
assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.MITT_AKSJONSPUNKT);
```

Fil: `ytelse-aktivitetspenger/src/test/java/no/nav/ung/ytelse/aktivitetspenger/testdata/AktivitetspengerTestScenarioBuilder.java`

**Viktig:** Hvis det nye aksjonspunktet krever data som builderen ikke støtter ennå (f.eks. nye grunnlagstyper, spesielle søknadsdata), **utvid builderen** med nye metoder i stedet for å bygge testdata manuelt.

#### Hva testen bør dekke

- **Aksjonspunkt-produksjon:** Steget returnerer riktig aksjonspunkt
- **Automatisk løsning:** Steget løser seg selv når mulig (hvis relevant)
- **IKKE_RELEVANT-perioder:** Perioder avslått av andre vilkår settes til IKKE_RELEVANT (kun hvis IKKE_RELEVANT-håndtering er valgt)
- **Delvis avslått:** Perioder som kun er delvis avslått av andre vilkår skal fortsatt vurderes (kun hvis IKKE_RELEVANT-håndtering er valgt)
- **Grenseverdier:** Perioder, datoer og data som treffer kanttilfeller

**Referansetest:** `ytelse-aktivitetspenger/src/test/java/no/nav/ung/ytelse/aktivitetspenger/medlemskap/ForutgåendeMedlemskapsvilkårStegTest.java`

## Viktige mønstre

### CDI-oppdagelse
Alle steg og oppdaterere oppdages automatisk via CDI-annotasjoner. Ingen manuell registrering utover prosessmodellen er nødvendig.

### AksjonspunktType-hierarki
```
AksjonspunktType
├── MANUELL              — Sentral saksbehandler
├── LOKALKONTOR_MANUELL  — Lokalkontor-saksbehandler
├── AUTOPUNKT            — Automatisk (7xxx), setter behandling på vent
├── LOKALKONTOR_AUTOPUNKT
├── OVERSTYRING          — Overstyringsaksjonspunkt (6xxx)
├── LOKALKONTOR_OVERSTYRING
├── SAKSBEHANDLEROVERSTYRING
└── LOKALKONTOR_SAKSBEHANDLEROVERSTYRING
```

## Utenfor scope

Denne skillen dekker **ikke**:
- Vilkår-spesifikke deler (VilkårType, Avslagsårsak, VilkårUtleder) — bruk `new-vilkaar`-skillen
- Formidling/brev (formidling-pdfgen-templates, formidling-moduler)
- Frontend-endringer (ung-sak-web)
- Ungdomsprogramytelsen (annet prosessmønster)
