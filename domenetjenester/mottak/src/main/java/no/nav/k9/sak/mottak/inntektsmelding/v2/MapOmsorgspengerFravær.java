package no.nav.k9.sak.mottak.inntektsmelding.v2;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBElement;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.sak.typer.PeriodeAndel;
import no.seres.xsd.nav.inntektsmelding_m._20181211.DelvisFravaer;
import no.seres.xsd.nav.inntektsmelding_m._20181211.DelvisFravaersListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.FravaersPeriodeListe;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Omsorgspenger;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Periode;

public class MapOmsorgspengerFravær {

    private Omsorgspenger omsorgspenger;

    public MapOmsorgspengerFravær(Omsorgspenger omsorgspenger) {
        this.omsorgspenger = omsorgspenger;
    }

    private LocalDateTimeline<Duration> getFraværDeldager() {
        Stream<DelvisFravaer> fravær = Optional.ofNullable(omsorgspenger.getDelvisFravaersListe())
            .map(JAXBElement::getValue)
            .map(DelvisFravaersListe::getDelvisFravaer)
            .map(List<DelvisFravaer>::stream)
            .orElse(Stream.empty());

        List<LocalDateSegment<Duration>> segmenter = fravær
            .map(f -> new LocalDateSegment<Duration>(f.getDato().getValue(), f.getDato().getValue(), PeriodeAndel.toDuration(f.getTimer().getValue())))
            .collect(Collectors.toList());
        return new LocalDateTimeline<Duration>(segmenter);
    }

    private LocalDateTimeline<Duration> getFraværHeledager() {
        Stream<Periode> fravær = Optional.ofNullable(omsorgspenger.getFravaersPerioder())
            .map(JAXBElement::getValue)
            .map(FravaersPeriodeListe::getFravaerPeriode)
            .map(List<Periode>::stream)
            .orElse(Stream.empty());

        List<LocalDateSegment<Duration>> segmenter = fravær
            .map(f -> new LocalDateSegment<Duration>(f.getFom().getValue(), f.getTom().getValue(), null))
            .collect(Collectors.toList());
        return new LocalDateTimeline<Duration>(segmenter);
    }

    public List<PeriodeAndel> getAndeler() {
        var fraværTimeline = getFraværHeledager();
        var delvisFraværTimeline = getFraværDeldager();
        var timeline = fraværTimeline.combine(delvisFraværTimeline, this::summerVarighetPerDag, JoinStyle.CROSS_JOIN);
        return timeline.toSegments().stream().map(s -> new PeriodeAndel(s.getFom(), s.getTom(), s.getValue())).collect(Collectors.toList());
    }

    private LocalDateSegment<Duration> summerVarighetPerDag(LocalDateInterval di, LocalDateSegment<Duration> lhs, LocalDateSegment<Duration> rhs) {
        if (lhs == null && rhs != null) {
            return new LocalDateSegment<>(di, rhs.getValue());
        } else if (rhs == null && lhs != null) {
            return new LocalDateSegment<>(di, lhs.getValue());
        } else {
            if (lhs.getValue() == null || rhs.getValue() == null) {
                return new LocalDateSegment<>(di, null);
            } else {
                var dur = lhs.getValue().plus(rhs.getValue());
                return new LocalDateSegment<>(di, dur);
            }
        }
    }

}
