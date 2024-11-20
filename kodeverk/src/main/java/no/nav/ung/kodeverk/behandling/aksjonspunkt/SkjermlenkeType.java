package no.nav.ung.kodeverk.behandling.aksjonspunkt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum SkjermlenkeType implements Kodeverdi {
    // FIXME K9 rydd vekk overflødig
    BEREGNING("BEREGNING", "Beregning"),
    FAKTA_FOR_OPPTJENING("FAKTA_FOR_OPPTJENING", "Opptjening"),
    FAKTA_OM_ARBEIDSFORHOLD("FAKTA_OM_ARBEIDSFORHOLD", "Arbeidsforhold"),
    FAKTA_OM_BEREGNING("FAKTA_OM_BEREGNING", "Beregning"),
    INFOTRYGD_MIGRERING("INFOTRYGD_MIGRERING", "Infotrygdmigrering"),
    OVERSTYR_INPUT_BEREGNING("OVERSTYR_INPUT_BEREGNING", "Overstyr input beregning"),
    FAKTA_OM_FORDELING("FAKTA_OM_FORDELING", "Fordeling"),
    FAKTA_OM_MEDLEMSKAP("FAKTA_OM_MEDLEMSKAP", "Medlemskap"),
    FAKTA_OM_MEDISINSK("FAKTA_OM_MEDISINSK", "Sykdom"),
    FAKTA_OM_ÅRSKVANTUM("FAKTA_OM_ÅRSKVANTUM", "Årskvantum"),
    PUNKT_FOR_ALDERSVILKÅR_BARN("FAKTA_OM_ALDERSVILKÅR_BARN", "Aldersvilkår barn"),
    FAKTA_OM_UTVIDETRETT("FAKTA_OM_UTVIDETRETT", "Omsorgsdager"),
    FAKTA_OM_OMSORGENFOR("FAKTA_OM_OMSORGEN_FOR", "Omsorgen for"),
    FAKTA_OM_OPPTJENING("FAKTA_OM_OPPTJENING", "Opptjening"),
    FAKTA_OM_INNTEKTSMELDING("FAKTA_OM_INNTEKTSMELDING", "Inntektsmelding"),
    FAKTA_OM_SIMULERING("FAKTA_OM_SIMULERING", "Simulering"),
    FAKTA_OM_UTTAK("FAKTA_OM_UTTAK", "Uttak"),
    FAKTA_OM_VERGE("FAKTA_OM_VERGE", "Verge/fullmektig/søker under 18 år"),
    KONTROLL_AV_SAKSOPPLYSNINGER("KONTROLL_AV_SAKSOPPLYSNINGER", "Kontroll av saksopplysninger"),
    OPPLYSNINGSPLIKT("OPPLYSNINGSPLIKT", "Opplysningsplikt"),
    PUNKT_FOR_MEDLEMSKAP("PUNKT_FOR_MEDLEMSKAP", "Medlemskap"),
    PUNKT_FOR_MEDISINSK("PUNKT_FOR_MEDISINSK", "Medisinsk"),
    PUNKT_FOR_OMSORGEN_FOR("PUNKT_FOR_OMSORGEN_FOR", "Omsorgen for"),
    PUNKT_FOR_MEDLEMSKAP_LØPENDE("PUNKT_FOR_MEDLEMSKAP_LØPENDE", "Punkt for medlemskap løpende"),
    PUNKT_FOR_OPPTJENING("PUNKT_FOR_OPPTJENING", "Opptjening"),
    PUNKT_FOR_MAN_VILKÅRSVURDERING("PUNKT_FOR_MAN_VILKÅRSVURDERING", "Punkt for manuell vilkårsvurdering"),
    PUNKT_FOR_UTVIDETRETT("PUNKT_FOR_UTVIDETRETT", "Utvidet rett"),
    SOEKNADSFRIST("SOEKNADSFRIST", "Søknadsfrist"),
    TILKJENT_YTELSE("TILKJENT_YTELSE", "Tilkjent ytelse"),
    UDEFINERT("-", "Ikke definert"),
    UTLAND("UTLAND", "Endret utland"),
    UTTAK("UTTAK", "Uttak"),
    VEDTAK("VEDTAK", "Vedtak"),
    /** @deprecated pt. ikke i bruk i K9 */
    @Deprecated
    VURDER_FARESIGNALER("VURDER_FARESIGNALER", "Vurder faresignaler"),
    VURDER_NATTEVÅK("VURDER_NATTEVÅK", "Vurder nattevåk"),
    VURDER_BEREDSKAP("VURDER_BEREDSKAP", "Vurder beredskap"),
    VURDER_RETT_ETTER_PLEIETRENGENDES_DØD("VURDER_RETT_ETTER_PLEIETRENGENDES_DØD", "Vurder rett etter pleietrengendes død")
    ;

    private static final Map<String, SkjermlenkeType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "SKJERMLENKE_TYPE"; //$NON-NLS-1$



    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    private String kode;

    private SkjermlenkeType(String kode) {
        this.kode = kode;
    }

    private SkjermlenkeType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    /**
     * toString is set to output the kode value of the enum instead of the default that is the enum name.
     * This makes the generated openapi spec correct when the enum is used as a query param. Without this the generated
     * spec incorrectly specifies that it is the enum name string that should be used as input.
     */
    @Override
    public String toString() {
        return this.getKode();
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static SkjermlenkeType fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(SkjermlenkeType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent SkjermlenkeType: " + kode);
        }
        return ad;
    }

    public static Map<String, SkjermlenkeType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @JsonProperty
    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    /**
     * Returnerer skjermlenketype for eit aksjonspunkt.
     * @deprecated Brukes kun i totrinnskontroll og foreslå vedtak, bør også fjernes derfra og heller lagres på Aksjonspunktet (ikke definisjonen)
     */
    @Deprecated
    public static SkjermlenkeType finnSkjermlenkeType(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return aksjonspunktDefinisjon.getSkjermlenkeType();
    }

}