package no.nav.ung.kodeverk.historikk;

/**
 * <p>
 * Definerer typer historikkinnslag for arbeidsforhold i 5080
 * </p>
 */

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum VurderArbeidsforholdHistorikkinnslag implements Kodeverdi {

    UDEFINERT("-", "UDEFINERT"),
    MANGLENDE_OPPLYSNINGER("MANGLENDE_OPPLYSNINGER", "Benytt i behandlingen, men har manglende opplysninger"),
    LAGT_TIL_AV_SAKSBEHANDLER("LAGT_TIL_AV_SAKSBEHANDLER", "Arbeidsforholdet er lagt til av saksbehandler beregningsgrunnlaget"),
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

    private String navn;

    private String kode;

    private VurderArbeidsforholdHistorikkinnslag(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static VurderArbeidsforholdHistorikkinnslag  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
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

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }


    @Override
    public String getOffisiellKode() {
        return getKode();
    }
}
