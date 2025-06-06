package no.nav.ung.kodeverk.arbeidsforhold;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum RelatertYtelseTilstand implements Kodeverdi {

    ÅPEN("ÅPEN", "Åpen"),
    LØPENDE("LØPENDE", "Løpende"),
    AVSLUTTET("AVSLUTTET", "Avsluttet"),
    IKKE_STARTET("IKKESTARTET", "Ikke startet"),
    ;

    private static final Map<String, RelatertYtelseTilstand> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "RELATERT_YTELSE_TILSTAND";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private RelatertYtelseTilstand(String kode) {
        this.kode = kode;
    }

    private RelatertYtelseTilstand(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static RelatertYtelseTilstand fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent RelatertYtelseTilstand: " + kode);
        }
        return ad;
    }

    public static Map<String, RelatertYtelseTilstand> kodeMap() {
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
}
