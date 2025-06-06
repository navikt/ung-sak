package no.nav.ung.kodeverk.dokument;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum DokumentGruppe implements Kodeverdi {

    SØKNAD("SØKNAD", "Søknad"),
    INNTEKTSMELDING("INNTEKTSMELDING", "Inntektsmelding"),
    VEDLEGG("VEDLEGG", "Vedlegg"),
    UDEFINERT("-", "Ikke definert"),

    ;

    public static final String KODEVERK = "DOKUMENT_GRUPPE";

    private static final Map<String, DokumentGruppe> KODER = new LinkedHashMap<>();

    private String navn;

    private String kode;

    private DokumentGruppe(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static DokumentGruppe  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent DokumentGruppe: " + kode);
        }
        return ad;
    }

    public static Map<String, DokumentGruppe> kodeMap() {
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
        return getKode();
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

}
