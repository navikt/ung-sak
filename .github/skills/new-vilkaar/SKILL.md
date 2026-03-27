---
name: new-vilkaar
description: Legg til eller endre vilkår for aktivitetspenger i ung-sak. USE FOR: opprette nytt vilkår, endre eksisterende vilkår, legge til/endre avslagsårsaker, endre vilkårstype, endre vilkårsvurdering i steg eller oppdaterer. DO NOT USE FOR: aksjonspunkt uten vilkår (bruk new-aksjonspunkt), formidling/brev, frontend.
---

# Nytt vilkår for Aktivitetspenger

Denne skillen beskriver vilkår-spesifikke tillegg når et aksjonspunkt skal knyttes til et vilkår.

**Forutsetning:** Følg først `new-aksjonspunkt`-skillen for å opprette aksjonspunktet, steget, DTO-en og oppdatereren. Deretter utvid med vilkår-stegene nedenfor.

## Arbeidsflyt

**Steg 0 — Samle inn vilkår-detaljer fra bruker**

Før du skriver kode, bruk `vscode_askQuestions` for å stille følgende spørsmål (i tillegg til spørsmålene fra `new-aksjonspunkt`-skillen). Ikke anta verdier — vent på svar.

```
Spørsmål å stille (bruk vscode_askQuestions):

1. header: "Vilkårnavn"
   question: "Hva skal vilkåret hete? (f.eks. 'Forutgående medlemskapsvilkåret')"

2. header: "Vilkårkode"
   question: "Hvilken kode skal vilkåret ha? (format AKT_VK_N, sjekk neste ledige i VilkårType.java)"

3. header: "Lovreferanse"
   question: "Hvilken lovreferanse gjelder for vilkåret? (f.eks. 'Forskrift om aktivitetspenger § X')"

4. header: "Avslagsårsaker"
   question: "Hvilke avslagsårsaker skal vilkåret ha? (oppgi navn og kode for hver, f.eks. 'SØKER_ER_IKKE_MEDLEM / 4001')"
```

Bruk svarene til å fylle inn konkrete verdier i alle steg under. Ikke bruk placeholder-navn.

## Steg-for-steg sjekkliste

### 1. Avslagsårsak

Fil: `kodeverk/src/main/java/no/nav/ung/kodeverk/vilkår/Avslagsårsak.java`

Legg til ny(e) enum-konstant(er):
```java
AVSLAGSÅRSAK_NAVN("KODE", "Beskrivelse",
    Map.of(FagsakYtelseType.AKTIVITETSPENGER, "Lovreferanse"),
    "VILKAR_TYPE"),
```

### 2. VilkårType

Fil: `kodeverk/src/main/java/no/nav/ung/kodeverk/vilkår/VilkårType.java`

Legg til ny enum-konstant:
```java
MITT_VILKÅR("AKT_VK_N", "Vilkårnavn",
    Map.of(FagsakYtelseType.AKTIVITETSPENGER, "Lovreferanse"),
    Avslagsårsak.MIN_AVSLAGSÅRSAK),
```

### 3. Oppdater AksjonspunktDefinisjon

Gå tilbake til `AksjonspunktDefinisjon`-enum-konstanten som ble opprettet i `new-aksjonspunkt`-skillen og erstatt `UTEN_VILKÅR` med `VilkårType.MITT_VILKÅR`.

### 4. Vilkårregistrering

Fil: `domenetjenester/perioder/src/main/java/no/nav/ung/sak/vilkår/AktivitetspengerInngangsvilkårUtleder.java`

Legg til den nye `VilkårType` i `YTELSE_VILKÅR`-listen:
```java
private static final List<VilkårType> YTELSE_VILKÅR = asList(
    ALDERSVILKÅR,
    BOSTEDSVILKÅR,
    FORUTGÅENDE_MEDLEMSKAPSVILKÅRET,
    MITT_VILKÅR   // <-- ny
);
```

### 5. Utvid steget med vilkårsvurdering

Steget som ble opprettet i `new-aksjonspunkt`-skillen må utvides til å vurdere vilkåret:

