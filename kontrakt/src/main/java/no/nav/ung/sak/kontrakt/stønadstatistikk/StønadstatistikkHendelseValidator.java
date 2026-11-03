package no.nav.ung.sak.kontrakt.stønadstatistikk;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadsstatistikkSatsPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkHendelse;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkSatsType;

import java.util.Objects;
import java.util.Set;

public class StønadstatistikkHendelseValidator {

    private static final Validator JAKARTA_VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    public void valider(StønadstatistikkHendelse stønadstatistikkHendelse) {

        Set<ConstraintViolation<StønadstatistikkHendelse>> constraintViolations = JAKARTA_VALIDATOR.validate(stønadstatistikkHendelse);
        if (!constraintViolations.isEmpty()) {
            throw new IllegalArgumentException("Ugyldig stønadsstatistikkHendelse: " + String.join("\n", constraintViolations.stream().map(cv -> cv.getPropertyPath() + " " + cv.getMessage()).toList()));
        }

        switch (stønadstatistikkHendelse.ytelseType()) {
            case UNGDOMSYTELSE -> validerUngdomsprogramytelse(stønadstatistikkHendelse);
            case AKTIVITETSPENGER -> validerAktivitetspenger(stønadstatistikkHendelse);
            default ->
                throw new IllegalArgumentException("Ikke-støttet ytelsetype:  " + stønadstatistikkHendelse.ytelseType());
        }
    }

    private void validerUngdomsprogramytelse(StønadstatistikkHendelse stønadstatistikkHendelse) {
        for (StønadsstatistikkSatsPeriode satsPeriode : stønadstatistikkHendelse.satsPerioder()) {
            if (satsPeriode.satsType() == StønadstatistikkSatsType.BEREGNINGSGRUNNLAG) {
                throw new IllegalArgumentException("Satstype " + satsPeriode.satsType() + " er ikke støttet for ungdomsprogramytelsen");
            }
            krevNotNull(satsPeriode.grunnbeløpFaktor(), "grunnbeløpFaktor");
        }
    }

    private void validerAktivitetspenger(StønadstatistikkHendelse stønadstatistikkHendelse) {
        krevNull(stønadstatistikkHendelse.ungdomsprogramDeltakelsePeriode(), "ungdomsprogramDeltakelsePeriode");
        for (StønadsstatistikkSatsPeriode satsPeriode : stønadstatistikkHendelse.satsPerioder()) {
            if (satsPeriode.satsType() == StønadstatistikkSatsType.BEREGNINGSGRUNNLAG) {
                krevNull(satsPeriode.grunnbeløpFaktor(), "grunnbeløpFaktor");
            } else {
                krevNotNull(satsPeriode.grunnbeløpFaktor(), "grunnbeløpFaktor");
            }
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
