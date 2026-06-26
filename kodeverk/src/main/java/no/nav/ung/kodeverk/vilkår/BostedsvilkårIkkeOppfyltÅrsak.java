package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum BostedsvilkårIkkeOppfyltÅrsak implements Kodeverdi {

    IKKE_BOSATTADRESSE_I_TRONDHEIM("IKKE_BOSATTADRESSE_I_TRONDHEIM", "Ikke bosattadresse i Trondheim"),
    IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM("IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM", "Ikke bostedsadresse i Trondheim og ikke folkeregistrert i Trondheim"),
    STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM("STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM", "Har studie- eller arbeidssted utenfor Trondheim"),
    ANNET("ANNET", "Annet"),
    UDEFINERT("-", "Ikke definert"),
    ;

    public static final String KODEVERK = "BOSTEDSVILKAAR_IKKE_OPPFYLT_AARSAK";
    private static final Map<String, BostedsvilkårIkkeOppfyltÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String kode;
    private final String navn;

    BostedsvilkårIkkeOppfyltÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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