- Injiser `VilkårResultatRepository` og `Instance<VilkårsPerioderTilVurderingTjeneste>`
- I `utførSteg()`: sjekk om vilkåret allerede er vurdert (ikke `IKKE_VURDERT`), og hopp over i så fall
- Ved automatisk vurdering: oppdater vilkåret via `VilkårResultatBuilder`

**Referansefiler:**
- Enkel (alltid aksjonspunkt): `ytelse-aktivitetspenger/src/main/java/no/nav/ung/ytelse/aktivitetspenger/del1/steg/bistandsvilkår/BistandsvilkårSteg.java`
- Kompleks (auto + aksjonspunkt): `ytelse-aktivitetspenger/src/main/java/no/nav/ung/ytelse/aktivitetspenger/medlemskap/ForutgåendeMedlemskapsvilkårSteg.java`
- Auto-vurdert (aldersvilkår): `ytelse-aktivitetspenger/src/main/java/no/nav/ung/ytelse/aktivitetspenger/del1/steg/aldersvilkår/VurderAldersvilkåretSteg.java`

### 6. Utvid oppdatereren med vilkårsoppdatering

Oppdatereren som ble opprettet i `new-aksjonspunkt`-skillen må utvides til å oppdatere vilkåret basert på saksbehandlers beslutning:

```java
var resultatBuilder = param.getVilkårResultatBuilder();
var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.MITT_VILKÅR);

var perioderTilVurdering = perioderTilVurderingTjeneste.utled(param.getBehandlingId(), VilkårType.MITT_VILKÅR);

perioderTilVurdering.stream()
    .map(periode -> vilkårBuilder.hentBuilderFor(periode)
        .medUtfall(dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT)
        .medAvslagsårsak(avslagsårsak)
        .medRegelInput("..."))
    .forEach(vilkårBuilder::leggTil);

resultatBuilder.leggTil(vilkårBuilder);
```

**Referansefiler:**
- Enkel: `web/src/main/java/no/nav/ung/sak/web/app/tjenester/behandling/aktivitetspenger/VurderBehovForBistandOppdaterer.java`
- Med avslagsårsak: `web/src/main/java/no/nav/ung/sak/web/app/tjenester/behandling/aktivitetspenger/BekreftErMedlemVurderingOppdaterer.java`

### 7. Utvid test med vilkårsassertions

I testen som ble opprettet i `new-aksjonspunkt`-skillen, legg til vilkår i scenarioet og verifiser vilkårsutfall:

```java
var scenario = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
    .leggTilVilkår(VilkårType.MITT_VILKÅR, Utfall.IKKE_VURDERT)
    .lagre(entityManager);
```

**Ekstra testcaser for vilkår:**
- Vilkåret oppfylles automatisk når data tilsier det (hvis relevant)
- Vilkåret som allerede er vurdert hoppes over
- Riktig avslagsårsak settes ved avslag
- Perioder oppdateres korrekt

**Referansetest:** `ytelse-aktivitetspenger/src/test/java/no/nav/ung/ytelse/aktivitetspenger/medlemskap/ForutgåendeMedlemskapsvilkårStegTest.java`

## Viktige mønstre

### Vilkårsperioder
`VilkårsPerioderTilVurderingTjeneste` bestemmer hvilke perioder et vilkår skal vurderes for. Implementasjonen ligger i `AktivitetspengerVilkårsPerioderTilVurderingTjeneste`. Nye vilkår bruker samme periode-tjeneste.

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

### AktivitetspengerTestScenarioBuilder
Bruk **alltid** builderen for testscenarioer. Hvis vilkåret krever data som builderen ikke støtter, **utvid builderen** med nye metoder.

Fil: `behandlingslager/testutil/src/main/java/no/nav/ung/sak/test/util/behandling/aktivitetspenger/AktivitetspengerTestScenarioBuilder.java`

## Utenfor scope

Denne skillen dekker **ikke**:
- Aksjonspunkt, steg, DTO og oppdaterer uten vilkår — bruk `new-aksjonspunkt`-skillen
- Formidling/brev (formidling-pdfgen-templates, formidling-moduler)
- Frontend-endringer (ung-sak-web)
- Ungdomsprogramytelsen (annet vilkårsmønster)
- Flyway-migreringer (vilkår lagres dynamisk, ikke som faste DB-tabeller)
