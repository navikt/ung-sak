package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TomVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

@Dependent
public final class AvslagInngangsvilkår implements VedtaksbrevInnholdbyggerStrategy {


    private final TomVedtaksbrevInnholdBygger tomVedtaksbrevInnholdBygger;

    @Inject
    public AvslagInngangsvilkår(TomVedtaksbrevInnholdBygger tomVedtaksbrevInnholdBygger) {
        this.tomVedtaksbrevInnholdBygger = tomVedtaksbrevInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return new VedtaksbrevStrategyResultat(
            DokumentMalType.MANUELT_VEDTAK_DOK,
            tomVedtaksbrevInnholdBygger,
            new VedtaksbrevEgenskaper(true, true, true, true),
            null,
            "Tom brev for redigering ved avslag"
        );
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholderBare(DetaljertResultatType.AVSLAG_INNGANGSVILKÅR);
    }

}
