package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;

import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramOpphørUtleder;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;

import java.time.LocalDate;

public class MaksdatoOpphørVarslingPeriode {

    // Kan vurdere om dette skal vere miljøvariabler, men per no er det ikkje nødvendig med ulik konfigurasjon per miljø for desse
    public static final int VARSEL_UKER_FØR_MAKSDATO = 3;


    public static boolean harPassertVarseldato(LocalDate periodeMaksDato) {
        LocalDate dagensDato = LocalDate.now();
        return !dagensDato.isBefore(periodeMaksDato.minusWeeks(VARSEL_UKER_FØR_MAKSDATO));
    }

    /**
     * @throws IllegalStateException dersom grunnlaget mangler maksdato eller ikke har nøyaktig én programperiode
     */
    public static boolean erRelevantForVarsling(UngdomsprogramPeriodeGrunnlag grunnlag) {
        var maksdato = grunnlag.getPeriodeMaksDato()
            .orElseThrow(() -> new IllegalStateException("Forventer at periodeMaksDato er satt på ungdomsprogramperiodegrunnlaget"));
        return harPassertVarseldato(maksdato) && UngdomsprogramOpphørUtleder.opphørerVedMaksdato(grunnlag);
    }

}
