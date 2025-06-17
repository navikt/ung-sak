package no.nav.ung.kodeverk;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

public enum OmrådeTema implements Kodeverdi {

    UNG("UNG", "UNG", "Ungdomsytelse"), // Ungdomsytelse
    UDEFINERT("-", null, null),
    ;

    private static final Map<String, OmrådeTema> KODER = new LinkedHashMap<>();
    private static final Map<String, OmrådeTema> OFFISIELLE_KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "TEMA";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            if (v.offisiellKode != null) {
                OFFISIELLE_KODER.putIfAbsent(v.offisiellKode, v);
            }
        }
    }

    private String navn;
    private String kode;

    private String offisiellKode;

    private OmrådeTema(String kode, String offisiellKode, String navn) {
        this.kode = kode;
        this.offisiellKode = offisiellKode;
        this.navn = navn;
    }

    public static OmrådeTema fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Tema: " + kode);
        }
        return ad;
    }

    public static OmrådeTema fraKodeDefaultUdefinert(final String kode) {
        if (kode == null) {
            return UDEFINERT;
        }
        return KODER.getOrDefault(kode, UDEFINERT);
    }

    public static OmrådeTema fraOffisiellKode(String kode) {
        if (kode == null) {
            return UDEFINERT;
        }
        return OFFISIELLE_KODER.getOrDefault(kode, UDEFINERT);
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    public String getOffisiellKode() {
        return offisiellKode;
    }

}
