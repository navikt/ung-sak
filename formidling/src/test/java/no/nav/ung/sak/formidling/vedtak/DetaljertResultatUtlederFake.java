package no.nav.ung.sak.formidling.vedtak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatUtleder;

public class DetaljertResultatUtlederFake implements DetaljertResultatUtleder {

    private final LocalDateTimeline<DetaljertResultat> resultat;

    public DetaljertResultatUtlederFake(LocalDateTimeline<DetaljertResultat> resultat) {
        this.resultat = resultat;
    }

    @Override
    public LocalDateTimeline<DetaljertResultat> utledDetaljertResultat(Behandling behandling) {
        return resultat;
    }
}
