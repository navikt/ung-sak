package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public enum AndreLivsoppholdsytelserIkkeOppfyltÅrsak implements Kodeverdi, IkkeOppfyltDetaljertÅrsak {

    HAR_ANNEN_LIVSOPPHOLDSYTELSE("HAR_ANNEN_LIVSOPPHOLDSYTELSE", "Søker har livsoppholdsytelse som ikke er forenelig med ytelsen.",
        Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, false, true),
    AVKORTET("AVKORTET", "Saksbehandler har valgt å innvilge periode som er kortere enn perioden saksbehandlingssystemet tillater å innvilge.",
        Avslagsårsak.AVKORTET, false, false),
    UDEFINERT("-", "Ikke definert", null, true, false),
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
    private final Avslagsårsak avslagsårsak;
    private final boolean krevesFritekst;
    private final boolean erGyldigAvklaringsårsak;

    AndreLivsoppholdsytelserIkkeOppfyltÅrsak(String kode, String navn, Avslagsårsak avslagsårsak,
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
