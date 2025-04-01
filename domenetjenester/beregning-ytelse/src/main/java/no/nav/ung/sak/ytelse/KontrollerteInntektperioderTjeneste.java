package no.nav.ung.sak.ytelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPerioder;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ytelseperioder.YtelseperiodeUtleder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

/**
 * Tjeneste for oppretting og uthenting av kontrollerte perioder for rapportert inntekt
 * <p>
 * Perioder med kontrollert inntekt styrer hvilke perioder som det lages tilkjent ytelse perioder for og hvilke perioder som sendes over til økonomi.
 */
@Dependent
public class KontrollerteInntektperioderTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(KontrollerteInntektperioderTjeneste.class);
    private final TilkjentYtelseRepository tilkjentYtelseRepository;
    private final YtelseperiodeUtleder ytelseperiodeUtleder;


    @Inject
    public KontrollerteInntektperioderTjeneste(TilkjentYtelseRepository tilkjentYtelseRepository, YtelseperiodeUtleder ytelseperiodeUtleder) {
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.ytelseperiodeUtleder = ytelseperiodeUtleder;
    }

    public void opprettKontrollerteInntekterPerioderFraBruker(Long behandlingId, LocalDateTimeline<Set<RapportertInntekt>> inntektTidslinje, LocalDateTimeline<Set<BehandlingÅrsakType>> prosesstriggerTidslinje) {
        final var relevantePerioderForKontroll = prosesstriggerTidslinje.filterValue(it -> it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        final var kontrollertePerioder = mapTilKontrollerteInntektperioder(inntektTidslinje.mapValue(it -> new RapportertInntektOgKilde(KontrollertInntektKilde.BRUKER, it)), relevantePerioderForKontroll, Optional.of(KontrollertInntektKilde.BRUKER), false);
        LOG.info("Lagrer inntekt fra bruker: {}", kontrollertePerioder);


        final var allePerioder = utvidEksisterendePerioder(behandlingId, kontrollertePerioder);

        tilkjentYtelseRepository.lagre(behandlingId, allePerioder);
    }

    private ArrayList<KontrollertInntektPeriode> utvidEksisterendePerioder(Long behandlingId, List<KontrollertInntektPeriode> nyePerioder) {
        final var eksisterende = tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandlingId);

        final var eksisterendePerioderSomSkalBeholdes = eksisterende.stream()
            .flatMap(it -> it.getPerioder().stream())
            .filter(p -> nyePerioder.stream().map(KontrollertInntektPeriode::getPeriode).noneMatch(p2 -> p.getPeriode().overlapper(p2)))
            .map(KontrollertInntektPeriode::new).toList();
        final var allePerioder = new ArrayList<KontrollertInntektPeriode>();
        allePerioder.addAll(eksisterendePerioderSomSkalBeholdes);
        allePerioder.addAll(nyePerioder);
        return allePerioder;
    }

    public void opprettKontrollerteInntekterPerioderFraEtterManuellVurdering(Long behandlingId, LocalDateTimeline<RapportertInntektOgKilde> inntektTidslinje, LocalDateTimeline<Set<BehandlingÅrsakType>> prosesstriggerTidslinje) {
        final var relevantePerioderForKontroll = prosesstriggerTidslinje.filterValue(it -> it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        final var kontrollertePerioder = mapTilKontrollerteInntektperioder(inntektTidslinje, relevantePerioderForKontroll, Optional.empty(), true);
        tilkjentYtelseRepository.lagre(behandlingId, utvidEksisterendePerioder(behandlingId, kontrollertePerioder));
    }

    public void gjenopprettTilOriginal(Long originalBehandlingId, Long behandlingId) {
        tilkjentYtelseRepository.gjenopprettTilOriginal(originalBehandlingId, behandlingId);
    }

    public LocalDateTimeline<Set<RapportertInntekt>> hentTidslinje(Long behandlingId) {
        return tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandlingId)
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .map(p -> {
                Set<RapportertInntekt> rapportertInntekter = new HashSet<>();
                if (p.getArbeidsinntekt() != null) {
                    rapportertInntekter.add(new RapportertInntekt(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, p.getArbeidsinntekt()));
                }
                if (p.getYtelse() != null) {
                    rapportertInntekter.add(new RapportertInntekt(InntektType.YTELSE, p.getYtelse()));
                }
                return new LocalDateTimeline<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), rapportertInntekter);
            }).reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    private static List<KontrollertInntektPeriode> mapTilKontrollerteInntektperioder(LocalDateTimeline<RapportertInntektOgKilde> inntektTidslinje,
                                                                                     LocalDateTimeline<Set<BehandlingÅrsakType>> relevantePerioderForKontroll,
                                                                                     Optional<KontrollertInntektKilde> defaultKilde,
                                                                                     boolean erManueltVurdert) {
        return relevantePerioderForKontroll.combine(inntektTidslinje, lagTomListeForIngenInntekter(defaultKilde), LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .toSegments().stream().map(
                s -> KontrollertInntektPeriode.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
                    .medArbeidsinntekt(finnInntektAvType(s.getValue().rapporterteInntekter(), InntektType.ARBEIDSTAKER_ELLER_FRILANSER))
                    .medYtelse(finnInntektAvType(s.getValue().rapporterteInntekter(), InntektType.YTELSE))
                    .medKilde(s.getValue().kilde())
                    .medErManueltVurdert(erManueltVurdert)
                    .build()
            ).toList();
    }

    private static LocalDateSegmentCombinator<Set<BehandlingÅrsakType>, RapportertInntektOgKilde, RapportertInntektOgKilde> lagTomListeForIngenInntekter(Optional<KontrollertInntektKilde> kilde) {
        return (di, lhs, rhs) -> rhs == null ? new LocalDateSegment<>(di, new RapportertInntektOgKilde(kilde.orElseThrow(() -> new IllegalStateException("Forventer å få default kilde dersom tidslinjen med inntekter ikke dekker alle perioder til vurdering")), Set.of())) : rhs;
    }

    private static BigDecimal finnInntektAvType(Set<RapportertInntekt> s, InntektType inntektType) {
        return s.stream()
            .filter(it -> it.inntektType().equals(inntektType))
            .map(RapportertInntekt::beløp)
            .reduce(BigDecimal::add)
            .orElse(null);
    }

    public void ryddMotYtelsetidslinje(Long behandlingId) {
        final var kontrollertInntektPerioder = tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandlingId);
        if (kontrollertInntektPerioder.isEmpty()) {
            return;
        }

        final var ytelseTidslinje = ytelseperiodeUtleder.utledYtelsestidslinje(behandlingId);
        final var relevantForKontrollTidslinje = RelevanteKontrollperioderUtleder.utledPerioderRelevantForKontrollAvInntekt(ytelseTidslinje);
        if (relevantForKontrollTidslinje.isEmpty()) {
            tilkjentYtelseRepository.lagre(behandlingId, new ArrayList<>());
        } else {
            final var eksisterendePerioder = kontrollertInntektPerioder.get().getPerioder();
            final var perioderSomBeholdes = eksisterendePerioder.stream()
                .filter(it -> !relevantForKontrollTidslinje.intersection(it.getPeriode().toLocalDateInterval()).isEmpty())
                .toList();
            tilkjentYtelseRepository.lagre(behandlingId, perioderSomBeholdes);
        }


    }
}
