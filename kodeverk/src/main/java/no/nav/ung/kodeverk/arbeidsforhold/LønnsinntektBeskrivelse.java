package no.nav.ung.kodeverk.arbeidsforhold;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum LønnsinntektBeskrivelse implements Kodeverdi {

    KOMMUNAL_OMSORGSLOENN_OG_FOSTERHJEMSGODTGJOERELSE("KOMMUNAL_OMSORGSLOENN_OG_FOSTERHJEMSGODTGJOERELSE", "Kommunal omsorgslønn og fosterhjemsgodtgjørelse", "kommunalOmsorgsloennOgFosterhjemsgodtgjoerelse"),
    UDEFINERT("-", "Udefinert", "Ikke definert"),
    ;

    private static final Map<String, LønnsinntektBeskrivelse> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "LONNSINNTEKT_BESKRIVELSE";

    @Deprecated
    public static final String DISCRIMINATOR = "LONNSINNTEKT_BESKRIVELSE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;
    private String offisiellKode;

    private LønnsinntektBeskrivelse(String kode) {
        this.kode = kode;
    }

    private LønnsinntektBeskrivelse(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    public static LønnsinntektBeskrivelse fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent LønnsinntektBeskrivelse: " + kode);
        }
        return ad;
    }

    public static Map<String, LønnsinntektBeskrivelse> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

}
