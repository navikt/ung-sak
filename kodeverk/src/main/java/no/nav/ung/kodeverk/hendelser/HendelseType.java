package no.nav.ung.kodeverk.hendelser;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

public enum HendelseType implements Kodeverdi {

    PDL_DØDFØDSEL("PDL_DØDFØDSEL"),
    PDL_FORELDER_BARN_RELASJON("PDL_FORELDERBARNRELASJON"),
    PDL_DØDSFALL("PDL_DØDSFALL"),
    UNGDOMSPROGRAM_OPPHØR("UNG_OPPHØR"),
    UNGDOMSPROGRAM_ENDRET_STARTDATO("UNGDOMSPROGRAM_ENDRET_STARTDATO"),

    UDEFINERT("-"),
    ;

    public static final String KODEVERK = "HENDELSE_TYPE";

    private static final Map<String, HendelseType> KODER = new LinkedHashMap<>();

    private String kode;

    HendelseType() {
        // Hibernate trenger den
    }

    private HendelseType(String kode) {
        this.kode = kode;
    }

    public static HendelseType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ReferanseType: " + kode);
        }
        return ad;
    }


    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return null;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return null;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }
}
