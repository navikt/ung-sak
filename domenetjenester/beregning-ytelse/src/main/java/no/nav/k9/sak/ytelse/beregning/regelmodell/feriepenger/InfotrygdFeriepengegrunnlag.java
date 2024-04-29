package no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.SaksnummerOgSisteBehandling;

public class InfotrygdFeriepengegrunnlag {

    private List<InfotrygdFeriepengegrunnlagAndel> feriepengegrunnlag;

    public InfotrygdFeriepengegrunnlag(List<InfotrygdFeriepengegrunnlagAndel> feriepengegrunnlag) {
        Objects.requireNonNull(feriepengegrunnlag);
        this.feriepengegrunnlag = feriepengegrunnlag;
    }

    public record InfotrygdFeriepengegrunnlagAndel(
        LocalDateInterval periode,
        Saksnummer saksnummer,
        Arbeidsgiver arbeidsgiver,
        BigDecimal dagsatsBruker,
        BigDecimal dagsatsRefusjon) {
    }


    public LocalDateTimeline<Boolean> dagerFeriepengerFraInfotrygd() {
        return TidslinjeUtil.begrensTilAntallDager(grunnlagTidslinje(), 60, false);
    }

    public LocalDateTimeline<BigDecimal> tidslinjeDirekteutbetaling() {
        List<LocalDateSegment<BigDecimal>> segmenter = feriepengegrunnlag.stream()
            .map(andel -> new LocalDateSegment<>(andel.periode, andel.dagsatsBruker))
            .toList();
        LocalDateTimeline<BigDecimal> totalDagsats = new LocalDateTimeline<>(segmenter, (intervall, lhs, rhs) -> new LocalDateSegment<>(intervall, lhs.getValue().add(rhs.getValue())));
        return totalDagsats.filterValue(v -> v.signum() != 0);
    }

    public Map<Arbeidsgiver, LocalDateTimeline<BigDecimal>> tidslinjerRefusjon() {
        Map<Arbeidsgiver, List<InfotrygdFeriepengegrunnlagAndel>> andelerPrArbeidsgiver = feriepengegrunnlag.stream()
            .filter(andel -> andel.dagsatsRefusjon.signum() != 0)
            .collect(Collectors.groupingBy(andel -> andel.arbeidsgiver));

        Map<Arbeidsgiver, LocalDateTimeline<BigDecimal>> resultat = new LinkedHashMap<>();
        for (Map.Entry<Arbeidsgiver, List<InfotrygdFeriepengegrunnlagAndel>> entry : andelerPrArbeidsgiver.entrySet()) {
            List<LocalDateSegment<BigDecimal>> segmenter = entry.getValue().stream()
                .map(andel -> new LocalDateSegment<>(andel.periode, andel.dagsatsRefusjon))
                .toList();
            resultat.put(entry.getKey(), new LocalDateTimeline<>(segmenter, (intervall, lhs, rhs) -> new LocalDateSegment<>(intervall, lhs.getValue().add(rhs.getValue()))));
        }
        return resultat;
    }

    public static LocalDateTimeline<BigDecimal> tilFeriepengerTidslinje(LocalDateTimeline<BigDecimal> dagsatsTidslinje, LocalDateTimeline<Boolean> innenforKvote) {
        BigDecimal feriepengeSats = new BigDecimal("0.102");
        return dagsatsTidslinje
            .mapValue(feriepengeSats::multiply)
            .intersection(innenforKvote);
    }

    public LocalDateTimeline<Boolean> grunnlagTidslinje() {
        List<LocalDateSegment<Boolean>> segmenter = feriepengegrunnlag.stream()
            .filter(andel -> andel.dagsatsBruker.signum() != 0 || andel.dagsatsRefusjon.signum() != 0)
            .map(andel -> new LocalDateSegment<>(andel.periode, true))
            .toList();
        return new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch);
    }


    public LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> saksnummerTidslinje() {
        long dummyInfotrygdBehandlingId = 0;
        List<LocalDateSegment<Set<SaksnummerOgSisteBehandling>>> segmenter = feriepengegrunnlag.stream()
            .map(y -> new LocalDateSegment<>(y.periode, Set.of(new SaksnummerOgSisteBehandling(y.saksnummer, dummyInfotrygdBehandlingId))))
            .toList();
        return new LocalDateTimeline<>(segmenter, StandardCombinators::union);
    }
}
