package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
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
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.formidling.vedtak.resultat.Inntektskontroll;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.EndringInntektReduksjonInnholdBygger;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.EndringInntektUtenReduksjonInnholdBygger;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
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
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        var detaljertResultat = resultatTidslinje.tilVurdering();
        boolean harReduksjon = Inntektskontroll.harInntektReduksjon(detaljertResultat);
        boolean harFullUtbetaling = Inntektskontroll.harInntektFullUtbetaling(detaljertResultat);

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
            VedtaksbrevEgenskaper.builder()
                .kanHindre(false)
                .kanOverstyreHindre(false)
                .kanRedigere(true)
                .kanOverstyreRediger(true)
                .build(),
            null,
            forklaring
        );
    }

    private VedtaksbrevStrategyResultat fullUtbetalingResultat(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        if (harManueltFastsattInntekt(behandling, detaljertResultat)) {
            return new VedtaksbrevStrategyResultat(
                DokumentMalType.ENDRING_INNTEKT_UTEN_REDUKSJON,
                endringInntektUtenReduksjonInnholdBygger,
                VedtaksbrevEgenskaper.builder()
                    .kanHindre(false)
                    .kanOverstyreHindre(false)
                    .kanRedigere(true)
                    .kanOverstyreRediger(true)
                    .build(),
                null,
                "Redigerbar brev ved full utbetaling med manuelt fastsatt inntekt på 0 kr uten registerinntekt."
            );
        }
        return VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_RELEVANT, "Ingen brev ved full utbetaling etter kontroll av inntekt.");
    }

    private boolean harManueltFastsattInntekt(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var fullUtbetaling = Inntektskontroll.fullUtbetalingTidslinje(detaljertResultat);
        return tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandling.getId())
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .filter(EndringInntektStrategy::harManuellFastsatt0kr)
            .anyMatch(it -> !fullUtbetaling.intersection(tilIntervall(it)).isEmpty());
    }

    private static LocalDateInterval tilIntervall(KontrollertInntektPeriode kontrollertInntektPeriode) {
        return new LocalDateInterval(
            kontrollertInntektPeriode.getPeriode().getFomDato(),
            kontrollertInntektPeriode.getPeriode().getTomDato());
    }

    private static boolean harManuellFastsatt0kr(KontrollertInntektPeriode kontrollertInntektPeriode) {
        boolean harFastsattInntektTil0kr = kontrollertInntektPeriode.getInntekt().compareTo(BigDecimal.ZERO) == 0;
        return kontrollertInntektPeriode.getErManueltVurdert() && harFastsattInntektTil0kr;
    }

}
