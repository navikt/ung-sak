package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public enum BostedsvilkårIkkeOppfyltÅrsak implements IkkeOppfyltDetaljertÅrsak {

    // Ikke bosattadresse i Trondheim
    IKKE_BOSATTADRESSE_I_TRONDHEIM(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE, false),
    // Ikke bostedsadresse i Trondheim og ikke folkeregistrert i Trondheim
    IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE, false),
    // Har studie- eller arbeidssted utenfor Trondheim
    STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE, false),
    // Saksbehandler har valgt å innvilge periode som er kortere enn perioden saksbehandlingssystemet tillater å innvilge.
    AVKORTET(Avslagsårsak.AVKORTET, false),
    // Annet
    ANNET(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE, true),
    UDEFINERT(null, true),
    ;

    private static final Map<String, BostedsvilkårIkkeOppfyltÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            KODER.put(v.name(), v);
        }
    }

    private final Avslagsårsak avslagsårsak;
    private final boolean kreverFritekst;

    BostedsvilkårIkkeOppfyltÅrsak(Avslagsårsak avslagsårsak,
                                  boolean kreverFritekst) {
        this.avslagsårsak = avslagsårsak;
        this.kreverFritekst = kreverFritekst;
    }

    @Override
    public Optional<Avslagsårsak> avslagsårsak() {
        return Optional.ofNullable(avslagsårsak);
    }

    @Override
    public boolean kreverFritekst() {
        return kreverFritekst;
    }

    public static BostedsvilkårIkkeOppfyltÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var v = KODER.get(kode);
        if (v == null) {
            throw new IllegalArgumentException("Ukjent BostedsvilkårIkkeOppfyltÅrsak: " + kode);
        }
        return v;
    }

    public static Map<String, BostedsvilkårIkkeOppfyltÅrsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @JsonValue
    @Override
    public String getKode() {
        return name();
    }
}
