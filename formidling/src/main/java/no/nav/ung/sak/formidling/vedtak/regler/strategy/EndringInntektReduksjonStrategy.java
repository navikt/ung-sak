package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektReduksjonInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

@Dependent
public final class EndringInntektReduksjonStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringRapportertInntektReduksjonInnholdBygger endringRapportertInntektReduksjonInnholdBygger;

    @Inject
    public EndringInntektReduksjonStrategy(EndringRapportertInntektReduksjonInnholdBygger endringRapportertInntektReduksjonInnholdBygger) {
        this.endringRapportertInntektReduksjonInnholdBygger = endringRapportertInntektReduksjonInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        boolean harUtførtKontrollerInntekt = behandling.getAksjonspunkter().stream()
            .filter(Aksjonspunkt::erUtført)
            .anyMatch(it -> it.getAksjonspunktDefinisjon() == AksjonspunktDefinisjon.KONTROLLER_INNTEKT);

        var forklaring = "Automatisk brev ved endring av rapportert inntekt.";
        if (harUtførtKontrollerInntekt) {
            return medRedigerbarKontrollerInntektBrev(forklaring);
        }

        return VedtaksbrevStrategyResultat.medUredigerbarBrev(DokumentMalType.ENDRING_INNTEKT,
            endringRapportertInntektReduksjonInnholdBygger,
            forklaring);
    }

    private VedtaksbrevStrategyResultat medRedigerbarKontrollerInntektBrev(String forklaring) {
        forklaring += " Kan redigere pga ap=" + AksjonspunktDefinisjon.KONTROLLER_INNTEKT.getKode() + ".";
        return new VedtaksbrevStrategyResultat(
            DokumentMalType.ENDRING_INNTEKT,
            endringRapportertInntektReduksjonInnholdBygger,
            new VedtaksbrevEgenskaper(
                false,
                false,
                true,
                true),
            null,
            forklaring
        );
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
