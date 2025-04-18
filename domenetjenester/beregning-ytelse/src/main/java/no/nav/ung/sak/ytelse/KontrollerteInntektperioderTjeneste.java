package no.nav.ung.sak.ytelse;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Tjeneste for oppretting og uthenting av kontrollerte perioder for rapportert inntekt
 * <p>
 * Perioder med kontrollert inntekt styrer hvilke perioder som det lages tilkjent ytelse perioder for og hvilke perioder som sendes over til økonomi.
 */
@Dependent
public class KontrollerteInntektperioderTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(KontrollerteInntektperioderTjeneste.class);
    private final TilkjentYtelseRepository tilkjentYtelseRepository;
    private final MånedsvisTidslinjeUtleder ytelsesperiodeutleder;


    @Inject
    public KontrollerteInntektperioderTjeneste(TilkjentYtelseRepository tilkjentYtelseRepository, MånedsvisTidslinjeUtleder ytelsesperiodeutleder) {
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.ytelsesperiodeutleder = ytelsesperiodeutleder;
    }

    public void opprettKontrollerteInntekterPerioderFraBruker(Long behandlingId,
                                                              LocalDateInterval vurdertPeriode,
                                                              LocalDateTimeline<Set<RapportertInntekt>> inntektTidslinje) {
        final var kontrollertePerioder = mapTilKontrollerteInntektperioder(new LocalDateTimeline<>(vurdertPeriode, true),
            inntektTidslinje.mapValue(it -> new RapportertInntektOgKilde(KontrollertInntektKilde.BRUKER, summerRapporterteInntekter(it))),
            Optional.of(KontrollertInntektKilde.BRUKER),
            false);
        LOG.info("Lagrer inntekt fra bruker: {}", kontrollertePerioder);


        final var allePerioder = utvidEksisterendePerioder(behandlingId, kontrollertePerioder);

        tilkjentYtelseRepository.lagre(behandlingId, allePerioder);
    }

    /**
     * Det er ikke påkrevd med kontroll av inntekt for første og siste måned.
     * Dersom programperioden har endret seg fjernes allerede kontrollerte perioder dersom første og siste måned for programmet er endret.
     * Dette for å unngå at utbetaling reduseres i disse månedene og potensielt også reduseres basert på feilaktig inntekt.
     *
     * @param behandlingId BehandlingId
     */
    public void ryddPerioderFritattForKontroll(Long behandlingId) {
        final var kontrollertInntektPerioder = tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandlingId);
        if (kontrollertInntektPerioder.isEmpty()) {
            return;
        }

        final var ytelseTidslinje = ytelsesperiodeutleder.periodiserMånedsvis(behandlingId);
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

    public void opprettKontrollerteInntekterPerioderEtterManuellVurdering(Long behandlingId, LocalDateTimeline<RapportertInntektOgKilde> inntektTidslinje) {
        final var kontrollertePerioder = mapTilKontrollerteInntektperioder(inntektTidslinje.mapValue(it -> true), inntektTidslinje, Optional.empty(), true);
        tilkjentYtelseRepository.lagre(behandlingId, utvidEksisterendePerioder(behandlingId, kontrollertePerioder));
    }

    public void gjenopprettTilOriginal(Long originalBehandlingId, Long behandlingId) {
        tilkjentYtelseRepository.gjenopprettTilOriginal(originalBehandlingId, behandlingId);
    }

    public LocalDateTimeline<BigDecimal> hentTidslinje(Long behandlingId) {
        return tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandlingId)
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .map(p -> new LocalDateTimeline<>(
                p.getPeriode().getFomDato(),
                p.getPeriode().getTomDato(),
                p.getInntekt())).reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    private static List<KontrollertInntektPeriode> mapTilKontrollerteInntektperioder(LocalDateTimeline<Boolean> vurdertTidslinje,
                                                                                     LocalDateTimeline<RapportertInntektOgKilde> inntektTidslinje,
                                                                                     Optional<KontrollertInntektKilde> defaultKilde,
                                                                                     boolean erManueltVurdert) {

        return vurdertTidslinje.combine(inntektTidslinje, settVerdiForIngenInntekter(defaultKilde), LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .toSegments().stream().map(
                s -> KontrollertInntektPeriode.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
                    .medInntekt(s.getValue().samletInntekt())
                    .medKilde(s.getValue().kilde())
                    .medErManueltVurdert(erManueltVurdert)
                    .build()
            ).toList();
    }

    private static BigDecimal summerRapporterteInntekter(Set<RapportertInntekt> rapportertInntekts) {
        return rapportertInntekts.stream().map(RapportertInntekt::beløp).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    private static LocalDateSegmentCombinator<Boolean, RapportertInntektOgKilde, RapportertInntektOgKilde> settVerdiForIngenInntekter(Optional<KontrollertInntektKilde> kilde) {
        return (di, lhs, rhs) ->
            rhs == null ?
            new LocalDateSegment<>(di, new RapportertInntektOgKilde(kilde.orElseThrow(() -> new IllegalStateException("Forventer å få default kilde dersom tidslinjen med inntekter ikke dekker alle perioder til vurdering")), BigDecimal.ZERO)) :
                rhs;
    }
}
