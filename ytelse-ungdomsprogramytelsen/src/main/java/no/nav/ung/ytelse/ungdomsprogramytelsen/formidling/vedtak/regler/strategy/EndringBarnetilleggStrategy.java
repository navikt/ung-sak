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
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.EndringBarnetilleggInnholdBygger;

import java.util.List;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class EndringBarnetilleggStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringBarnetilleggInnholdBygger endringBarnetilleggInnholdBygger;

    @Inject
    public EndringBarnetilleggStrategy(EndringBarnetilleggInnholdBygger endringBarnetilleggInnholdBygger) {
        this.endringBarnetilleggInnholdBygger = endringBarnetilleggInnholdBygger;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        //TODO endre til å sjekke faktisk endring i grunnlaget
        return List.of(VedtaksbrevStrategyResultat.medUredigerbarBrev(DokumentMalType.ENDRING_BARNETILLEGG, endringBarnetilleggInnholdBygger, "Automatisk brev ved fødsel av barn."));
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultater = new ResultatHelper(VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat));
        return resultater.innholder(DetaljertResultatType.ENDRING_BARN_FØDSEL);
    }

}
