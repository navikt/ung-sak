package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.UUID;

import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDager;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.Saksnummer;

public interface ÅrskvantumTjeneste {

    ÅrskvantumResultat hentÅrskvantumUttak(BehandlingReferanse ref);

    ÅrskvantumForbrukteDager hentÅrskvantumForBehandling(UUID behandlingUuid);

    Periode hentPeriodeForFagsak(Saksnummer ref);

    void deaktiverUttakForBehandling(UUID behandlingUuid);

    void bekreftUttaksplan(Long behandlingId);
}
