package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest;

import java.util.UUID;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumForbrukteDager;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumGrunnlag;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResterendeDager;

@RequestScoped
@Alternative
public class ÅrskvantumInMemoryKlient implements ÅrskvantumKlient {

    private ÅrskvantumResultat årskvantumResultat;

    @Override
    public ÅrskvantumResultat hentÅrskvantumUttak(ÅrskvantumGrunnlag årskvantumRequest) {
        return årskvantumResultat;
    }

    @Override
    public void avbrytÅrskvantumForBehandling(UUID behandlingUUID) {

    }

    @Override
    public ÅrskvantumForbrukteDager hentÅrskvantumForBehandling(UUID behandlingUUID) {
        return null;
    }

    @Override
    public Periode hentPeriodeForFagsak(String saksnummer) {
        return null;
    }

    @Override
    public ÅrskvantumResterendeDager hentResterendeKvantum(String aktørId) {
        return null;
    }

    public void setÅrskvantumResultat(ÅrskvantumResultat årskvantumResultat) {
        this.årskvantumResultat = årskvantumResultat;

    }
}
