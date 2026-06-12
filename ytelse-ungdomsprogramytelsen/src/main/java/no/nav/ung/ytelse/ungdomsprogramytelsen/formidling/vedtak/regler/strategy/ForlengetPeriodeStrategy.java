package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.ForlengetPeriodeInnholdBygger;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class ForlengetPeriodeStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final ForlengetPeriodeInnholdBygger forlengetPeriodeInnholdBygger;

    @Inject
    public ForlengetPeriodeStrategy(ForlengetPeriodeInnholdBygger forlengetPeriodeInnholdBygger) {
        this.forlengetPeriodeInnholdBygger = forlengetPeriodeInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return VedtaksbrevStrategyResultat.medUredigerbarBrev(DokumentMalType.FORLENGET_PERIODE, forlengetPeriodeInnholdBygger, "Automatisk brev ved forlenget periode");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholderIkke(DetaljertResultatType.INNVILGELSE_UTBETALING)
            && resultater.innholderIkke(DetaljertResultatType.ENDRING_SLUTTDATO)
            && resultater.innholder(DetaljertResultatType.FORLENGET_PERIODE);
    }

}
