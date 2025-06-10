package no.nav.ung.kodeverk.dokument;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

public enum FordelBehandlingType implements Kodeverdi {

    DIGITAL_SØKNAD("DIGITAL_SØKNAD","ae0227", "Digital søknad"),
    DIGITAL_ETTERSENDELSE("DIGITAL_ETTERSENDELSE", "ae0246", "Digital ettersendelse"),
    UDEFINERT("-", null, "Ikke definert"),
    ;

    private static final Map<String, FordelBehandlingType> KODER = new LinkedHashMap<>();
    private static final Map<String, FordelBehandlingType> OFFISIELLE_KODER = new LinkedHashMap<>();
    private static final Map<String, FordelBehandlingType> ALLE_TERMNAVN = new LinkedHashMap<>();

    public static final String KODEVERK = "BEHANDLING_TEMA";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            if (v.offisiellKode != null) {
                OFFISIELLE_KODER.putIfAbsent(v.offisiellKode, v);
            }
            if (v.navn != null) {
                ALLE_TERMNAVN.putIfAbsent(v.navn, v);
            }
        }
    }

    private String kode;

    private String offisiellKode;

    private String navn;

    private FordelBehandlingType(String kode, String offisiellKode, String navn) {
        this.kode = kode;
        this.offisiellKode = offisiellKode;
        this.navn = navn;
    }

    public static FordelBehandlingType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Tema: " + kode);
        }
        return ad;
    }

    public static FordelBehandlingType fraKodeDefaultUdefinert(final String kode) {
        if (kode == null) {
            return UDEFINERT;
        }
        return KODER.getOrDefault(kode, UDEFINERT);
    }

    public static FordelBehandlingType fraOffisiellKode(String kode) {
        if (kode == null) {
            return UDEFINERT;
        }
        return OFFISIELLE_KODER.getOrDefault(kode, UDEFINERT);
    }

    public static FordelBehandlingType fraTermNavn(String termnavn) {
        if (termnavn == null) {
            return UDEFINERT;
        }
        return ALLE_TERMNAVN.getOrDefault(termnavn, UDEFINERT);
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

    public String getOffisiellKode() {
        return offisiellKode;
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
