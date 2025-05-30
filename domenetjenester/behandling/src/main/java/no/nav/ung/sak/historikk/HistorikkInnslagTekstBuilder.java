package no.nav.ung.sak.historikk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.felles.feil.Feil;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.ung.kodeverk.historikk.HistorikkAvklartSoeknadsperiodeType;
import no.nav.ung.kodeverk.historikk.HistorikkBegrunnelseType;
import no.nav.ung.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.ung.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.ung.kodeverk.historikk.HistorikkOpplysningType;
import no.nav.ung.kodeverk.historikk.HistorikkResultatType;
import no.nav.ung.kodeverk.historikk.HistorikkinnslagFeltType;
import no.nav.ung.kodeverk.historikk.HistorikkinnslagMal;
import no.nav.ung.kodeverk.historikk.HistorikkinnslagType;
import no.nav.ung.kodeverk.historikk.VurderArbeidsforholdHistorikkinnslag;
import no.nav.ung.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.ung.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagTekstBuilderFormater;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagTotrinnsvurdering;

public class HistorikkInnslagTekstBuilder {

    /** Kodeverdi mappinger støttet i historikk. */
    public static final Map<String, Map<String, ? extends Kodeverdi>> KODEVERK_KODEVERDI_MAP = Map.ofEntries(
        new SimpleEntry<>(Venteårsak.KODEVERK, Venteårsak.kodeMap()),
        new SimpleEntry<>(OppgaveÅrsak.KODEVERK, OppgaveÅrsak.kodeMap()),

        new SimpleEntry<>(BehandlingÅrsakType.KODEVERK, BehandlingÅrsakType.kodeMap()),
        new SimpleEntry<>(BehandlingResultatType.KODEVERK, BehandlingResultatType.kodeMap()),

        new SimpleEntry<>(HistorikkAvklartSoeknadsperiodeType.KODEVERK, HistorikkAvklartSoeknadsperiodeType.kodeMap()),
        new SimpleEntry<>(HistorikkBegrunnelseType.KODEVERK, HistorikkBegrunnelseType.kodeMap()),
        new SimpleEntry<>(HistorikkEndretFeltType.KODEVERK, HistorikkEndretFeltType.kodeMap()),
        new SimpleEntry<>(HistorikkEndretFeltVerdiType.KODEVERK, HistorikkEndretFeltVerdiType.kodeMap()),
        new SimpleEntry<>(HistorikkinnslagType.KODEVERK, HistorikkinnslagType.kodeMap()),
        new SimpleEntry<>(HistorikkOpplysningType.KODEVERK, HistorikkOpplysningType.kodeMap()),
        new SimpleEntry<>(HistorikkResultatType.KODEVERK, HistorikkResultatType.kodeMap()),

        new SimpleEntry<>(SkjermlenkeType.KODEVERK, SkjermlenkeType.kodeMap()),

        new SimpleEntry<>(VedtakResultatType.KODEVERK, VedtakResultatType.kodeMap()),
        new SimpleEntry<>(Utfall.KODEVERK, Utfall.kodeMap()),

        // ulike domenespesifikke kodeverk som tillates

        // Domene : Medlemskap
        new SimpleEntry<>(MedlemskapManuellVurderingType.KODEVERK, MedlemskapManuellVurderingType.kodeMap()),

        // Domene : arbeid og beregningsgrunnlag
        new SimpleEntry<>(Inntektskategori.KODEVERK, Inntektskategori.kodeMap()),
        new SimpleEntry<>(VurderArbeidsforholdHistorikkinnslag.KODEVERK, VurderArbeidsforholdHistorikkinnslag.kodeMap()),
        new SimpleEntry<>(Vurdering.KODEVERK, Vurdering.kodeMap()),

        // Domene : Tilbakekreving
        new SimpleEntry<>(TilbakekrevingVidereBehandling.KODEVERK, TilbakekrevingVidereBehandling.kodeMap()));

    private boolean begrunnelseEndret = false;
    private boolean gjeldendeFraSatt = false;

