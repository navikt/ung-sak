package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektUtenReduksjonInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

import java.math.BigDecimal;

@Dependent
public final class EndringInntektUtenReduksjonStrategy implements VedtaksbrevInnholdbyggerStrategy {


    private final EndringRapportertInntektUtenReduksjonInnholdBygger endringRapportertInntektUtenReduksjonInnholdBygger;
    private final TilkjentYtelseRepository tilkjentYtelseRepository;

    @Inject
    public EndringInntektUtenReduksjonStrategy(EndringRapportertInntektUtenReduksjonInnholdBygger endringRapportertInntektUtenReduksjonInnholdBygger, TilkjentYtelseRepository tilkjentYtelseRepository) {
        this.endringRapportertInntektUtenReduksjonInnholdBygger = endringRapportertInntektUtenReduksjonInnholdBygger;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var kontrollertInntektPerioderTidslinje = hentKontrollertInntektTidslinje(behandling);

        var manueltFastsatt0MedOver0RapportertTidslinje = DetaljertResultat
            .filtererTidslinje(detaljertResultat, DetaljertResultatType.KONTROLLER_INNTEKT_FULL_UTBETALING)
            .combine(kontrollertInntektPerioderTidslinje, StandardCombinators::rightOnly,
                LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .filterValue(it ->
                it.getErManueltVurdert() && it.getRapportertInntekt().compareTo(BigDecimal.ZERO) > 0 && it.getInntekt().compareTo(BigDecimal.ZERO) == 0
            );

        boolean harUtførtKontrollerInntekt = behandling.getAksjonspunkter().stream()
            .filter(Aksjonspunkt::erUtført)
            .anyMatch(it -> it.getAksjonspunktDefinisjon() == AksjonspunktDefinisjon.KONTROLLER_INNTEKT);

        if (harUtførtKontrollerInntekt && !manueltFastsatt0MedOver0RapportertTidslinje.isEmpty()) {
            return new VedtaksbrevStrategyResultat(
                DokumentMalType.ENDRING_INNTEKT_UTEN_REDUKSJON,
                endringRapportertInntektUtenReduksjonInnholdBygger,
                new VedtaksbrevEgenskaper(false,
                    false,
                    true,
                    true),
                null,
                "Redigerbar brev ved full utbetaling med manuelt fastsatt inntekt på 0 kr uten registerinntekt."
            );
        }
        return VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_RELEVANT,"Ingen brev ved full utbetaling etter kontroll av inntekt.");
    }

    private LocalDateTimeline<KontrollertInntektPeriode> hentKontrollertInntektTidslinje(Behandling behandling) {
        return tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandling.getId())
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .map(p -> new LocalDateTimeline<>(
                p.getPeriode().getFomDato(),
                p.getPeriode().getTomDato(),
                p)).reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholderBare(DetaljertResultatType.KONTROLLER_INNTEKT_FULL_UTBETALING);
    }

}
