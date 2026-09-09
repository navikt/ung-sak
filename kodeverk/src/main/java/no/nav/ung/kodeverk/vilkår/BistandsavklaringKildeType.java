package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum BistandsavklaringKildeType implements AvklaringKilde {

    BRUKER(false),
    ANNET(true),
    ;

    private static final Map<String, BistandsavklaringKildeType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            KODER.put(v.name(), v);
        }
    }

    private final boolean kreverFritekst;

    BistandsavklaringKildeType(boolean kreverFritekst) {
        this.kreverFritekst = kreverFritekst;
    }

    @Override
    public boolean kreverFritekst() {
        return kreverFritekst;
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
        return name();
    }
}
