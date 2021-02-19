package no.nav.k9.kodeverk.historikk;

/**
 * <p>
 * Definerer typer historikkinnslag for arbeidsforhold i 5080
 * </p>
 */

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
public enum VurderArbeidsforholdHistorikkinnslag implements Kodeverdi {

    UDEFINERT("-", "UDEFINERT"),
    MANGLENDE_OPPLYSNINGER("MANGLENDE_OPPLYSNINGER", "Benytt i behandlingen, men har manglende opplysninger"),
    LAGT_TIL_AV_SAKSBEHANDLER("LAGT_TIL_AV_SAKSBEHANDLER", "Arbeidsforholdet er lagt til av saksbehandler beregningsgrunnlaget"),
    BRUK("BRUK", "Benytt uten endringen")
    ;

    private static final Map<String, VurderArbeidsforholdHistorikkinnslag> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "VURDER_ARBEIDSFORHOLD_HISTORIKKINNSLAG";

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

    private VurderArbeidsforholdHistorikkinnslag(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static VurderArbeidsforholdHistorikkinnslag  fraKode(Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(VurderArbeidsforholdHistorikkinnslag.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VurderArbeidsforholdHistorikkinnslag: " + kode);
        }
        return ad;
    }

    public static Map<String, VurderArbeidsforholdHistorikkinnslag> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
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
}
