package no.nav.ung.sak.ytelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Tjeneste for oppretting og uthenting av kontrollerte perioder for rapportert inntekt
 * <p>
 * Perioder med kontrollert inntekt styrer hvilke perioder som det lages tilkjent ytelse perioder for og hvilke perioder som sendes over til økonomi.
 */
@Dependent
public class KontrollerteInntektperioderTjeneste {

    private TilkjentYtelseRepository tilkjentYtelseRepository;


    @Inject
    public KontrollerteInntektperioderTjeneste(TilkjentYtelseRepository tilkjentYtelseRepository) {
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    public void opprettKontrollerteInntekterPerioder(Long behandlingId, LocalDateTimeline<Set<RapportertInntekt>> inntektTidslinje, LocalDateTimeline<Set<BehandlingÅrsakType>> prosesstriggerTidslinje) {
        final var relevantePerioderForKontroll = prosesstriggerTidslinje.filterValue(it -> it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        final var kontrollertePerioder = relevantePerioderForKontroll.combine(inntektTidslinje, lagTomListeForIngenInntekter(), LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .toSegments().stream().map(
                s -> KontrollertInntektPeriode.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
                    .medArbeidsinntekt(finnInntektAvType(s, InntektType.ARBEIDSTAKER_ELLER_FRILANSER))
                    .medYtelse(finnInntektAvType(s, InntektType.YTELSE))
                    .build()
            ).toList();


        tilkjentYtelseRepository.lagre(behandlingId, kontrollertePerioder);


    }


    public LocalDateTimeline<Set<RapportertInntekt>> hentTidslinje(Long behandlingId) {
        return tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandlingId)
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .map(p -> new LocalDateTimeline<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), Set.of(
                new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, p.getArbeidsinntekt()),
                new RapportertInntekt(InntektType.YTELSE, p.getYtelse())
            ))).reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    private static LocalDateSegmentCombinator<Set<BehandlingÅrsakType>, Set<RapportertInntekt>, Set<RapportertInntekt>> lagTomListeForIngenInntekter() {
        return (di, lhs, rhs) -> rhs != null ? new LocalDateSegment<>(di, Set.of()) : rhs;
    }

    private static BigDecimal finnInntektAvType(LocalDateSegment<Set<RapportertInntekt>> s, InntektType inntektType) {
        return s.getValue().stream()
            .filter(it -> it.inntektType().equals(inntektType))
            .map(RapportertInntekt::beløp)
            .reduce(BigDecimal::add)
            .orElse(null);
    }

}