    private HistorikkinnslagDel.Builder historikkinnslagDelBuilder = HistorikkinnslagDel.builder();
    private List<HistorikkinnslagDel> historikkinnslagDeler = new ArrayList<>();
    private int antallEndredeFelter = 0;
    private int antallAksjonspunkter = 0;
    private int antallOpplysninger = 0;

    public HistorikkInnslagTekstBuilder() {
    }

    public List<HistorikkinnslagDel> getHistorikkinnslagDeler() {
        return historikkinnslagDeler;
    }

    public HistorikkInnslagTekstBuilder medHendelse(HistorikkinnslagType historikkInnslagsType) {
        return medHendelse(historikkInnslagsType, null);
    }

    public HistorikkInnslagTekstBuilder medHendelse(HistorikkinnslagType historikkinnslagType, Object verdi) {
        if (!HistorikkinnslagType.FAKTA_ENDRET.equals(historikkinnslagType)
            && !HistorikkinnslagType.OVERSTYRT.equals(historikkinnslagType)
            && !HistorikkinnslagType.OPPTJENING.equals(historikkinnslagType)) { // PKMANTIS-753 FPFEIL-805
            String verdiStr = formatString(verdi);
            HistorikkinnslagFelt.builder()
                .medFeltType(HistorikkinnslagFeltType.HENDELSE)
                .medNavn(validerKodeverdi(historikkinnslagType))
                .medTilVerdi(verdiStr)
                .build(historikkinnslagDelBuilder);
        }
        return this;
    }

    public boolean erSkjermlenkeSatt() {
        return getHistorikkinnslagDeler().stream()
            .anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
    }

