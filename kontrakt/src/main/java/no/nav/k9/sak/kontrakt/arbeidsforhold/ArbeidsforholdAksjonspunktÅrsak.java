package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum ArbeidsforholdAksjonspunktÅrsak implements Kodeverdi {

    MANGLENDE_INNTEKTSMELDING("MANGLENDE_INNTEKTSMELDING"),
    INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD("INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD"),
    OVERGANG_ARBEIDSFORHOLDS_ID_UNDER_YTELSE("OVERGANG_ARBEIDSFORHOLDS_ID_UNDER_YTELSE");

    public static final String KODEVERK = "ARBEIDSFORHOLD_AKSJONSPUNKT_ÅRSAKER";
    private static final Map<String, ArbeidsforholdAksjonspunktÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String kode;

    ArbeidsforholdAksjonspunktÅrsak(String kode) {
        this.kode = kode;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ArbeidsforholdAksjonspunktÅrsak fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(ArbeidsforholdAksjonspunktÅrsak.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ArbeidsforholdAksjonspunktÅrsak: " + kode);
        }
        return ad;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return kode;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return null;
    }
}
