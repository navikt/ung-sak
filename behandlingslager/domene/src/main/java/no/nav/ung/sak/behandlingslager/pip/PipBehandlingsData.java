package no.nav.ung.sak.behandlingslager.pip;

import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.sak.typer.Saksnummer;

import java.util.UUID;

public record PipBehandlingsData (
    UUID behandlingUuid,
    BehandlingStatus behandligStatus,
    FagsakStatus fagsakStatus,
    String ansvarligSaksbehandler,
    Saksnummer saksnummer
){

}
