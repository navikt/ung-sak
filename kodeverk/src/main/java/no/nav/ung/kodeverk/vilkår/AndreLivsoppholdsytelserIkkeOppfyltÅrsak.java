package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum AndreLivsoppholdsytelserIkkeOppfyltÅrsak implements Kodeverdi {

    HAR_ANNEN_LIVSOPPHOLDSYTELSE("HAR_ANNEN_LIVSOPPHOLDSYTELSE", "Søker har livsoppholdsytelse som ikke er forenelig med ytelsen."),
    UDEFINERT("-", "Ikke definert"),
    ;

    public static final String KODEVERK = "ANDRE_LIVSOPPHOLDSYTELSER_IKKE_OPPFYLT_AARSAK";
    private static final Map<String, AndreLivsoppholdsytelserIkkeOppfyltÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String kode;
    private final String navn;

    AndreLivsoppholdsytelserIkkeOppfyltÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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
