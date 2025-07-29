package no.nav.ung.sak.formidling.vedtak.regler;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.EndringBarnetilleggInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatType;

public final class EndringBarnetilleggByggerStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringBarnetilleggInnholdBygger endringHøySatsInnholdBygger;

    @Inject
    public EndringBarnetilleggByggerStrategy(EndringBarnetilleggInnholdBygger endringBarnetilleggInnholdBygger) {
        this.endringHøySatsInnholdBygger = endringBarnetilleggInnholdBygger;
    }

    @Override
    public ByggerResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return new ByggerResultat(endringHøySatsInnholdBygger, "Automatisk brev ved fødsel av barn.");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholderBare(DetaljertResultatType.ENDRING_BARN_FØDSEL);
    }

}
