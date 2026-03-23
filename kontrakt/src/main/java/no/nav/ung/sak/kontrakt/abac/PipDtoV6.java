package no.nav.ung.sak.kontrakt.abac;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

import java.util.Set;

public record PipDtoV6(
    FagsakYtelseType fagsakYtelseType,
    Saksnummer saksnummer,
    Set<AktørId> aktørIder,
    Set<AktørId> aktørIderForSporingslogg,
    BehandlingStatus behandlingStatus,
    FagsakStatus fagsakStatus,
    Set<String> ansvarligSaksbehandlere
) {

    public PipDtoV6 {
        if (!aktørIder.containsAll(aktørIderForSporingslogg)){
            throw new IllegalArgumentException("Forventet at aktørIderForSporingslogg er et sub-set av aktørIder");
        }
    }
}
