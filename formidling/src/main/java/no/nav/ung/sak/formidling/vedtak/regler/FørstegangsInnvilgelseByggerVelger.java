package no.nav.ung.sak.formidling.vedtak.regler;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.FørstegangsInnvilgelseInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatType;

public class FørstegangsInnvilgelseByggerVelger implements VedtaksbrevByggerVelger {

    private final FørstegangsInnvilgelseInnholdBygger førstegangsInnvilgelseInnholdBygger;

    @Inject
    public FørstegangsInnvilgelseByggerVelger(FørstegangsInnvilgelseInnholdBygger førstegangsInnvilgelseInnholdBygger) {
        this.førstegangsInnvilgelseInnholdBygger = førstegangsInnvilgelseInnholdBygger;
    }

    @Override
    public ByggerResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return new ByggerResultat(førstegangsInnvilgelseInnholdBygger, "Automatisk brev ved ny innvilgelse. ");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevByggerVelger.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater
            .utenom(DetaljertResultatType.INNVILGELSE_VILKÅR_NY_PERIODE)
            .innholderBare(DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE);
    }
}
