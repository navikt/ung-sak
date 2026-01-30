package no.nav.ung.sak.kontrakt.abac;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

import java.util.Set;

public record PipDtoV4(
    Saksnummer saksnummer,
    Set<AktørId> aktørIder,
    Set<AktørId> aktørIderForSporingslogg,
    BehandlingStatus behandlingStatus,
    FagsakStatus fagsakStatus,
    String ansvarligSaksbehandler) {

    public PipDtoV4 {
        if (!aktørIder.containsAll(aktørIderForSporingslogg)){
            throw new IllegalArgumentException("Forventet at aktørIderForSporingslogg er et sub-set av aktørIder");
        }
    }
}
