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
import no.nav.ung.sak.formidling.innhold.EndringInntektUtenReduksjonInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

import java.math.BigDecimal;
import java.util.Optional;

@Dependent
public final class EndringInntektUtenReduksjonStrategy implements VedtaksbrevInnholdbyggerStrategy {


    private final EndringInntektUtenReduksjonInnholdBygger endringInntektUtenReduksjonInnholdBygger;
    private final TilkjentYtelseRepository tilkjentYtelseRepository;

    @Inject
    public EndringInntektUtenReduksjonStrategy(EndringInntektUtenReduksjonInnholdBygger endringInntektUtenReduksjonInnholdBygger, TilkjentYtelseRepository tilkjentYtelseRepository) {
        this.endringInntektUtenReduksjonInnholdBygger = endringInntektUtenReduksjonInnholdBygger;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var kontrollertInntektPerioderTidslinje = hentKontrollertInntektTidslinje(behandling);

        var manueltFastsatt0MedOver0RapportertTidslinje = DetaljertResultat
            .filtererTidslinje(detaljertResultat, DetaljertResultatType.KONTROLLER_INNTEKT_FULL_UTBETALING)
            .combine(kontrollertInntektPerioderTidslinje, StandardCombinators::rightOnly,
                LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .stream()
            .anyMatch(it -> harManuellFastsatt0krMedOver0krRapportert(it.getValue()));

        boolean harUtførtKontrollerInntekt = behandling.getAksjonspunkter().stream()
            .filter(Aksjonspunkt::erUtført)
            .anyMatch(it -> it.getAksjonspunktDefinisjon() == AksjonspunktDefinisjon.KONTROLLER_INNTEKT);

        if (harUtførtKontrollerInntekt && manueltFastsatt0MedOver0RapportertTidslinje) {
            return new VedtaksbrevStrategyResultat(
                DokumentMalType.ENDRING_INNTEKT_UTEN_REDUKSJON,
                endringInntektUtenReduksjonInnholdBygger,
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

    private static boolean harManuellFastsatt0krMedOver0krRapportert(KontrollertInntektPeriode it) {
        boolean harFastSattInntektTil0kr = it.getInntekt().compareTo(BigDecimal.ZERO) == 0;
        boolean harRapporterInntektOver0kr = Optional.ofNullable(it.getRapportertInntekt())
            .map( r -> r.compareTo(BigDecimal.ZERO) > 0)
            .orElse(true); //Tolker ingen verdi som over 0 kr for å la andre sjekker fange opp
        return it.getErManueltVurdert() && harRapporterInntektOver0kr && harFastSattInntektTil0kr;
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
