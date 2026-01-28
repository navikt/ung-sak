package no.nav.ung.sak.kontrakt.abac;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.felles.typer.Saksnummer;

import java.util.Set;
public record PipDtoV3(
    Saksnummer saksnummer,
    Set<AktørId> aktørIder,
    BehandlingStatus behandlingStatus,
    FagsakStatus fagsakStatus,
    String ansvarligSaksbehandler) {
}
