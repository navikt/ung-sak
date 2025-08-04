package no.nav.ung.sak.formidling.vedtak.regler;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.EndringHøySatsInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

@Dependent
public final class EndringHøySatsStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringHøySatsInnholdBygger endringHøySatsInnholdBygger;

    @Inject
    public EndringHøySatsStrategy(EndringHøySatsInnholdBygger endringHøySatsInnholdBygger) {
        this.endringHøySatsInnholdBygger = endringHøySatsInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return new VedtaksbrevStrategyResultat(DokumentMalType.ENDRING_HØY_SATS, endringHøySatsInnholdBygger, "Automatisk brev ved endring til høy sats.", null);
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholderBare(DetaljertResultatType.ENDRING_ØKT_SATS);
    }

}
