package no.nav.ung.sak.kontrakt.oppgaver;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resultat fra migrering av brukerdialogoppgaver.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public record MigreringsResultat(
    @JsonProperty(value = "antallOpprettet")
    int antallOpprettet,

    @JsonProperty(value = "antallHoppetOver")
    int antallHoppetOver,

    @JsonProperty(value = "antallTotalt")
    int antallTotalt
) {
    public MigreringsResultat(int antallOpprettet, int antallHoppetOver) {
        this(antallOpprettet, antallHoppetOver, antallOpprettet + antallHoppetOver);
    }
}

