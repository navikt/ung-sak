package no.nav.ung.sak.formidling.vedtak.regler;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatType;

public final class EndringInntektByggerStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringRapportertInntektInnholdBygger endringRapportertInntektInnholdBygger;

    @Inject
    public EndringInntektByggerStrategy(EndringRapportertInntektInnholdBygger endringRapportertInntektInnholdBygger) {
        this.endringRapportertInntektInnholdBygger = endringRapportertInntektInnholdBygger;
    }

    @Override
    public ByggerResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return new ByggerResultat(endringRapportertInntektInnholdBygger, "Automatisk brev ved endring av rapportert inntekt.");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholder(DetaljertResultatType.KONTROLLER_INNTEKT_REDUKSJON);
    }

}
