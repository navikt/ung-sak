package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.k9.aarskvantum.kontrakter.FullUttaksplan;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplanForBehandlinger;
import no.nav.k9.aarskvantum.kontrakter.LukketPeriode;
import no.nav.k9.aarskvantum.kontrakter.RammevedtakResponse;
import no.nav.k9.aarskvantum.kontrakter.RammevedtakV2Request;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDager;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDagerV2;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumGrunnlag;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumGrunnlagV2;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumUtbetalingGrunnlag;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumUttrekk;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;

public interface ÅrskvantumKlient {

    ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumGrunnlag årskvantumRequest);

    ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumGrunnlagV2 årskvantumRequest);

    void deaktiverUttakForBehandling(UUID behandlingUUID);

    void settUttaksplanTilManueltBekreftet(UUID behandlingUUID);

    void innvilgeEllerAvslåPeriodeneManuelt(UUID behandlingUUID, boolean innvilgePeriodene, Optional<Integer> antallDager);

    void slettUttaksplan(UUID behandlingUUID);

    ÅrskvantumForbrukteDager hentÅrskvantumForBehandling(UUID behandlingUUID);

    ÅrskvantumForbrukteDagerV2 hentÅrskvantumForBehandlingV2(UUID behandlingUUID);

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

    ÅrskvantumUtbetalingGrunnlag hentUtbetalingGrunnlag(ÅrskvantumGrunnlagV2 årskvantumGrunnlag);

    RammevedtakResponse hentRammevedtak(PersonIdent personIdent, List<PersonIdent> barnFnr, LukketPeriode periode);

    RammevedtakResponse hentRammevedtak(RammevedtakV2Request request);

    ÅrskvantumUttrekk hentUttrekk();

    Integer oppdaterPersonident(PersonIdent nyPersonident, List<PersonIdent> gamlePersonidenter);
}
