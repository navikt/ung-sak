package no.nav.ung.sak.kontrakt.oppgaver;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resultat fra migrering av brukerdialogoppgaver.
 */
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

