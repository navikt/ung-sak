package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum AktivitetsvilkåretIkkeOppfyltÅrsak implements Kodeverdi {

    ÅRSAK_1("ÅRSAK_1", "TODO beskrivelse for årsak 1"),
    AVKORTET("AVKORTET", "Saksbehandler har valgt å innvilge periode som er kortere enn perioden saksbehandlingssystemet tillater å innvilge."),
    UDEFINERT("-", "Ikke definert"),
    ;

    public static final String KODEVERK = "AKTIVITETSVILKAAR_IKKE_OPPFYLT_AARSAK";
    private static final Map<String, AktivitetsvilkåretIkkeOppfyltÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String kode;
    private final String navn;

    AktivitetsvilkåretIkkeOppfyltÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static AktivitetsvilkåretIkkeOppfyltÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var v = KODER.get(kode);
        if (v == null) {
            throw new IllegalArgumentException("Ukjent AktivitetsvilkåretIkkeOppfyltÅrsak: " + kode);
        }
        return v;
    }

    public static Map<String, AktivitetsvilkåretIkkeOppfyltÅrsak> kodeMap() {
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
