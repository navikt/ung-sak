package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BistandsavklaringKildeType implements AvklaringKilde {

    BRUKER(false),
    ANNET(true),
    ;

    private final boolean kreverFritekst;

    BistandsavklaringKildeType(boolean kreverFritekst) {
        this.kreverFritekst = kreverFritekst;
    }

    @Override
    public boolean kreverFritekst() {
        return kreverFritekst;
    }

    public static BistandsavklaringKildeType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        try {
            return valueOf(kode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ukjent BistandsavklaringKildeType: " + kode, e);
        }
    }

    @JsonValue
    @Override
    public String getKode() {
        return name();
    }
}
