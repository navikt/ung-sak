package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AndreLivsoppholdsytelserAvklaringKildeType implements AvklaringKilde {
    BRUKER(false),
    NAV(false),
    ANNET(true),
    ;

    private final boolean kreverFritekst;

    AndreLivsoppholdsytelserAvklaringKildeType(boolean kreverFritekst) {
        this.kreverFritekst = kreverFritekst;
    }

    @Override
    public boolean kreverFritekst() {
        return kreverFritekst;
    }

    public static AndreLivsoppholdsytelserAvklaringKildeType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        try {
            return valueOf(kode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ukjent AndreLivsoppholdsytelserAvklaringKildeType: " + kode, e);
        }
    }

    @JsonValue
    @Override
    public String getKode() {
        return name();
    }
}
