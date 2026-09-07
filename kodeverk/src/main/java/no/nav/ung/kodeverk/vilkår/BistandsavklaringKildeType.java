package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum BistandsavklaringKildeType implements AvklaringKilde {

    BRUKER("BRUKER", "Bruker", false),
    ANNET("ANNET", "Annet", true),
    ;

    public static final String KODEVERK = "BISTANDSAVKLARING_KILDE_TYPE";
    private static final Map<String, BistandsavklaringKildeType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String kode;
    private final String navn;
    private final boolean krevesFritekst;

    BistandsavklaringKildeType(String kode, String navn, boolean krevesFritekst) {
        this.kode = kode;
        this.navn = navn;
        this.krevesFritekst = krevesFritekst;
    }

    @Override
    public boolean krevesFritekst() {
        return krevesFritekst;
    }

    public static BistandsavklaringKildeType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var v = KODER.get(kode);
        if (v == null) {
            throw new IllegalArgumentException("Ukjent BistandsavklaringKildeType: " + kode);
        }
        return v;
    }

    public static Map<String, BistandsavklaringKildeType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
