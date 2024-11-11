package no.nav.ung.sak.mottak;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

public interface NyBehandlingOppretter {

    Behandling opprettNyBehandling(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak);
}
