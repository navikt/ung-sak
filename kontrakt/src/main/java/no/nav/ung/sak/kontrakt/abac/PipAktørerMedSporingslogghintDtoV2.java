package no.nav.ung.sak.kontrakt.abac;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.AktørId;

import java.util.Set;


public record PipAktørerMedSporingslogghintDtoV2(Set<AktørId> aktørIderForTilgangskontroll,
                                                 Set<AktørId> aktørIderForSporingslogg,
                                                 FagsakYtelseType fagsakYtelseType) {

    public PipAktørerMedSporingslogghintDtoV2 {
        if (!aktørIderForTilgangskontroll.containsAll(aktørIderForSporingslogg)){
            throw new IllegalArgumentException("Forventet at aktørIderForSporingslogg er et sub-set av aktørIderForTilgangskontroll");
        }
    }
}
