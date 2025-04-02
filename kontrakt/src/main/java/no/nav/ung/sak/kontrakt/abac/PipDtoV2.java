package no.nav.ung.sak.kontrakt.abac;

import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.sak.typer.AktørId;

import java.util.Set;

public record PipDtoV2(
    Set<AktørId> aktørIder,
    BehandlingStatus behandlingStatus,
    FagsakStatus fagsakStatus,
    String ansvarligSaksbehandler) {
}
