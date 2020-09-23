package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import java.util.List;
import java.util.UUID;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import no.nav.k9.aarskvantum.kontrakter.*;
import no.nav.k9.sak.kontrakt.uttak.Periode;
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
    public void deaktiverUttakForBehandling(UUID behandlingUUID) {
    }

    @Override
    public void settUttaksplanTilManueltBekreftet(UUID behandlingUUID) {

    }

    @Override
    public void slettUttaksplan(UUID behandlingUUID) {

    }

    @Override
    public ÅrskvantumForbrukteDager hentÅrskvantumForBehandling(UUID behandlingUUID) {
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
}
