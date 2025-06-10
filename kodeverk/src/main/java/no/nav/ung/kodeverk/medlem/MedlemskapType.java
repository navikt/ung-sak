package no.nav.ung.kodeverk.medlem;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum MedlemskapType implements Kodeverdi {

    ENDELIG("ENDELIG", "Endelig"),
    FORELOPIG("FORELOPIG", "Foreløpig"),
    UNDER_AVKLARING("AVKLARES", "Under avklaring"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, MedlemskapType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "MEDLEMSKAP_TYPE";

    @Deprecated
    public static final String DISCRIMINATOR = "MEDLEMSKAP_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private MedlemskapType(String kode) {
        this.kode = kode;
    }

    private MedlemskapType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static MedlemskapType  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent MedlemskapType: " + kode);
        }
        return ad;
    }

    public static Map<String, MedlemskapType> kodeMap() {
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
