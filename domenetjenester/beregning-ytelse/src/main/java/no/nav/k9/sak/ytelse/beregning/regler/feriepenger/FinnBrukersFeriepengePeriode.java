package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

class FinnBrukersFeriepengePeriode extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.3";
    public static final String BESKRIVELSE = "Finner brukers feriepengeperiode.";

    FinnBrukersFeriepengePeriode() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        List<BeregningsresultatPeriode> beregningsresultatPerioder = regelModell.getBeregningsresultatPerioder();
        LocalDate feriepengePeriodeFom = finnFørsteUttaksdag(beregningsresultatPerioder);
        LocalDate feriepengePeriodeTom = finnFeriepengerPeriodeTom(regelModell, feriepengePeriodeFom);

        BeregningsresultatFeriepengerRegelModell.builder(regelModell)
            .medFeriepengerPeriode(feriepengePeriodeFom, feriepengePeriodeTom);

        // Regelsporing
        Map<String, Object> resultater = new LinkedHashMap<>();
        resultater.put("FeriepengePeriode.fom", feriepengePeriodeFom);
        resultater.put("FeriepengePeriode.tom", feriepengePeriodeTom);
        return beregnet(resultater);
    }

    private LocalDate finnFeriepengerPeriodeTom(BeregningsresultatFeriepengerRegelModell regelModell, LocalDate feriepengePeriodeFom) {
        List<BeregningsresultatPeriode> beregningsresultatPerioder = regelModell.getBeregningsresultatPerioder();
        int maksAntallDager = antallDagerFeriepenger(regelModell.getAntallDagerFeriepenger());
        LocalDate sisteUttaksdag = finnSisteUttaksdag(beregningsresultatPerioder);
        int antallDager = 0;

        for (LocalDate dato = feriepengePeriodeFom; !dato.isAfter(sisteUttaksdag); dato = dato.plusDays(1)) {
            int antallDagerSomLeggesTilFeriepengeperioden = finnAntallDagerSomSkalLeggesTil(beregningsresultatPerioder, dato, regelModell.harFeriepengeopptjeningFoHelg());
            antallDager += antallDagerSomLeggesTilFeriepengeperioden;
            if (antallDager == maksAntallDager) {
                return dato;
            }
            if (antallDager > maksAntallDager) {
                return dato;
            }
        }
        return sisteUttaksdag;
    }

    private int antallDagerFeriepenger(int antallDagerFeriepenger) {
        return antallDagerFeriepenger;
    }

    private int finnAntallDagerSomSkalLeggesTil(List<BeregningsresultatPeriode> beregningsresultatPerioder, LocalDate dato, boolean harFeriepengeopptjeningForHelg) {
        if (erHelg(dato) && !harFeriepengeopptjeningForHelg) {
            return 0;
        }
        return harUttak(beregningsresultatPerioder, dato) ? 1 : 0;
    }

    private boolean harUttak(List<BeregningsresultatPeriode> beregningsresultatPerioder, LocalDate dato) {
        return beregningsresultatPerioder.stream().filter(p -> p.inneholder(dato))
            .flatMap(beregningsresultatPeriode -> beregningsresultatPeriode.getBeregningsresultatAndelList().stream())
            .anyMatch(andel -> andel.getDagsats() > 0);
    }

    private boolean erHelg(LocalDate dato) {
        return dato.getDayOfWeek().getValue() > DayOfWeek.FRIDAY.getValue();
    }

    private LocalDate finnFørsteUttaksdag(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        return beregningsresultatPerioder.stream()
            .filter(periode -> periode.getBeregningsresultatAndelList().stream().anyMatch(andel -> andel.getDagsats() > 0))
            .map(BeregningsresultatPeriode::getFom)
            .min(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalStateException("Fant ingen perioder med utbetaling for bruker"));
    }

    private LocalDate finnSisteUttaksdag(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        return beregningsresultatPerioder.stream()
            .filter(periode -> periode.getBeregningsresultatAndelList().stream().anyMatch(andel -> andel.getDagsats() > 0))
            .map(BeregningsresultatPeriode::getTom)
            .max(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalStateException("Fant ingen perioder med utbetaling for bruker"));
    }
}
