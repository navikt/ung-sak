package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

class FinnFeriepengePerioder extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.3";
    public static final String BESKRIVELSE = "Finner brukers og arbeidsgivers feriepengeperiode.";

    FinnFeriepengePerioder() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        var regelmodellBuilder = BeregningsresultatFeriepengerRegelModell.builder(regelModell);
        var regelsporingData = new LinkedHashMap<String, Object>();

        {
            LocalDate feriepengePeriodeFom = finnFørsteUttaksdag(regelModell);
            LocalDate feriepengePeriodeTom = finnFeriepengerPeriodeTomForBruker(regelModell, feriepengePeriodeFom);
            regelmodellBuilder.medFeriepengerPeriode(feriepengePeriodeFom, feriepengePeriodeTom);
            regelsporingData.put("FeriepengePeriode.bruker.fom", feriepengePeriodeFom);
            regelsporingData.put("FeriepengePeriode.bruker.tom", feriepengePeriodeTom);
        }

        {
            LocalDate feriepengePeriodeFom = finnFørsteUttaksdag(regelModell);
            LocalDate feriepengePeriodeTom = finnFeriepengerPeriodeTomVedRefusjon(regelModell, feriepengePeriodeFom);
            regelmodellBuilder.medFeriepengerPeriodeRefusjon(feriepengePeriodeFom, feriepengePeriodeTom);
            regelsporingData.put("FeriepengePeriode.refusjon.fom", feriepengePeriodeFom);
            regelsporingData.put("FeriepengePeriode.refusjon.tom", feriepengePeriodeTom);
        }
        return beregnet(regelsporingData);
    }

    private LocalDate finnFeriepengerPeriodeTomForBruker(BeregningsresultatFeriepengerRegelModell regelModell, LocalDate feriepengePeriodeFom) {
        UttakSjekk uttakFilter = regelModell.harUbegrensetFeriepengedagerVedRefusjon() ? UttakSjekk.SJEKK_KUN_ANDEL_BRUKER : UttakSjekk.DEFAULT;
        return finnFeriepengerPeriodeTom(regelModell, feriepengePeriodeFom, uttakFilter);
    }

    private LocalDate finnFeriepengerPeriodeTomVedRefusjon(BeregningsresultatFeriepengerRegelModell regelModell, LocalDate feriepengePeriodeFom) {
        return regelModell.harUbegrensetFeriepengedagerVedRefusjon() ? finnSisteUttaksdag(regelModell) : finnFeriepengerPeriodeTom(regelModell, feriepengePeriodeFom, UttakSjekk.DEFAULT);
    }

    private LocalDate finnFeriepengerPeriodeTom(BeregningsresultatFeriepengerRegelModell regelModell, LocalDate feriepengePeriodeFom, UttakSjekk uttakFilter) {
        List<BeregningsresultatPeriode> beregningsresultatPerioder = regelModell.getBeregningsresultatPerioder();
        int maksAntallDager = antallDagerFeriepenger(regelModell.getAntallDagerFeriepenger());
        LocalDate sisteUttaksdag = finnSisteUttaksdag(beregningsresultatPerioder);
        int antallDager = 0;

        for (LocalDate dato = feriepengePeriodeFom; !dato.isAfter(sisteUttaksdag); dato = dato.plusDays(1)) {
            int antallDagerSomLeggesTilFeriepengeperioden = finnAntallDagerSomSkalLeggesTil(beregningsresultatPerioder, dato, regelModell.harFeriepengeopptjeningFoHelg(), uttakFilter);
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

    private int finnAntallDagerSomSkalLeggesTil(List<BeregningsresultatPeriode> beregningsresultatPerioder, LocalDate dato, boolean harFeriepengeopptjeningForHelg, UttakSjekk uttakFilter) {
        if (erHelg(dato) && !harFeriepengeopptjeningForHelg) {
            return 0;
        }
        return harUttak(beregningsresultatPerioder, dato, uttakFilter) ? 1 : 0;
    }

    private enum UttakSjekk {
        SJEKK_KUN_ANDEL_BRUKER,
        DEFAULT
    }

    private boolean harUttak(List<BeregningsresultatPeriode> beregningsresultatPerioder, LocalDate dato, UttakSjekk uttakFilter) {
        return beregningsresultatPerioder.stream().filter(p -> p.inneholder(dato))
            .flatMap(beregningsresultatPeriode -> beregningsresultatPeriode.getBeregningsresultatAndelList().stream())
            .filter(andel -> uttakFilter == UttakSjekk.DEFAULT || andel.erBrukerMottaker())
            .anyMatch(andel -> andel.getDagsats() > 0);
    }

    private boolean erHelg(LocalDate dato) {
        return dato.getDayOfWeek().getValue() > DayOfWeek.FRIDAY.getValue();
    }

    private LocalDate finnFørsteUttaksdag(BeregningsresultatFeriepengerRegelModell regelModell) {
        return finnFørsteUttaksdag(regelModell.getBeregningsresultatPerioder());
    }

    private LocalDate finnFørsteUttaksdag(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        return beregningsresultatPerioder.stream()
            .filter(periode -> periode.getBeregningsresultatAndelList().stream().anyMatch(andel -> andel.getDagsats() > 0))
            .map(BeregningsresultatPeriode::getFom)
            .min(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalStateException("Fant ingen perioder med utbetaling for bruker"));
    }

    private LocalDate finnSisteUttaksdag(BeregningsresultatFeriepengerRegelModell regelModell) {
        return finnSisteUttaksdag(regelModell.getBeregningsresultatPerioder());
    }

    private LocalDate finnSisteUttaksdag(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        return beregningsresultatPerioder.stream()
            .filter(periode -> periode.getBeregningsresultatAndelList().stream().anyMatch(andel -> andel.getDagsats() > 0))
            .map(BeregningsresultatPeriode::getTom)
            .max(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalStateException("Fant ingen perioder med utbetaling for bruker"));
    }
}
