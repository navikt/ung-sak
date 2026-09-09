package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public enum AktivitetsvilkåretIkkeOppfyltÅrsak implements IkkeOppfyltDetaljertÅrsak {

    //FIXME spesifikke avlagsårsaker for aktivitetsvilkåret er var ikke klare. Oppdater med faktiske årsaker når de er på plass
    // Annet/fritekst
    ANNET,

    // Saksbehandler har valgt å innvilge periode som er kortere enn perioden saksbehandlingssystemet tillater å innvilge.
    AVKORTET,
    UDEFINERT,
    ;

    private static final Map<String, AktivitetsvilkåretIkkeOppfyltÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            KODER.put(v.name(), v);
        }
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