    public HistorikkInnslagTekstBuilder medSkjermlenke(SkjermlenkeType skjermlenkeType) {
        if (skjermlenkeType == null || SkjermlenkeType.UDEFINERT.equals(skjermlenkeType)) {
            return this;
        }
        validerKodeverdi(skjermlenkeType);
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.SKJERMLENKE)
            .medTilVerdi(validerKodeverdi(skjermlenkeType))
            .build(historikkinnslagDelBuilder);
        return this;
    }

    private Kodeverdi validerKodeverdi(Kodeverdi kodeverdi) {
        // validerer all input til HistorikkinnslagFelt#medTilVerdi(Kodeverdi).
        // ikke helt ideelt å ha validering utenfor HistorikkinnslagFelt, men nødvendig da Kodeverdi kan stamme fra andre plasser
        // Hvis det ikke valideres er det en mulighet for at det smeller i HistorikkinnslagDto ved mapping til GUI.
        if (!KODEVERK_KODEVERDI_MAP.containsKey(kodeverdi.getKodeverk())) {
            throw new IllegalStateException("Har ikke støtte for kodeverk :" + kodeverdi.getKodeverk() + " for Kodeverdi " + kodeverdi);
        }
        return kodeverdi;
    }

    public HistorikkInnslagTekstBuilder medNavnOgGjeldendeFra(HistorikkEndretFeltType endretFelt, String navnVerdi, LocalDate gjeldendeFraDato) {
        if (gjeldendeFraDato != null) {
            gjeldendeFraSatt = true;
        }
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.GJELDENDE_FRA)
            .medNavn(validerKodeverdi(endretFelt))
            .medNavnVerdi(navnVerdi)
            .medTilVerdi(formatString(gjeldendeFraDato))
            .build(historikkinnslagDelBuilder);
        return this;
    }

    public HistorikkInnslagTekstBuilder medAvklartSøknadperiode(HistorikkAvklartSoeknadsperiodeType endretFeltType, String verdi) {
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.AVKLART_SOEKNADSPERIODE)
            .medNavn(validerKodeverdi(endretFeltType))
            .medTilVerdi(verdi)
            .build(historikkinnslagDelBuilder);
        return this;
    }

    public HistorikkInnslagTekstBuilder medNavnVerdiOgAvklartSøknadperiode(HistorikkAvklartSoeknadsperiodeType endretFeltType, String navnVerdi, String verdi) {
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.AVKLART_SOEKNADSPERIODE)
            .medNavn(endretFeltType)
            .medNavnVerdi(navnVerdi)
            .medTilVerdi(verdi)
            .build(historikkinnslagDelBuilder);
        return this;
    }

    public HistorikkInnslagTekstBuilder medÅrsak(Venteårsak årsak) {
        return medÅrsakIntern(årsak);
    }

    public HistorikkInnslagTekstBuilder medÅrsak(BehandlingResultatType årsak) {
        return medÅrsakIntern(årsak);
    }

    private <K extends Kodeverdi> HistorikkInnslagTekstBuilder medÅrsakIntern(K årsak) {
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.AARSAK)
            .medTilVerdi(validerKodeverdi(årsak))
            .build(historikkinnslagDelBuilder);
        return this;
    }

    public HistorikkInnslagTekstBuilder medTema(HistorikkEndretFeltType endretFeltType, String verdi) {
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.ANGÅR_TEMA)
            .medNavn(validerKodeverdi(endretFeltType))
            .medNavnVerdi(verdi)
            .build(historikkinnslagDelBuilder);
        return this;
    }

    public HistorikkInnslagTekstBuilder medResultat(HistorikkResultatType resultat) {
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.RESULTAT)
            .medTilVerdi(validerKodeverdi(resultat))
            .build(historikkinnslagDelBuilder);
        return this;
    }

    public HistorikkInnslagTekstBuilder medResultat(VedtakResultatType resultat) {
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.RESULTAT)
            .medTilVerdi(validerKodeverdi(resultat))
            .build(historikkinnslagDelBuilder);
        return this;
    }

    public HistorikkInnslagTekstBuilder medBegrunnelse(LocalDateInterval begrunnelse) {
        return medBegrunnelse(formatString(begrunnelse), true);
    }

    public HistorikkInnslagTekstBuilder medBegrunnelse(LocalDate begrunnelse) {
        return medBegrunnelse(formatString(begrunnelse), true);
    }

    public HistorikkInnslagTekstBuilder medBegrunnelse(Kodeverdi begrunnelse) {
        return medBegrunnelse(begrunnelse, true);
    }

    public HistorikkInnslagTekstBuilder medBegrunnelse(String begrunnelse) {
        String begrunnelseStr = formatString(begrunnelse);
        return medBegrunnelse(begrunnelseStr, true);
    }

    public HistorikkInnslagTekstBuilder medBegrunnelse(String begrunnelse, boolean erBegrunnelseEndret) {
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.BEGRUNNELSE)
            .medTilVerdi(begrunnelse)
            .build(historikkinnslagDelBuilder);
        this.begrunnelseEndret = erBegrunnelseEndret;
        return this;
    }

    public <K extends Kodeverdi> HistorikkInnslagTekstBuilder medBegrunnelse(K begrunnelse, boolean erBegrunnelseEndret) {
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.BEGRUNNELSE)
            .medTilVerdi(validerKodeverdi(begrunnelse))
            .build(historikkinnslagDelBuilder);
        this.begrunnelseEndret = erBegrunnelseEndret;
        return this;
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, String navnVerdi, Integer fraVerdi, Integer tilVerdi) {
        if (Objects.equals(fraVerdi, tilVerdi)) {
            return this;
        }
        return medEndretFelt(historikkEndretFeltType, navnVerdi, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, String navnVerdi, Number fraVerdi, Number tilVerdi) {
        if (Objects.equals(fraVerdi, tilVerdi)) {
            return this;
        }
        return medEndretFelt(historikkEndretFeltType, navnVerdi, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, String navnVerdi, Boolean fraVerdi, Boolean tilVerdi) {
        return medEndretFelt(historikkEndretFeltType, navnVerdi, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, String navnVerdi, String fraVerdi, String tilVerdi) {
        String fraVerdiStr = formatString(fraVerdi);
        String tilVerdiStr = formatString(tilVerdi);

        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.ENDRET_FELT)
            .medNavn(validerKodeverdi(historikkEndretFeltType))
            .medNavnVerdi(navnVerdi)
            .medFraVerdi(fraVerdiStr)
            .medTilVerdi(tilVerdiStr)
            .medSekvensNr(getNesteEndredeFeltSekvensNr())
            .build(historikkinnslagDelBuilder);
        return this;
    }

    public <K extends Kodeverdi> HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, K fraVerdi, K tilVerdi) {
        if (Objects.equals(fraVerdi, tilVerdi)) {
            return this;
        }
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.ENDRET_FELT)
            .medNavn(validerKodeverdi(historikkEndretFeltType))
            .medFraVerdi(fraVerdi)
            .medTilVerdi(validerKodeverdi(tilVerdi))
            .medSekvensNr(getNesteEndredeFeltSekvensNr())
            .build(historikkinnslagDelBuilder);
        return this;
    }

    public <K extends Kodeverdi> HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType,
                                                                            String navnVerdi,
                                                                            K fraVerdi,
                                                                            K tilVerdi) {
        if (Objects.equals(fraVerdi, tilVerdi)) {
            return this;
        }
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.ENDRET_FELT)
            .medNavn(validerKodeverdi(historikkEndretFeltType))
            .medNavnVerdi(navnVerdi)
            .medFraVerdi(fraVerdi)
            .medTilVerdi(validerKodeverdi(tilVerdi))
            .medSekvensNr(getNesteEndredeFeltSekvensNr())
            .build(historikkinnslagDelBuilder);
        return this;
    }

    public  HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType,
                                                                            String navnVerdi,
                                                                            LocalDate fraVerdi,
                                                                            LocalDate tilVerdi) {
        return medEndretFelt(historikkEndretFeltType, navnVerdi, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, Boolean fraVerdi, Boolean tilVerdi) {
        return medEndretFelt(historikkEndretFeltType, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType type, Number fraVerdi, Number tilVerdi) {
        return medEndretFelt(type, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType type, LocalDateInterval fraVerdi, LocalDateInterval tilVerdi) {
        return medEndretFelt(type, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType type, LocalDate fraVerdi, LocalDate tilVerdi) {
        return medEndretFelt(type, formatString(fraVerdi), formatString(tilVerdi));
    }

    public HistorikkInnslagTekstBuilder medEndretFelt(HistorikkEndretFeltType historikkEndretFeltType, String fraVerdi, String tilVerdi) {
        if (Objects.equals(fraVerdi, tilVerdi)) {
            return this;
        }
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.ENDRET_FELT)
            .medNavn(validerKodeverdi(historikkEndretFeltType))
            .medFraVerdi(fraVerdi)
            .medTilVerdi(tilVerdi)
            .medSekvensNr(getNesteEndredeFeltSekvensNr())
            .build(historikkinnslagDelBuilder);
        return this;
    }

    public <T> HistorikkInnslagTekstBuilder medOpplysning(HistorikkOpplysningType opplysningType, T verdi) {
        String tilVerdi = formatString(verdi);
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.OPPLYSNINGER)
            .medNavn(validerKodeverdi(opplysningType))
            .medTilVerdi(tilVerdi)
            .medSekvensNr(hentNesteOpplysningSekvensNr())
            .build(historikkinnslagDelBuilder);
        return this;
    }

    public static String formatString(Object verdi) {
        return HistorikkinnslagTekstBuilderFormater.formatString(verdi);
    }

    public HistorikkInnslagTekstBuilder medTotrinnsvurdering(Map<SkjermlenkeType, List<HistorikkinnslagTotrinnsvurdering>> vurdering,
                                                             List<HistorikkinnslagTotrinnsvurdering> vurderingUtenVilkar) {
        boolean første = true;
        for (HistorikkinnslagTotrinnsvurdering totrinnsVurdering : vurderingUtenVilkar) {
            if (første) {
                første = false;
            } else {
                ferdigstillHistorikkinnslagDel();
            }
            leggTilTotrinnsvurdering(totrinnsVurdering);
        }

        List<Map.Entry<SkjermlenkeType, List<HistorikkinnslagTotrinnsvurdering>>> sortedList = vurdering.entrySet().stream()
            .sorted(getHistorikkDelComparator()).collect(Collectors.toList());

        for (Map.Entry<SkjermlenkeType, List<HistorikkinnslagTotrinnsvurdering>> lenkeVurdering : sortedList) {
            if (første) {
                første = false;
            } else {
                ferdigstillHistorikkinnslagDel();
            }
            SkjermlenkeType skjermlenkeType = lenkeVurdering.getKey();
            List<HistorikkinnslagTotrinnsvurdering> totrinnsVurderinger = lenkeVurdering.getValue();
            totrinnsVurderinger.sort(Comparator.comparing(HistorikkinnslagTotrinnsvurdering::getAksjonspunktSistEndret));
            medSkjermlenke(skjermlenkeType);
            totrinnsVurderinger.forEach(this::leggTilTotrinnsvurdering);
        }
        return this;
    }

    public int antallEndredeFelter() {
        return antallEndredeFelter;
    }

    /**
     * Returnerer om begrunnelse er endret.
     */
    public boolean getErBegrunnelseEndret() {
        return begrunnelseEndret;
    }

    /**
     * Returnerer om gjeldendeFra er satt.
     */
    public boolean getErGjeldendeFraSatt() {
        return gjeldendeFraSatt;
    }

    public HistorikkInnslagTekstBuilder ferdigstillHistorikkinnslagDel() {
        if (!historikkinnslagDelBuilder.harFelt()) {
            return this;
        }
        historikkinnslagDeler.add(historikkinnslagDelBuilder.build());
        historikkinnslagDelBuilder = HistorikkinnslagDel.builder();
        antallEndredeFelter = 0;
        antallAksjonspunkter = 0;
        antallOpplysninger = 0;
        begrunnelseEndret = false;
        return this;
    }

    public List<HistorikkinnslagDel> build(Historikkinnslag historikkinnslag) {
        ferdigstillHistorikkinnslagDel();
        verify(historikkinnslag.getType());
        historikkinnslag.setHistorikkinnslagDeler(historikkinnslagDeler);
        return historikkinnslagDeler;
    }

    private int getNesteEndredeFeltSekvensNr() {
        int neste = antallEndredeFelter;
        antallEndredeFelter++;
        return neste;
    }

    private int hentNesteOpplysningSekvensNr() {
        int sekvensNr = antallOpplysninger;
        antallOpplysninger++;
        return sekvensNr;
    }

    private Comparator<Map.Entry<SkjermlenkeType, List<HistorikkinnslagTotrinnsvurdering>>> getHistorikkDelComparator() {
        return (o1, o2) -> {
            List<HistorikkinnslagTotrinnsvurdering> totrinnsvurderinger1 = o1.getValue();
            List<HistorikkinnslagTotrinnsvurdering> totrinnsvurderinger2 = o2.getValue();
            totrinnsvurderinger1.sort(Comparator.comparing(HistorikkinnslagTotrinnsvurdering::getAksjonspunktSistEndret));
            totrinnsvurderinger2.sort(Comparator.comparing(HistorikkinnslagTotrinnsvurdering::getAksjonspunktSistEndret));
            LocalDateTime date1 = totrinnsvurderinger1.get(0).getAksjonspunktSistEndret();
            LocalDateTime date2 = totrinnsvurderinger2.get(0).getAksjonspunktSistEndret();
            if (date1 == null || date2 == null) {
                return -1;
            }
            return date1.isAfter(date2) ? 1 : -1;
        };
    }

    private HistorikkInnslagTekstBuilder leggTilTotrinnsvurdering(HistorikkinnslagTotrinnsvurdering totrinnsvurdering) {
        int sekvensNr = getNesteAksjonspunktSekvensNr();
        leggTilFelt(HistorikkinnslagFeltType.AKSJONSPUNKT_BEGRUNNELSE, totrinnsvurdering.getBegrunnelse(), sekvensNr);
        leggTilFelt(HistorikkinnslagFeltType.AKSJONSPUNKT_GODKJENT, totrinnsvurdering.erGodkjent(), sekvensNr);
        leggTilFelt(HistorikkinnslagFeltType.AKSJONSPUNKT_KODE, totrinnsvurdering.getAksjonspunktDefinisjon().getKode(), sekvensNr);
        return this;
    }

    private <T> void leggTilFelt(HistorikkinnslagFeltType feltType, T verdi, int sekvensNr) {
        HistorikkinnslagFelt.builder()
            .medFeltType(feltType)
            .medTilVerdi(verdi != null ? verdi.toString() : null)
            .medSekvensNr(sekvensNr)
            .build(historikkinnslagDelBuilder);
    }

    private int getNesteAksjonspunktSekvensNr() {
        int sekvensNr = antallAksjonspunkter;
        antallAksjonspunkter++;
        return sekvensNr;
    }

    /**
     * Sjekker at alle påkrevde felter for gitt historikkinnslagstype er angitt
     *
     * @param historikkinnslagType
     */
    private void verify(HistorikkinnslagType historikkinnslagType) {
        List<Feil> verificationResults = new ArrayList<>();
        historikkinnslagDeler.forEach(del -> {
            Optional<Feil> exception = verify(historikkinnslagType, del);
            exception.ifPresent(verificationResults::add);
        });
        // kast feil dersom alle deler feiler valideringen
        if (verificationResults.size() == historikkinnslagDeler.size()) {
            throw verificationResults.get(0).toException();
        }
    }

    private Optional<Feil> verify(HistorikkinnslagType historikkinnslagType, HistorikkinnslagDel historikkinnslagDel) {
        String type = historikkinnslagType.getMal();

        if (HistorikkinnslagMal.MAL_TYPE_1.equals(type)) {
            return checkFieldsPresent(type, historikkinnslagDel, HistorikkinnslagFeltType.HENDELSE);
        }
        if (HistorikkinnslagMal.MAL_TYPE_2.equals(type)) {
            return checkFieldsPresent(type, historikkinnslagDel, HistorikkinnslagFeltType.HENDELSE, HistorikkinnslagFeltType.SKJERMLENKE);
        }
        if (HistorikkinnslagMal.MAL_TYPE_3.equals(type)) {
            return checkFieldsPresent(type, historikkinnslagDel, HistorikkinnslagFeltType.HENDELSE, HistorikkinnslagFeltType.AKSJONSPUNKT_KODE);
        }
        if (HistorikkinnslagMal.MAL_TYPE_4.equals(type)) {
            return checkFieldsPresent(type, historikkinnslagDel, HistorikkinnslagFeltType.HENDELSE);
        }
        if (HistorikkinnslagMal.MAL_TYPE_5.equals(type) || HistorikkinnslagMal.MAL_TYPE_7.equals(type) || HistorikkinnslagMal.MAL_TYPE_8.equals(type)
            || HistorikkinnslagMal.MAL_TYPE_10.equals(type)) {
            return checkAtLeastOnePresent(type, historikkinnslagDel, HistorikkinnslagFeltType.SKJERMLENKE,
                HistorikkinnslagFeltType.HENDELSE,
                HistorikkinnslagFeltType.ENDRET_FELT,
                HistorikkinnslagFeltType.BEGRUNNELSE);
        }
        if (HistorikkinnslagMal.MAL_TYPE_6.equals(type)) {
            return checkFieldsPresent(type, historikkinnslagDel, HistorikkinnslagFeltType.OPPLYSNINGER);
        }
        if (HistorikkinnslagMal.MAL_TYPE_9.equals(type)) {
            return checkFieldsPresent(type, historikkinnslagDel, HistorikkinnslagFeltType.HENDELSE, HistorikkinnslagFeltType.ENDRET_FELT);
        }
        throw HistorikkInnsalgFeil.FACTORY.ukjentHistorikkinnslagType(type).toException();
    }

    private Optional<Feil> checkFieldsPresent(String type, HistorikkinnslagDel del, HistorikkinnslagFeltType... fields) {
        List<HistorikkinnslagFeltType> fieldList = Arrays.asList(fields);
        Set<HistorikkinnslagFeltType> harFelt = findFields(del, fieldList).collect(Collectors.toCollection(LinkedHashSet::new));

        // harFelt skal inneholde alle de samme feltene som fieldList
        if (harFelt.size() == fields.length) {
            return Optional.empty();
        } else {
            List<String> feltKoder = fieldList.stream().map(HistorikkinnslagFeltType::getKode).collect(Collectors.toList());
            return Optional.of(HistorikkInnsalgFeil.FACTORY.manglerFeltForHistorikkInnslag(type, feltKoder));
        }
    }

    private Optional<Feil> checkAtLeastOnePresent(String type, HistorikkinnslagDel del, HistorikkinnslagFeltType... fields) {
        List<HistorikkinnslagFeltType> fieldList = Arrays.asList(fields);
        Optional<HistorikkinnslagFeltType> opt = findFields(del, fieldList).findAny();

        if (opt.isPresent()) {
            return Optional.empty();
        } else {
            Set<String> eksisterendeKoder = del.getHistorikkinnslagFelt().stream()
                .map(HistorikkinnslagFelt::getFeltType)
                .map(HistorikkinnslagFeltType::getKode).collect(Collectors.toSet());
            List<String> feltKoder = fieldList.stream().map(HistorikkinnslagFeltType::getKode).collect(Collectors.toList());
            return Optional.of(HistorikkInnsalgFeil.FACTORY.manglerMinstEtFeltForHistorikkinnslag(type, feltKoder, eksisterendeKoder));
        }
    }

    private Stream<HistorikkinnslagFeltType> findFields(HistorikkinnslagDel del, List<HistorikkinnslagFeltType> fieldList) {
        return del.getHistorikkinnslagFelt().stream().map(HistorikkinnslagFelt::getFeltType).filter(fieldList::contains);
    }

    /** Tar med felt selv om ikke verdi er endret. */
    public HistorikkInnslagTekstBuilder medEndretFeltBegrunnelse(HistorikkEndretFeltType historikkEndretFeltType, String fraVerdi, String tilVerdi) {
        if(!begrunnelseEndret && Objects.equals(fraVerdi, tilVerdi)) {
            return this;
        }
        HistorikkinnslagFelt.builder()
            .medFeltType(HistorikkinnslagFeltType.ENDRET_FELT)
            .medNavn(validerKodeverdi(historikkEndretFeltType))
            .medFraVerdi(fraVerdi)
            .medTilVerdi(tilVerdi)
            .medSekvensNr(getNesteEndredeFeltSekvensNr())
            .build(historikkinnslagDelBuilder);
        return this;
    }

    /*
     * https://confluence.adeo.no/display/MODNAV/OMR-13+SF4+Sakshistorikk+-+UX+og+grafisk+design
     *
     * Fem design patterns:
     *
     * +----------------------------+
     * | Type 1 |
     * | BEH_VENT |
     * | BEH_GJEN |
     * | BEH_STARTET |
     * | VEDLEGG_MOTTATT |
     * | BREV_SENT |
     * | REGISTRER_PAPIRSØK |
     * +----------------------------+
     * <tidspunkt> // <rolle> <id>
     * <hendelse>
     * <OPTIONAL begrunnelsestekst>
     *
     *
     * +----------------------------+
     * | Type 2 |
     * | FORSLAG_VEDTAK |
     * | VEDTAK_FATTET |
     * | OVERSTYRT (hvis beslutter) |
     * | UENDRET UTFALL |
     * +----------------------------+
     * <tidspunkt> // <rolle> <id>
     * <hendelse>: <resultat>
     * <skjermlinke>
     * <OPTIONAL totrinnskontroll>
     *
     *
     * +----------------------------+
     * | Type 3 |
     * | SAK_RETUR |
     * +----------------------------+
     * <tidspunkt> // <rolle> <id>
     * <hendelse>
     * <totrinnsvurdering> med <skjermlinke> til vilkåret og liste med <aksjonspunkter>
     *
     *
     * +----------------------------+
     * | Type 4 |
     * | AVBRUTT_BEH |
     * | OVERSTYRT (hvis saksbeh.) |
     * +----------------------------+
     * <tidspunkt> // <rolle> <id>
     * <hendelse>
     * <årsak>
     * <begrunnelsestekst>
     *
     *
     * +----------------------------+
     * | Type 5 |
     * | FAKTA_ENDRET |
     * +----------------------------+
     * <tidspunkt> // <rolle> <id>
     * <skjermlinke>
     * <feltnavn> er endret <fra-verdi> til <til-verdi>
     * <radiogruppe> er satt til <verdi>
     * <begrunnelsestekst>
     *
     */

}
