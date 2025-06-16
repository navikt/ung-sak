package no.nav.ung.kodeverk.vedtak;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Deprecated
public enum Vedtaksbrev implements Kodeverdi {

    AUTOMATISK("AUTOMATISK", "Automatisk generert vedtaksbrev"),
    @Deprecated
    FRITEKST("FRITEKST", "Fritekstbrev"),
    MANUELL("MANUELL", "Manuell vedtaksbrev"),
    INGEN("INGEN", "Ingen vedtaksbrev"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, Vedtaksbrev> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "VEDTAKSBREV";

    private String navn;

    private String kode;

    private Vedtaksbrev(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Vedtaksbrev fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Vedtaksbrev: for input " + kode);
        }
        return ad;
    }

    public static Map<String, Vedtaksbrev> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

}
