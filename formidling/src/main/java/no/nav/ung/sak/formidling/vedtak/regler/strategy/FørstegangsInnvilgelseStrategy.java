package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.FørstegangsInnvilgelseInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

@Dependent
public final class FørstegangsInnvilgelseStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final FørstegangsInnvilgelseInnholdBygger førstegangsInnvilgelseInnholdBygger;

    @Inject
    public FørstegangsInnvilgelseStrategy(FørstegangsInnvilgelseInnholdBygger førstegangsInnvilgelseInnholdBygger) {
        this.førstegangsInnvilgelseInnholdBygger = førstegangsInnvilgelseInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return VedtaksbrevStrategyResultat.medBrev(DokumentMalType.INNVILGELSE_DOK, førstegangsInnvilgelseInnholdBygger, "Automatisk brev ved ny innvilgelse. ");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater
            .innholder(DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE);
    }
}
