package no.nav.ung.sak.kontrakt.stønadstatistikk;

import java.util.Objects;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkHendelse;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkUtbetalingsgrad;

public class StønadstatistikkHendelseValidator {

    private static final Validator JAKARTA_VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    public void valider(StønadstatistikkHendelse stønadstatistikkHendelse) {

        Set<ConstraintViolation<StønadstatistikkHendelse>> constraintViolations = JAKARTA_VALIDATOR.validate(stønadstatistikkHendelse);
        if (!constraintViolations.isEmpty()) {
            throw new IllegalArgumentException("Ugyldig stønadsstatistikkHendelse: " + String.join("\n", constraintViolations.stream().map(cv -> cv.getPropertyPath() + " " + cv.getMessage()).toList()));
        }

        switch (stønadstatistikkHendelse.getYtelseType()) {
            case PLEIEPENGER_NÆRSTÅENDE, PLEIEPENGER_SYKT_BARN -> validerPleiepenger(stønadstatistikkHendelse);
            case OMSORGSPENGER -> validerOmsorgspenger(stønadstatistikkHendelse);
            default ->
                throw new IllegalArgumentException("Ikke-støttet ytelsetype:  " + stønadstatistikkHendelse.getYtelseType());
        }
    }

    private void validerOmsorgspenger(StønadstatistikkHendelse stønadstatistikkHendelse) {
        if (!stønadstatistikkHendelse.getDiagnosekoder().isEmpty()) {
            throw new IllegalArgumentException("Skal ikke sette diagnosekoder for OMP");
        }

        for (StønadstatistikkPeriode periode : stønadstatistikkHendelse.getPerioder()) {
            validerPeriodeOmp(periode);
        }
    }

    private void validerPeriodeOmp(StønadstatistikkPeriode periode) {
        validerUtbetalingsgraderOmp(periode);
        krevNull(periode.getSøkersTapteArbeidstid(), "SøkersTapteArbeidstid");
        krevNull(periode.getOppgittTilsyn(), "OppgittTilsyn");
        krevNull(periode.getPleiebehov(), "Pleiebehov");
        krevNull(periode.getGraderingMotTilsyn(), "GraderingMotTilsyn");
        krevNull(periode.getNattevåk(), "Nattevåk");
        krevNull(periode.getBeredskap(), "Beredskap");
        krevNull(periode.getSøkersTapteTimer(), "SøkersTapteTimer");
    }

    private static void validerUtbetalingsgraderOmp(StønadstatistikkPeriode periode) {
        for (StønadstatistikkUtbetalingsgrad utbetalingsgrad : periode.getUtbetalingsgrader()) {
            validerUtbetalingsgradOmp(utbetalingsgrad);
        }
    }

    private static void validerUtbetalingsgradOmp(StønadstatistikkUtbetalingsgrad utbetalingsgrad) {
        //har ikke data for disse på OMP
        krevNull(utbetalingsgrad.getNormalArbeidstid(), "NormalArbeidstid");
        krevNull(utbetalingsgrad.getFaktiskArbeidstid(), "FaktiskArbeidstid");
    }

    private void validerPleiepenger(StønadstatistikkHendelse stønadstatistikkHendelse) {
        for (StønadstatistikkPeriode periode : stønadstatistikkHendelse.getPerioder()) {
            validerPeriodePleiepenger(periode);
        }
    }

    private void validerPeriodePleiepenger(StønadstatistikkPeriode periode) {
        validerUtbetalingsgraderPleiepenger(periode);
        krevNotNull(periode.getSøkersTapteArbeidstid(), "SøkersTapteArbeidstid");
        krevNotNull(periode.getOppgittTilsyn(), "OppgittTilsyn");
        krevNotNull(periode.getPleiebehov(), "Pleiebehov");
        krevNotNull(periode.getUtfall(), "Utfall");
        krevNotNull(periode.getUttaksgrad(), "Uttaksgrad");
    }

    private static void validerUtbetalingsgraderPleiepenger(StønadstatistikkPeriode periode) {
        for (StønadstatistikkUtbetalingsgrad utbetalingsgrad : periode.getUtbetalingsgrader()) {
            krevNotNull(utbetalingsgrad.getNormalArbeidstid(), "NormalArbeidstid");
            krevNotNull(utbetalingsgrad.getFaktiskArbeidstid(), "FaktiskArbeidstid");
        }
    }


    private static void krevNull(Object object, String felt) {
        if (object != null) {
            throw new IllegalArgumentException(felt + " skal være null");
        }
    }

    private static void krevNotNull(Object object, String felt) {
        Objects.requireNonNull(object, felt);
    }

}
