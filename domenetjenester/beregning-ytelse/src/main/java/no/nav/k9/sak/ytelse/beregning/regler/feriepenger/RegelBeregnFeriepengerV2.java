package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregnetFeriepenger;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

/**
 * Det mangler dokumentasjon
 */

@RuleDocumentation(value = RegelBeregnFeriepengerV2.ID, specificationReference = "https://confluence.adeo.no/display/MODNAV/27c+Beregn+feriepenger+PK-51965+OMR-49")
public class RegelBeregnFeriepengerV2 implements RuleService<BeregningsresultatFeriepengerRegelModell> {

    public static final String ID = "";
    public static final String BESKRIVELSE = "RegelBeregnFeriepenger";

    @Override
    public Evaluation evaluer(BeregningsresultatFeriepengerRegelModell regelmodell) {
        return getSpecification().evaluate(regelmodell);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<BeregningsresultatFeriepengerRegelModell> getSpecification() {
        Ruleset<BeregningsresultatFeriepengerRegelModell> rs = new Ruleset<>();

        // FP_BR 8.6 Beregn feriepenger (Flere kalenderår)
        Specification<BeregningsresultatFeriepengerRegelModell> beregnFeriepenger =
            rs.beregningsRegel(BeregnFeriepengerV2.ID, BeregnFeriepengerV2.BESKRIVELSE, new BeregnFeriepengerV2(), new BeregnetFeriepenger());

        // FP_BR 8.2 Har bruker fått utbetalt ytelse i den totale stønadsperioden?
        Specification<BeregningsresultatFeriepengerRegelModell> sjekkOmBrukerHarFåttUtbetaltYtelse =
            rs.beregningHvisRegel(new SjekkOmYtelseErTilkjent(), beregnFeriepenger, new BeregnetFeriepenger());

        // FP_BR 8.1 Er brukers inntektskategori arbeidstaker eller sjømann?
        Specification<BeregningsresultatFeriepengerRegelModell> sjekkInntektskatoriATellerSjømann =
            rs.beregningHvisRegel(new SjekkOmBrukerHarInntektkategoriATellerSjømann(), sjekkOmBrukerHarFåttUtbetaltYtelse, new BeregnetFeriepenger());

        return sjekkInntektskatoriATellerSjømann;
    }
}
