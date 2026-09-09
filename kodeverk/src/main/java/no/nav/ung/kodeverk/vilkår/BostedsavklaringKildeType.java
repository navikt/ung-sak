package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum BostedsavklaringKildeType implements AvklaringKilde {

    BRUKER(false),
    FOLKEREGISTER(false),
    ANNET(true),
    ;

    private static final Map<String, BostedsavklaringKildeType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            KODER.put(v.name(), v);
        }
    }

    private final boolean kreverFritekst;

    BostedsavklaringKildeType(boolean kreverFritekst) {
        this.kreverFritekst = kreverFritekst;
    }

    @Override
    public boolean kreverFritekst() {
        return kreverFritekst;
    }

    public static BostedsavklaringKildeType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var v = KODER.get(kode);
        if (v == null) {
            throw new IllegalArgumentException("Ukjent BostedsavklaringKildeType: " + kode);
        }
        return v;
    }

    public static Map<String, BostedsavklaringKildeType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @JsonValue
    @Override
    public String getKode() {
        return name();
    }
}
