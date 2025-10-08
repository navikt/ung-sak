package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektUtenReduksjonInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

@Dependent
public final class EndringInntektUtenReduksjonStrategy implements VedtaksbrevInnholdbyggerStrategy {


    private final EndringRapportertInntektUtenReduksjonInnholdBygger endringRapportertInntektUtenReduksjonInnholdBygger;

    @Inject
    public EndringInntektUtenReduksjonStrategy(EndringRapportertInntektUtenReduksjonInnholdBygger endringRapportertInntektUtenReduksjonInnholdBygger) {
        this.endringRapportertInntektUtenReduksjonInnholdBygger = endringRapportertInntektUtenReduksjonInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        boolean harUtførtKontrollerInntekt = behandling.getAksjonspunkter().stream()
            .filter(Aksjonspunkt::erUtført)
            .anyMatch(it -> it.getAksjonspunktDefinisjon() == AksjonspunktDefinisjon.KONTROLLER_INNTEKT);

        if (harUtførtKontrollerInntekt) {
            return new VedtaksbrevStrategyResultat(
                DokumentMalType.ENDRING_INNTEKT_UTEN_REDUKSJON,
                endringRapportertInntektUtenReduksjonInnholdBygger,
                new VedtaksbrevEgenskaper(false,
                    false,
                    true,
                    true),
                null,
                " Redigerbar brev ved full utbetaling med ap=" + AksjonspunktDefinisjon.KONTROLLER_INNTEKT.getKode() + "."
            );
        }
        return VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_RELEVANT,"Ingen brev ved full utbetaling etter kontroll av inntekt.");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholderBare(DetaljertResultatType.KONTROLLER_INNTEKT_FULL_UTBETALING);
    }

}
