package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import java.util.List;
import java.util.UUID;

import no.nav.k9.aarskvantum.kontrakter.*;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.Saksnummer;

public interface ÅrskvantumKlient {

    ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumGrunnlag årskvantumRequest);

    void deaktiverUttakForBehandling(UUID behandlingUUID);

    void settUttaksplanTilManueltBekreftet(UUID behandlingUUID);

    void slettUttaksplan(UUID behandlingUUID);

    ÅrskvantumForbrukteDager hentÅrskvantumForBehandling(UUID behandlingUUID);

    Periode hentPeriodeForFagsak(Saksnummer saksnummer);

    FullUttaksplan hentFullUttaksplan(Saksnummer saksnummer);

    /**
     * Henter ut uttaksplanen slik den ser ut for et sett med behandlinger
     *
     * @param behandlinger behandlinger
     *
     * @return uttaksplanen for behandlingene
     */
    FullUttaksplanForBehandlinger hentFullUttaksplanForBehandling(List<UUID> behandlinger);

    ÅrskvantumUtbetalingGrunnlag hentUtbetalingGrunnlag(ÅrskvantumGrunnlag årskvantumGrunnlag);
}
