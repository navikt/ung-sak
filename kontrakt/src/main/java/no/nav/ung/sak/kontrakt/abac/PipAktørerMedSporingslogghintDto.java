package no.nav.ung.sak.kontrakt.abac;

import no.nav.ung.sak.typer.AktørId;

import java.util.Set;


public record PipAktørerMedSporingslogghintDto(Set<AktørId> aktørIderForTilgangskontroll,
                                               Set<AktørId> aktørIderForSporingslogg) {

    public PipAktørerMedSporingslogghintDto {
        if (!aktørIderForTilgangskontroll.containsAll(aktørIderForSporingslogg)){
            throw new IllegalArgumentException("Forventet at aktørIderForSporingslogg er et sub-set av aktørIderForTilgangskontroll");
        }
    }
}
