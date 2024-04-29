package no.nav.k9.sak.kontrakt.opplæringspenger.dokument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OpplæringDokumentType {
    DOKUMENTASJON_AV_OPPLÆRING("DOKUMENTASJON_AV_OPPLÆRING", "DO"),
    LEGEERKLÆRING_MED_DOKUMENTASJON_AV_OPPLÆRING("LEGEERKLÆRING_MED_DOKUMENTASJON_AV_OPPLÆRING", "LDO");

    @JsonValue
    private final String apikode;

    @JsonIgnore
    private final String databasekode;


    OpplæringDokumentType(String apikode, String databasekode) {
        this.apikode = apikode;
        this.databasekode = databasekode;
    }


    public String getDatabasekode() {
        return databasekode;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OpplæringDokumentType fraApikode(String s) {
        for (var type : values()) {
            if (type.apikode.equals(s)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Ukjent type: " + s);
    }
}
