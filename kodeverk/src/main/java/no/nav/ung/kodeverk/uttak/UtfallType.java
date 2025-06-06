package no.nav.ung.kodeverk.uttak;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.LegacyKodeverdiJsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@LegacyKodeverdiJsonValue // Serialiserast som kode string i default object mapper
public enum UtfallType implements Kodeverdi {

    INNVILGET("INNVILGET", "Innvilget"),
    AVSLÅTT("AVSLÅTT", "Avslått"),
    UDEFINERT("UDEFINERT", "Ikke definert"),
    ;

    private static final Map<String, UtfallType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "UTTAK_UTFALL_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            KODER.putIfAbsent(v.name(), v); // kompatibilitet
        }
    }

    private String navn;

    @JsonValue
    private String kode;

    private UtfallType(String kode) {
        this.kode = kode;
    }

    private UtfallType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static UtfallType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent UtfallType: " + kode);
        }
        return ad;
    }

    public static Map<String, UtfallType> kodeMap() {
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

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

}
