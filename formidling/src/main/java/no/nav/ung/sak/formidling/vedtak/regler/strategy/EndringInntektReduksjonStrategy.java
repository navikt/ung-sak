package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
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
        boolean harUtførtKontrollerInntekt = behandling.getAksjonspunkterMedTotrinnskontroll().stream()
            .filter(Aksjonspunkt::erUtført)
            .anyMatch(it -> it.getAksjonspunktDefinisjon() == AksjonspunktDefinisjon.KONTROLLER_INNTEKT);

        var forklaring = "Automatisk brev ved endring av rapportert inntekt. ";
        if (harUtførtKontrollerInntekt) {
            forklaring += "Kan redigere pga ap " + AksjonspunktDefinisjon.KONTROLLER_INNTEKT.getKode() + ".";
        }
        return VedtaksbrevStrategyResultat.medBrev(DokumentMalType.ENDRING_INNTEKT,
            harUtførtKontrollerInntekt,
            endringRapportertInntektInnholdBygger,
            forklaring);
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholderIkke(DetaljertResultatType.INNVILGELSE_UTBETALING)
            && (resultater.innholder(DetaljertResultatType.KONTROLLER_INNTEKT_REDUKSJON)
            || resultater.innholder(DetaljertResultatType.KONTROLLER_INNTEKT_INGEN_UTBETALING)
        );
    }

}
