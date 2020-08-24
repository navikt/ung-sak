package no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

/**
 * Regel som sjekker om mellomliggende perioder om brukeren er mellom arbeidsforhold.
 * <p>
 * Må være arbeidsforhold som slutter på fredag eller lørdag og starter nytt på mandag. Eller tilsvarende.
 */
@RuleDocumentation("FP_VK_23.1.2")
public class SjekkMellomliggendePerioderMellomArbeidsforhold extends LeafSpecification<MellomregningOpptjeningsvilkårData> {

    public static final String ID = SjekkMellomliggendePerioderMellomArbeidsforhold.class.getSimpleName();
    private static final String ARBEID = Opptjeningsvilkår.ARBEID;
    private static final String MELLOM_ARBEID = Opptjeningsvilkår.MELLOM_ARBEID;

    public SjekkMellomliggendePerioderMellomArbeidsforhold() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MellomregningOpptjeningsvilkårData data) {

        SjekkMellomliggende mellomliggende = new SjekkMellomliggende(data.getGrunnlag());
        LocalDateTimeline<Boolean> arbeidsforholdstidslinje = new LocalDateTimeline<>(List.of());
        var arbeidsforholdsTidslinjer = data.getAktivitetTidslinjer(true, true)
            .entrySet()
            .stream()
            .filter(e -> ARBEID.equals(e.getKey().getAktivitetType()))
            .map(Entry::getValue)
            .collect(Collectors.toList());

        for (LocalDateTimeline<Boolean> linje : arbeidsforholdsTidslinjer) {
            arbeidsforholdstidslinje = arbeidsforholdstidslinje.combine(linje, this::mergePerioder, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        mellomliggende.sjekkMellomliggende(arbeidsforholdstidslinje.compress());

        Evaluation evaluation = ja();
        data.setAkseptertMellomliggendePerioder(mellomliggende.getAkseptertMellomliggendePerioder());
        evaluation.setEvaluationProperty(Opptjeningsvilkår.EVAL_RESULT_AKSEPTERT_MELLOMLIGGENDE_PERIODE,
            mellomliggende.getAkseptertMellomliggendePerioder());

        return evaluation;
    }

    private LocalDateSegment<Boolean> mergePerioder(LocalDateInterval di, LocalDateSegment<Boolean> førsteVersjon, LocalDateSegment<Boolean> sisteVersjon) {

        if ((førsteVersjon == null || førsteVersjon.getValue() == null) && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if ((sisteVersjon == null || sisteVersjon.getValue() == null) && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }

        var første = førsteVersjon.getValue();
        var siste = sisteVersjon.getValue();
        if (første) {
            return lagSegment(di, første);
        } else if (siste) {
            return lagSegment(di, siste);
        } else {
            return lagSegment(di, første);
        }
    }

    private LocalDateSegment<Boolean> lagSegment(LocalDateInterval di, Boolean siste) {
        if (siste == null) {
            return new LocalDateSegment<>(di, null);
        }
        return new LocalDateSegment<>(di, siste);
    }

    /**
     * Implementerer algoritme for sammenligne mellomliggende perioder.
     */
    static class SjekkMellomliggende {

        private final Map<Aktivitet, LocalDateTimeline<Boolean>> akseptertMellomliggendePerioder = new HashMap<>();
        private Opptjeningsgrunnlag grunnlag;

        SjekkMellomliggende(Opptjeningsgrunnlag grunnlag) {
            this.grunnlag = grunnlag;
        }

        Map<Aktivitet, LocalDateTimeline<Boolean>> getAkseptertMellomliggendePerioder() {
            return akseptertMellomliggendePerioder;
        }

        void sjekkMellomliggende(LocalDateTimeline<Boolean> e) {
            // compress for å sikre at vi slipper å sjekke sammenhengende segmenter med samme verdi
            LocalDateTimeline<Boolean> timeline = e.compress();

            LocalDateTimeline<Boolean> mellomliggendePeriode = timeline.collect(this::toPeriod, true).mapValue(v -> Boolean.TRUE);
            if (!mellomliggendePeriode.isEmpty()) {
                akseptertMellomliggendePerioder.put(new Aktivitet(MELLOM_ARBEID), mellomliggendePeriode);
            }
        }

        private boolean toPeriod(@SuppressWarnings("unused") NavigableSet<LocalDateSegment<Boolean>> segmenterFør, // NOSONAR
                                 LocalDateSegment<Boolean> segmentUnderVurdering,
                                 NavigableSet<LocalDateSegment<Boolean>> foregåendeSegmenter,
                                 NavigableSet<LocalDateSegment<Boolean>> påfølgendeSegmenter) {

            if (foregåendeSegmenter.isEmpty() || påfølgendeSegmenter.isEmpty()) {
                // mellomliggende segmenter har ingen verdi, så skipper de som har
                return false;
            } else {
                LocalDateSegment<Boolean> foregående = foregåendeSegmenter.last();
                LocalDateSegment<Boolean> påfølgende = påfølgendeSegmenter.first();


                return segmentUnderVurdering.getValue() == null && Boolean.TRUE.equals(foregående.getValue()) && Boolean.TRUE.equals(påfølgende.getValue())
                    && erKantIKantPåTversAvHelg(foregående.getLocalDateInterval(), påfølgende.getLocalDateInterval());
            }
        }

        boolean erKantIKantPåTversAvHelg(LocalDateInterval periode1, LocalDateInterval periode2) {
            return utledTomDato(periode1).equals(utledFom(periode2).minusDays(1)) || utledTomDato(periode2).equals(utledFom(periode1).minusDays(1));
        }

        private LocalDate utledFom(LocalDateInterval periode1) {
            var fomDato = periode1.getFomDato();
            if (DayOfWeek.SATURDAY.equals(fomDato.getDayOfWeek())) {
                return fomDato.plusDays(2);
            } else if (DayOfWeek.SUNDAY.equals(fomDato.getDayOfWeek())) {
                return fomDato.plusDays(1);
            }
            return fomDato;
        }

        private LocalDate utledTomDato(LocalDateInterval periode1) {
            var tomDato = periode1.getTomDato();
            if (DayOfWeek.FRIDAY.equals(tomDato.getDayOfWeek())) {
                return tomDato.plusDays(2);
            } else if (DayOfWeek.SATURDAY.equals(tomDato.getDayOfWeek())) {
                return tomDato.plusDays(1);
            }
            return tomDato;
        }
    }
}
