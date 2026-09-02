package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public enum BostedsvilkårIkkeOppfyltÅrsak implements Kodeverdi, IkkeOppfyltDetaljertÅrsak {

    IKKE_BOSATTADRESSE_I_TRONDHEIM("IKKE_BOSATTADRESSE_I_TRONDHEIM", "Ikke bosattadresse i Trondheim",
        Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE, false, true),
    IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM("IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM", "Ikke bostedsadresse i Trondheim og ikke folkeregistrert i Trondheim",
        Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE, false, true),
    STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM("STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM", "Har studie- eller arbeidssted utenfor Trondheim",
        Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE, false, true),
    AVKORTET("AVKORTET", "Saksbehandler har valgt å innvilge periode som er kortere enn perioden saksbehandlingssystemet tillater å innvilge.",
        Avslagsårsak.AVKORTET, false, false),
    ANNET("ANNET", "Annet",
        Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE, true, true),
    UDEFINERT("-", "Ikke definert", null, true, false),
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
    private final Avslagsårsak avslagsårsak;
    private final boolean krevesFritekst;
    private final boolean erGyldigAvklaringsårsak;

    BostedsvilkårIkkeOppfyltÅrsak(String kode, String navn, Avslagsårsak avslagsårsak,
                                  boolean krevesFritekst, boolean erGyldigAvklaringsårsak) {
        this.kode = kode;
        this.navn = navn;
        this.avslagsårsak = avslagsårsak;
        this.krevesFritekst = krevesFritekst;
        this.erGyldigAvklaringsårsak = erGyldigAvklaringsårsak;
    }

    @Override
    public Optional<Avslagsårsak> avslagsårsak() {
        return Optional.ofNullable(avslagsårsak);
    }

    @Override
    public boolean krevesFritekst() {
        return krevesFritekst;
    }

    @Override
    public boolean erGyldigAvklaringsårsak() {
        return erGyldigAvklaringsårsak;
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

