package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.EndringBarnetilleggInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

@Dependent
public final class EndringBarnetilleggStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringBarnetilleggInnholdBygger endringBarnetilleggInnholdBygger;

    @Inject
    public EndringBarnetilleggStrategy(EndringBarnetilleggInnholdBygger endringBarnetilleggInnholdBygger) {
        this.endringBarnetilleggInnholdBygger = endringBarnetilleggInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return VedtaksbrevStrategyResultat.medBrev(DokumentMalType.ENDRING_BARNETILLEGG, endringBarnetilleggInnholdBygger, "Automatisk brev ved fødsel av barn.");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholderIkke(DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE)
            && resultater.innholderIkke(DetaljertResultatType.AVSLAG_INNGANGSVILKÅR)
            && resultater.innholder(DetaljertResultatType.ENDRING_BARN_FØDSEL);
    }

}
