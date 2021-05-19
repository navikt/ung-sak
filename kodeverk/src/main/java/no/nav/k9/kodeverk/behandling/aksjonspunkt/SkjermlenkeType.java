package no.nav.k9.kodeverk.behandling.aksjonspunkt;

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

import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum SkjermlenkeType implements Kodeverdi {
    // FIXME K9 rydd vekk overflødig
    BEREGNING("BEREGNING", "Beregning"),
    FAKTA_FOR_OPPTJENING("FAKTA_FOR_OPPTJENING", "Fakta om opptjening"),
    FAKTA_OM_ARBEIDSFORHOLD("FAKTA_OM_ARBEIDSFORHOLD", "Fakta om arbeidsforhold"),
    FAKTA_OM_BEREGNING("FAKTA_OM_BEREGNING", "Fakta om beregning"),
    FAKTA_OM_FORDELING("FAKTA_OM_FORDELING", "Fakta om fordeling"),
    FAKTA_OM_MEDLEMSKAP("FAKTA_OM_MEDLEMSKAP", "Fakta om medlemskap"),
    FAKTA_OM_MEDISINSK("FAKTA_OM_MEDISINSK", "Fakta om medisinsk"),
    FAKTA_OM_ÅRSKVANTUM("FAKTA_OM_ÅRSKVANTUM", "Fakta om årskvantum"),
    FAKTA_OM_UTVIDETRETT("FAKTA_OM_UTVIDETRETT", "Fakta om omsorgsdager utvidet rett"),
    FAKTA_OM_OMSORGENFOR("FAKTA_OM_OMSORGEN_FOR", "Fakta om omsorgen for"),
    FAKTA_OM_OPPTJENING("FAKTA_OM_OPPTJENING", "Fakta om opptjening"),
    FAKTA_OM_SIMULERING("FAKTA_OM_SIMULERING", "Simulering"),
    FAKTA_OM_UTTAK("FAKTA_OM_UTTAK", "Fakta om uttak"),
    FAKTA_OM_VERGE("FAKTA_OM_VERGE", "Fakta om verge/fullmektig"),
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
    TILSYN("TILSYN", "Tilsyn"),
    UTTAK("UTTAK", "Uttak"),
    VEDTAK("VEDTAK", "Vedtak"),
    VURDER_FARESIGNALER("VURDER_FARESIGNALER", "Vurder faresignaler"),
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
