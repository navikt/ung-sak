package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public enum AndreLivsoppholdsytelserIkkeOppfyltÅrsak implements IkkeOppfyltDetaljertÅrsak {

    // Søker har livsoppholdsytelse som ikke er forenelig med ytelsen.
    HAR_ANNEN_LIVSOPPHOLDSYTELSE,
    // Saksbehandler har valgt å innvilge periode som er kortere enn perioden saksbehandlingssystemet tillater å innvilge.
    AVKORTET,
    UDEFINERT,
    ;

    private static final Map<String, AndreLivsoppholdsytelserIkkeOppfyltÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            KODER.put(v.name(), v);
        }
    }


    public static AndreLivsoppholdsytelserIkkeOppfyltÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var v = KODER.get(kode);
        if (v == null) {
            throw new IllegalArgumentException("Ukjent AndreLivsoppholdsytelserIkkeOppfyltÅrsak: " + kode);
        }
        return v;
    }

    public static Map<String, AndreLivsoppholdsytelserIkkeOppfyltÅrsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @JsonValue
    @Override
    public String getKode() {
        return name();
    }

    @Override
    public Optional<Avslagsårsak> avslagsårsak() {
        return Optional.empty();
    }

    @Override
    public boolean kreverFritekst() {
        return false;
    }
}
