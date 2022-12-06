package no.nav.k9.kodeverk.beregningsgrunnlag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

/**
 * Definerer aksjonspunkter i beregning.
 */
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BeregningAvklaringsbehovDefinisjon implements Kodeverdi {

    OVERSTYRING_AV_BEREGNINGSAKTIVITETER(
        BeregningAvklaringsbehovKodeDefinition.OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE,
        AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE, "Overstyring av beregningsaktiviteter"),
    OVERSTYRING_AV_BEREGNINGSGRUNNLAG(
        BeregningAvklaringsbehovKodeDefinition.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE,
        AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE, "Overstyring av beregningsgrunnlag"),
    VURDER_FAKTA_FOR_ATFL_SN(
        BeregningAvklaringsbehovKodeDefinition.VURDER_FAKTA_FOR_ATFL_SN_KODE,
        AksjonspunktKodeDefinisjon.VURDER_FAKTA_FOR_ATFL_SN_KODE, "Vurder fakta for arbeidstaker, frilans og selvstendig næringsdrivende"),
    AVKLAR_AKTIVITETER(BeregningAvklaringsbehovKodeDefinition.AVKLAR_AKTIVITETER_KODE,
        AksjonspunktKodeDefinisjon.AVKLAR_AKTIVITETER_KODE, "Avklar aktivitet for beregning"),
    AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST(
        BeregningAvklaringsbehovKodeDefinition.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST_KODE,
        AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST_KODE, "Vent på rapporteringsfrist for inntekt"),
    AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT(
        BeregningAvklaringsbehovKodeDefinition.AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT_KODE,
        AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT_KODE, "Vent på siste meldekort for AAP eller DP-mottaker"),
    AUTO_VENT_FRISINN(
        BeregningAvklaringsbehovKodeDefinition.AUTO_VENT_FRISINN_KODE,
        AksjonspunktKodeDefinisjon.AUTO_VENT_FRISINN_BEREGNING, "Vent på mangel i løsning: 36 måneder med ytelse"),
    FORDEL_BEREGNINGSGRUNNLAG(
        BeregningAvklaringsbehovKodeDefinition.FORDEL_BEREGNINGSGRUNNLAG_KODE,
        AksjonspunktKodeDefinisjon.FORDEL_BEREGNINGSGRUNNLAG_KODE, "Fordel beregningsgrunnlag"),
    FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS(
        BeregningAvklaringsbehovKodeDefinition.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE,
        AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE, "Fastsette beregningsgrunnlag for arbeidstaker/frilanser skjønnsmessig"),
    FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE(
        BeregningAvklaringsbehovKodeDefinition.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE_KODE,
        AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE_KODE, "Fastsett beregningsgrunnlag for selvstendig næringsdrivende"),
    FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET(
        BeregningAvklaringsbehovKodeDefinition.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE,
        AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE, "Fastsett beregningsgrunnlag for SN som er ny i arbeidslivet"),
    VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE(
        BeregningAvklaringsbehovKodeDefinition.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE,
        AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE, "Vurder varig endret/nyoppstartet næring selvstendig næringsdrivende"),
    VURDER_VARIG_ENDRET_ARBEIDSSITUASJON(
        BeregningAvklaringsbehovKodeDefinition.VURDER_VARIG_ENDRET_ARBEIDSSITUASJON_KODE,
        AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ARBEIDSSITUASJON_KODE, "Vurder varig endret arbeidssituasjon"),
    FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD(
        BeregningAvklaringsbehovKodeDefinition.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE,
        AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE, "Fastsett beregningsgrunnlag for tidsbegrenset arbeidsforhold"),
    VURDER_REFUSJON_BERGRUNN(
        BeregningAvklaringsbehovKodeDefinition.VURDER_REFUSJON_BERGRUNN_KODE,
        AksjonspunktKodeDefinisjon.VURDER_REFUSJON_BERGRUNN_KODE, "Vurder endring i refusjon refusjon"),
    VURDER_NYTT_INNTKTSFRHLD(
        BeregningAvklaringsbehovKodeDefinition.VURDER_NYTT_INNTKTSFRHLD_KODE,
        AksjonspunktKodeDefinisjon.VURDER_NYTT_INNTKTSFRHLD_KODE, "Vurder om tilkommet inntektsforhold skal redusere utbetaling"),
    VURDER_REPRESENTERER_STORTINGET(
        BeregningAvklaringsbehovKodeDefinition.VURDER_REPRESENTERER_STORTINGET_KODE,
        AksjonspunktKodeDefinisjon.VURDER_REPRESENTERER_STORTINGET_KODE, "Vurder om tilkommet inntektsforhold skal redusere utbetaling"),
    ;

    static final String KODEVERK = "BEREGNING_AVKLARINGSBEHOV_DEF";

    private static final Map<String, BeregningAvklaringsbehovDefinisjon> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String kode;

    private String internKode;

    @JsonIgnore
    private String navn;

    private BeregningAvklaringsbehovDefinisjon() {
        // for hibernate
    }

    private BeregningAvklaringsbehovDefinisjon(String kode, String internKode, String navn) {
        this.kode = Objects.requireNonNull(kode);
        this.internKode = internKode;
        this.navn = navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    public String getInternKode() {
        return internKode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static BeregningAvklaringsbehovDefinisjon fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(BeregningAvklaringsbehovDefinisjon.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BeregningAvklaringsbehovDefinisjon: " + kode);
        }
        return ad;
    }

    public static Map<String, BeregningAvklaringsbehovDefinisjon> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }
}
