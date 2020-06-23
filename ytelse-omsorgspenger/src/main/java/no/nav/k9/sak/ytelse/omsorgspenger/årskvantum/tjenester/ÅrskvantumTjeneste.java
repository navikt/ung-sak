package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.UUID;

import no.nav.k9.aarskvantum.kontrakter.*;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.Saksnummer;

public interface ÅrskvantumTjeneste {

    ÅrskvantumResultat beregnÅrskvantumUttak(BehandlingReferanse ref);

    ÅrskvantumForbrukteDager hentÅrskvantumForBehandling(UUID behandlingUuid);

    FullUttaksplan hentFullUttaksplan(Saksnummer saksnummer);

    Periode hentPeriodeForFagsak(Saksnummer ref);

    void deaktiverUttakForBehandling(UUID behandlingUuid);

    void bekreftUttaksplan(Long behandlingId);

    void slettUttaksplan(Long behandlingId);

    ÅrskvantumGrunnlag hentInputTilBeregning(UUID behandlingUuid);

    ÅrskvantumUtbetalingGrunnlag hentUtbetalingGrunnlag(UUID behandlingUuid);
}
