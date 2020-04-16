package no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

/**
 * Slår sammen alle gjenværende aktivitet tidslinjer og akseptert mellomliggende perioder til en samlet tidslinje for
 * aktivitet, samt telle totalt antall godkjente perioder
 * <p>
 * Telling av dager for å finne aktiviteter i opptjeningsperioden skal gjøres etter følgende regler:
 * <ul>
 * <li>1 hel kalendermåned = 1 måned med godkjent opptjening</li>
 * <li>Perioder som ikke utgjør 1 hel kalender måned = x antall dager med godkjent
 * opptjening</li>
 * </ul>
 * <p>
 * Følgende legges til grunn for vurdering av opptjeningsvilkåret:
 * <ul>
 * <li>Minst 4 uker / 28 dager opptjening</li>
 * </ul>
 */
@RuleDocumentation(value = "FP_VK_23.1.3")
public class BeregnOpptjening extends LeafSpecification<MellomregningOpptjeningsvilkårData> {
    public static final String ID = BeregnOpptjening.class.getSimpleName();


    protected BeregnOpptjening() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MellomregningOpptjeningsvilkårData data) {

        Evaluation evaluation = ja();

        // Pseudo-beregn "norske" aktiviteter og finn max-dato. underkjenne utlandsk aktivitet i perioden MAX(norsk) - END(opptjeningsperiode)
        if (evaluerEvtUnderkjennUtlandskeAktiviteteter(data)) {
            evaluation.setEvaluationProperty(Opptjeningsvilkår.EVAL_RESULT_UNDERKJENTE_PERIODER, data.getUnderkjentePerioder().toString());
        }

        // beregn bekreftet opptjening
        LocalDateTimeline<Boolean> bekreftetOpptjeningTidslinje = slåSammenTilFellesTidslinje(data, false, Collections.emptyList());

        Period bekreftetOpptjening = beregnTotalOpptjeningPeriode(bekreftetOpptjeningTidslinje);
        data.setBekreftetTotalOpptjening(new OpptjentTidslinje(bekreftetOpptjening, bekreftetOpptjeningTidslinje));
        evaluation.setEvaluationProperty(Opptjeningsvilkår.EVAL_RESULT_BEKREFTET_AKTIVITET_TIDSLINJE, bekreftetOpptjeningTidslinje.toString());
        evaluation.setEvaluationProperty(Opptjeningsvilkår.EVAL_RESULT_BEKREFTET_OPPTJENING, bekreftetOpptjening.toString());

        // beregn inklusiv antatt opptjening
        LocalDateTimeline<Boolean> antattOpptjeningTidslinje = slåSammenTilFellesTidslinje(data, true, Collections.emptyList());
        Period antattOpptjening = beregnTotalOpptjeningPeriode(antattOpptjeningTidslinje);
        data.setAntattOpptjening(new OpptjentTidslinje(antattOpptjening, antattOpptjeningTidslinje));
        // ikke sett evaluation properties for antatt før vi vet vi trenger det. (gjøre ved Sjekk av tilstrekkelig opptjening inklusiv antatt godkjent)

        return evaluation;
    }

    private LocalDateTimeline<Boolean> slåSammenTilFellesTidslinje(MellomregningOpptjeningsvilkårData data, boolean medAntattGodkjent, Collection<Aktivitet> unntak) {
        LocalDateTimeline<Boolean> tidslinje = new LocalDateTimeline<>(Collections.emptyList());

        // slå sammen alle aktivitetperioder til en tidslinje (disse er fratrukket underkjente perioder allerede)
        Map<Aktivitet, LocalDateTimeline<Boolean>> aktivitetTidslinjer = data.getAktivitetTidslinjer(medAntattGodkjent, false);
        for (Map.Entry<Aktivitet, LocalDateTimeline<Boolean>> entry : aktivitetTidslinjer
            .entrySet()) {
            if (!unntak.contains(entry.getKey())) {
                tidslinje = tidslinje.crossJoin(entry.getValue(), StandardCombinators::alwaysTrueForMatch);
            }
        }

        tidslinje = tidslinje.compress(); // minimer tidslinje intervaller

        LocalDateTimeline<Boolean> opptjeningsTidslinje = new LocalDateTimeline<>(data.getGrunnlag().getOpptjeningPeriode(), Boolean.TRUE);

        tidslinje = tidslinje.intersection(opptjeningsTidslinje, StandardCombinators::leftOnly);
        return tidslinje;
    }

    private Period beregnTotalOpptjeningPeriode(LocalDateTimeline<Boolean> tidslinje) {
        if (tidslinje.isEmpty()) {
            return Period.ofDays(0);
        }
        return Period.between(tidslinje.getMinLocalDate(), tidslinje.getMaxLocalDate().plusDays(1));
    }

    private boolean evaluerEvtUnderkjennUtlandskeAktiviteteter(MellomregningOpptjeningsvilkårData data) {
        Aktivitet utlandsFilter = new Aktivitet(Opptjeningsvilkår.UTLAND);

        LocalDateTimeline<Boolean> tidslinje = slåSammenTilFellesTidslinje(data, false, Arrays.asList(utlandsFilter));

        LocalDate maxDatoIkkeUtlandsk = tidslinje.isEmpty() ? data.getGrunnlag().getFørsteDatoIOpptjening().minusDays(1) : tidslinje.getMaxLocalDate();

        // Må overskrive manuell godkjenning da annen aktivitet gjerne er vurdert i aksjonspunkt i steg 82
        return data.splitOgUnderkjennSegmenterEtterDatoForAktivitet(utlandsFilter, maxDatoIkkeUtlandsk);
    }
}
