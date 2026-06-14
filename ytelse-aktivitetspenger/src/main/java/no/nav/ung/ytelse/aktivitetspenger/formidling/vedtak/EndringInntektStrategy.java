package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.EndringInntektReduksjonInnholdBygger;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.EndringInntektUtenReduksjonInnholdBygger;

import java.math.BigDecimal;
import java.util.List;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class EndringInntektStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final EndringInntektReduksjonInnholdBygger endringInntektReduksjonInnholdBygger;
    private final EndringInntektUtenReduksjonInnholdBygger endringInntektUtenReduksjonInnholdBygger;
    private final TilkjentYtelseRepository tilkjentYtelseRepository;

    @Inject
    public EndringInntektStrategy(
        EndringInntektReduksjonInnholdBygger endringInntektReduksjonInnholdBygger,
        EndringInntektUtenReduksjonInnholdBygger endringInntektUtenReduksjonInnholdBygger,
        TilkjentYtelseRepository tilkjentYtelseRepository) {
        this.endringInntektReduksjonInnholdBygger = endringInntektReduksjonInnholdBygger;
        this.endringInntektUtenReduksjonInnholdBygger = endringInntektUtenReduksjonInnholdBygger;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultater = new ResultatHelper(VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat));
        boolean harReduksjon = resultater.innholder(DetaljertResultatType.KONTROLLER_INNTEKT_REDUKSJON)
            || resultater.innholder(DetaljertResultatType.KONTROLLER_INNTEKT_INGEN_UTBETALING);
        boolean harFullUtbetaling = resultater.innholder(DetaljertResultatType.KONTROLLER_INNTEKT_FULL_UTBETALING);

        if (harReduksjon) {
            return List.of(reduksjonResultat(behandling));
        }
        if (harFullUtbetaling) {
            return List.of(fullUtbetalingResultat(behandling, detaljertResultat));
        }
        return List.of();
    }

    private VedtaksbrevStrategyResultat reduksjonResultat(Behandling behandling) {
        boolean harUtførtKontrollerInntekt = behandling.getAksjonspunkter().stream()
            .filter(Aksjonspunkt::erUtført)
            .anyMatch(it -> it.getAksjonspunktDefinisjon() == AksjonspunktDefinisjon.KONTROLLER_INNTEKT);

        var forklaring = "Automatisk brev ved endring av inntekt.";
        if (harUtførtKontrollerInntekt) {
            return medRedigerbarKontrollerInntektBrev(forklaring);
        }

        return VedtaksbrevStrategyResultat.medUredigerbarBrev(DokumentMalType.ENDRING_INNTEKT,
            endringInntektReduksjonInnholdBygger,
            forklaring);
    }

    private VedtaksbrevStrategyResultat medRedigerbarKontrollerInntektBrev(String forklaring) {
        forklaring += " Kan redigere pga ap=" + AksjonspunktDefinisjon.KONTROLLER_INNTEKT.getKode() + ".";
        return new VedtaksbrevStrategyResultat(
            DokumentMalType.ENDRING_INNTEKT,
            endringInntektReduksjonInnholdBygger,
            new VedtaksbrevEgenskaper(
                false,
                false,
                true,
                true),
            null,
            forklaring
        );
    }

    private VedtaksbrevStrategyResultat fullUtbetalingResultat(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var kontrollertInntektPerioderTidslinje = hentKontrollertInntektTidslinje(behandling);

        var harManueltFastsattInntekt = harManueltFastsattInntekt(detaljertResultat, kontrollertInntektPerioderTidslinje);

        if (harManueltFastsattInntekt) {
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
        return VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_RELEVANT, "Ingen brev ved full utbetaling etter kontroll av inntekt.");
    }

    private boolean harManueltFastsattInntekt(
        LocalDateTimeline<DetaljertResultat> detaljertResultat,
        LocalDateTimeline<KontrollertInntektPeriode> kontrollertInntektPerioderTidslinje) {

        return DetaljertResultat
            .filtererTidslinje(detaljertResultat, DetaljertResultatType.KONTROLLER_INNTEKT_FULL_UTBETALING)
            .combine(kontrollertInntektPerioderTidslinje, StandardCombinators::rightOnly,
                LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .stream()
            .anyMatch(it -> harManuellFastsatt0kr(it.getValue()));
    }


    private static boolean harManuellFastsatt0kr(KontrollertInntektPeriode it) {
        boolean harFastSattInntektTil0kr = it.getInntekt().compareTo(BigDecimal.ZERO) == 0;
        return it.getErManueltVurdert() && harFastSattInntektTil0kr;
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

}
