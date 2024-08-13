package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.NotImplementedException;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
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

@RequestScoped
@Alternative
public class ÅrskvantumInMemoryKlient implements ÅrskvantumKlient {

    private ÅrskvantumResultat årskvantumResultat;

    @Override
    public ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumGrunnlag årskvantumRequest) {
        return årskvantumResultat;
    }

    @Override
    public ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumGrunnlagV2 årskvantumRequest) {
        return årskvantumResultat;
    }

    @Override
    public void deaktiverUttakForBehandling(UUID behandlingUUID) {
    }

    @Override
    public void settUttaksplanTilManueltBekreftet(UUID behandlingUUID) {

    }

    @Override
    public void innvilgeEllerAvslåPeriodeneManuelt(UUID behandlingUUID, boolean innvilgePeriodene, Optional<Integer> antallDager) {

    }

    @Override
    public void slettUttaksplan(UUID behandlingUUID) {

    }

    @Override
    public ÅrskvantumForbrukteDager hentÅrskvantumForBehandling(UUID behandlingUUID) {
        return null;
    }

    @Override
    public ÅrskvantumForbrukteDagerV2 hentÅrskvantumForBehandlingV2(UUID behandlingUUID) {
        return null;
    }

    @Override
    public Periode hentPeriodeForFagsak(Saksnummer saksnummer) {
        return null;
    }

    @Override
    public FullUttaksplan hentFullUttaksplan(Saksnummer saksnummer) {
        return null;
    }

    @Override
    public FullUttaksplanForBehandlinger hentFullUttaksplanForBehandling(List<UUID> behandlinger) {
        return null;
    }

    public void setÅrskvantumResultat(ÅrskvantumResultat årskvantumResultat) {
        this.årskvantumResultat = årskvantumResultat;

    }

    @Override
    public ÅrskvantumUtbetalingGrunnlag hentUtbetalingGrunnlag(ÅrskvantumGrunnlag årskvantumGrunnlag) {
        return null;
    }

    @Override
    public ÅrskvantumUtbetalingGrunnlag hentUtbetalingGrunnlag(ÅrskvantumGrunnlagV2 årskvantumGrunnlag) {
        return null;
    }

    @Override
    public RammevedtakResponse hentRammevedtak(PersonIdent personIdent, List<PersonIdent> barnFnr, LukketPeriode periode) {
        return null;
    }

    @Override
    public RammevedtakResponse hentRammevedtak(RammevedtakV2Request request) {
        return null;
    }

    @Override
    public ÅrskvantumUttrekk hentUttrekk() {
        return null;
    }

    @Override
    public Integer oppdaterPersonident(PersonIdent nyPersonident, List<PersonIdent> gamlePersonidenter) {
        throw new NotImplementedException("Ikke implementert");
    }
}
