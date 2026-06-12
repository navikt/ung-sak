package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum BistandsvilkårIkkeOppfyltÅrsak implements Kodeverdi {

    IKKE_14A_VEDTAK("IKKE_14A_VEDTAK", "Søker har ikke oppfølgingsvedtak etter Navloven §14a."),
    UDEFINERT("-", "Ikke definert"),
    ;

    public static final String KODEVERK = "BISTANDSVILKAAR_IKKE_OPPFYLT_AARSAK";
    private static final Map<String, BistandsvilkårIkkeOppfyltÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String kode;
    private final String navn;

    BistandsvilkårIkkeOppfyltÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static BistandsvilkårIkkeOppfyltÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var v = KODER.get(kode);
        if (v == null) {
            throw new IllegalArgumentException("Ukjent BistandsvilkårIkkeOppfyltÅrsak: " + kode);
        }
        return v;
    }

    public static Map<String, BistandsvilkårIkkeOppfyltÅrsak> kodeMap() {
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
