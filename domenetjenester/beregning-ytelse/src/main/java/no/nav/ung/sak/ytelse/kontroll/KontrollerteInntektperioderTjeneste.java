package no.nav.ung.sak.ytelse.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ytelse.RapportertInntektOgKilde;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;


    @Inject
    public KontrollerteInntektperioderTjeneste(TilkjentYtelseRepository tilkjentYtelseRepository, MånedsvisTidslinjeUtleder ytelsesperiodeutleder, ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder) {
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.ytelsesperiodeutleder = ytelsesperiodeutleder;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
    }

    public void opprettKontrollerteInntekterPerioderFraBruker(Long behandlingId,
                                                              LocalDateInterval vurdertPeriode,
                                                              Inntektsresultat inntektsresultat) {
        final var kontrollertePerioder = mapAutomatiskKontrollerteInntektperioder(new LocalDateTimeline<>(vurdertPeriode, true),
            new LocalDateTimeline<>(vurdertPeriode, new RapportertInntektOgKilde(inntektsresultat.kilde(), inntektsresultat.inntekt())),
            Optional.of(KontrollertInntektKilde.BRUKER)
        );
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
    public void ryddPerioderFritattForKontrollEllerTilVurderingIBehandlingen(Long behandlingId) {
        final var kontrollertInntektPerioder = tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandlingId);
        if (kontrollertInntektPerioder.isEmpty()) {
            return;
        }

        final var ytelseTidslinje = ytelsesperiodeutleder.periodiserMånedsvis(behandlingId);
        final var relevantForKontrollTidslinje = RelevanteKontrollperioderUtleder.utledPerioderRelevantForKontrollAvInntekt(ytelseTidslinje);
        if (relevantForKontrollTidslinje.isEmpty()) {
            tilkjentYtelseRepository.lagre(behandlingId, new ArrayList<>());
        } else {
            final var tidslinjeTilVurdering = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId).filterValue(it -> it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)).mapValue(it -> true);
            final var tidslinjeSomBeholdes = relevantForKontrollTidslinje.disjoint(tidslinjeTilVurdering);
            final var eksisterendePerioder = kontrollertInntektPerioder.get().getPerioder();
            final var perioderSomBeholdes = eksisterendePerioder.stream()
                .filter(it -> !tidslinjeSomBeholdes.intersection(it.getPeriode().toLocalDateInterval()).isEmpty())
                .toList();


            var perioderSomFjernes = eksisterendePerioder.stream()
                .filter(it -> relevantForKontrollTidslinje.intersection(it.getPeriode().toLocalDateInterval()).isEmpty())
                .toList();

            if (!perioderSomFjernes.isEmpty()) {
                LOG.info("Fjerner kontrollerte perioder siden det ikke er påkrevd kontroll for første og siste måned i programperioden. Perioder som beholdes: {}, perioder som fjernes: {}",
                    perioderSomBeholdes.size(), perioderSomFjernes.size());
            }
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

    public void opprettKontrollerteInntekterPerioderEtterManuellVurdering(Long behandlingId, LocalDateTimeline<ManueltKontrollertInntekt> inntektTidslinje) {
        final var kontrollertePerioder = mapManueltKontrollerteInntektperioder(inntektTidslinje);
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

    /**
     * Mapper til kontrollerte inntekter for automatisk vurdering
     *
     * @param vurdertTidslinje Vurdert tidslinje
     * @param inntektTidslinje Rapportert inntekt tidslinje
     * @param defaultKilde     Kilde som skal settes der vi ikke har rapporterte inntekter
     * @return List med kontrollerte perioder
     */
    private static List<KontrollertInntektPeriode> mapAutomatiskKontrollerteInntektperioder(LocalDateTimeline<Boolean> vurdertTidslinje,
                                                                                            LocalDateTimeline<RapportertInntektOgKilde> inntektTidslinje,
                                                                                            Optional<KontrollertInntektKilde> defaultKilde) {

        return vurdertTidslinje.combine(inntektTidslinje, settVerdiForIngenInntekter(defaultKilde), LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .toSegments().stream().map(
                s -> KontrollertInntektPeriode.ny()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
                    .medInntekt(s.getValue().samletInntekt())
                    .medKilde(s.getValue().kilde())
                    .medErManueltVurdert(false)
                    .build()
            ).toList();
    }

    /**
     * Mapper tidslinje for manuelt vurderte data til kontrollerte inntekter
     *
     * @param inntektTidslinje Manuelt kontrollert inntekt tidslinje
     * @return List med kontrollerte perioder
     */
    private static List<KontrollertInntektPeriode> mapManueltKontrollerteInntektperioder(LocalDateTimeline<ManueltKontrollertInntekt> inntektTidslinje) {
        return inntektTidslinje.toSegments().stream().map(
            s -> KontrollertInntektPeriode.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
                .medInntekt(s.getValue().samletInntekt())
                .medKilde(s.getValue().kilde())
                .medErManueltVurdert(true)
                .medBegrunnelse(s.getValue().begrunnelse())
                .build()
        ).toList();
    }

    private static LocalDateSegmentCombinator<Boolean, RapportertInntektOgKilde, RapportertInntektOgKilde> settVerdiForIngenInntekter(Optional<KontrollertInntektKilde> kilde) {
        return (di, lhs, rhs) ->
            rhs == null ?
                new LocalDateSegment<>(di, new RapportertInntektOgKilde(kilde.orElseThrow(() -> new IllegalStateException("Forventer å få default kilde dersom tidslinjen med inntekter ikke dekker alle perioder til vurdering")), BigDecimal.ZERO)) :
                rhs;
    }
}
