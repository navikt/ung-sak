package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BostedsavklaringKildeType implements AvklaringKilde {

    BRUKER(false),
    FOLKEREGISTER(false),
    ANNET(true),
    ;

    private final boolean kreverFritekst;

    BostedsavklaringKildeType(boolean kreverFritekst) {
        this.kreverFritekst = kreverFritekst;
    }

    @Override
    public boolean kreverFritekst() {
        return kreverFritekst;
    }

    public static BostedsavklaringKildeType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        try {
            return valueOf(kode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ukjent BostedsavklaringKildeType: " + kode, e);
        }
    }

    @JsonValue
    @Override
    public String getKode() {
        return name();
    }
}
