package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

@Dependent
public final class EndringInntektReduksjonStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringRapportertInntektInnholdBygger endringRapportertInntektInnholdBygger;

    @Inject
    public EndringInntektReduksjonStrategy(EndringRapportertInntektInnholdBygger endringRapportertInntektInnholdBygger) {
        this.endringRapportertInntektInnholdBygger = endringRapportertInntektInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return VedtaksbrevStrategyResultat.medBrev(DokumentMalType.ENDRING_INNTEKT, endringRapportertInntektInnholdBygger, "Automatisk brev ved endring av rapportert inntekt.");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholderIkke(DetaljertResultatType.INNVILGELSE_UTBETALING)
            && resultater.innholder(DetaljertResultatType.KONTROLLER_INNTEKT_REDUKSJON);
    }

}
