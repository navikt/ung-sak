package no.nav.ung.sak.kontrakt.abac;


import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

import java.util.Set;

public record FagsakPipDto(
    Saksnummer saksnummer,
    Set<AktørId> aktørIder,
    Set<AktørId> aktørIderForSporingslogg,
    FagsakStatus fagsakStatus,
    FagsakYtelseType ytelseType) {

    public FagsakPipDto {
        if (!aktørIder.containsAll(aktørIderForSporingslogg)) {
            throw new IllegalArgumentException("Forventet at aktørIderForSporingslogg er et sub-set av aktørIder");
        }
    }
}
