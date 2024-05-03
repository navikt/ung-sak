package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.domene.typer.tid.IntervallUtil;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.MottakerType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.FeriepengekorrigeringInfotrygd;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.InfotrygdFeriepengegrunnlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Period;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class BeregnFerietilleggDagpenger extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {

    public static final String ID = "FP_BR 8.11";
    public static final String BESKRIVELSE = "Beregn ferietillegg for dagpenger.";

    private static final BigDecimal FERIEPENGER_SATS = BigDecimal.valueOf(0.095);

    private static final Logger logger = LoggerFactory.getLogger(BeregnFerietilleggDagpenger.class);

    BeregnFerietilleggDagpenger() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        var tilkjentytelseSegmenter = regelModell.getBeregningsresultatPerioder().stream().map(this::lagDagpengeSegment).toList();
        var tilkjentytelseTidslinje = new LocalDateTimeline<>(tilkjentytelseSegmenter);
        var tidslinjeSplittetPåÅr = splittPåÅr(tilkjentytelseTidslinje);

        Map<String, Object> regelsporing = new LinkedHashMap<>();

        return beregnet(regelsporing);
    }

    private LocalDateTimeline<Long> splittPåÅr(LocalDateTimeline<Long> tilkjentytelseTidslinje) {
        var førsteDagIFørsteÅrMedYtelse = tilkjentytelseTidslinje.getMinLocalDate().withDayOfYear(1);
        return tilkjentytelseTidslinje.splitAtRegular(førsteDagIFørsteÅrMedYtelse, tilkjentytelseTidslinje.getMaxLocalDate(), Period.ofYears(1));
    }

    private LocalDateSegment<Long> lagDagpengeSegment(BeregningsresultatPeriode p) {
        var dagsatsFraDagpengerIPerioden = p.getBeregningsresultatAndelList().stream()
            .filter(a -> Inntektskategori.DAGPENGER.equals(a.getInntektskategori()))
            .map(BeregningsresultatAndel::getDagsats)
            .reduce(Long::sum)
            .orElse(0L);
        return new LocalDateSegment<>(p.getFom(), p.getTom(), dagsatsFraDagpengerIPerioden);
    }



}
