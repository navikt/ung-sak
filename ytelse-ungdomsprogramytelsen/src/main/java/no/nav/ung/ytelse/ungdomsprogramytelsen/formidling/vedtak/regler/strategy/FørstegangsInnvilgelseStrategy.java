package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.Presedens;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.FørstegangsInnvilgelseInnholdBygger;

import java.util.List;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class FørstegangsInnvilgelseStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final FørstegangsInnvilgelseInnholdBygger førstegangsInnvilgelseInnholdBygger;

    @Inject
    public FørstegangsInnvilgelseStrategy(FørstegangsInnvilgelseInnholdBygger førstegangsInnvilgelseInnholdBygger) {
        this.førstegangsInnvilgelseInnholdBygger = førstegangsInnvilgelseInnholdBygger;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultater = new ResultatHelper(VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat));
        if (!resultater.innholder(DetaljertResultatType.INNVILGELSE_UTBETALING)) {
            return List.of();
        }
        return List.of(VedtaksbrevStrategyResultat.medUredigerbarBrev(DokumentMalType.INNVILGELSE_DOK, førstegangsInnvilgelseInnholdBygger, "Automatisk brev ved ny innvilgelse. "));
    }

    @Override
    public Presedens presedens() {
        return Presedens.OVERSTYRENDE_ENKELTBREV;
    }
}
