package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public enum AktivitetsvilkåretIkkeOppfyltÅrsak implements Kodeverdi, IkkeOppfyltDetaljertÅrsak {

    //FIXME spesifikke avlagsårsaker for aktivitetsvilkåret er var ikke klare. Oppdater med faktiske årsaker når de er på plass
    ANNET("ANNET", "Annet/fritekst",
        Avslagsårsak.AKTIVITETSVILKÅR_GENERELL_AVSLAGSÅRSAK, true, true),

    AVKORTET("AVKORTET", "Saksbehandler har valgt å innvilge periode som er kortere enn perioden saksbehandlingssystemet tillater å innvilge.",
        Avslagsårsak.AVKORTET, false, false),
    UDEFINERT("-", "Ikke definert", null, true, false),
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
    private final Avslagsårsak avslagsårsak;
    private final boolean krevesFritekst;
    private final boolean erGyldigAvklaringsårsak;

    AktivitetsvilkåretIkkeOppfyltÅrsak(String kode, String navn, Avslagsårsak avslagsårsak,
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
