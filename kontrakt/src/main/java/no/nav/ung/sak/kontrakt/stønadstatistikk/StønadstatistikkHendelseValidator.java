package no.nav.ung.sak.kontrakt.stønadstatistikk;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.AktivitetspengerStønadstatistikkHendelse;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadsstatistikkSatsPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkHendelse;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkSatsType;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.UngStønadstatistikkHendelse;

import java.util.Objects;
import java.util.Set;

public class StønadstatistikkHendelseValidator {

    private static final Validator JAKARTA_VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    public void valider(StønadstatistikkHendelse hendelse) {
        Set<ConstraintViolation<StønadstatistikkHendelse>> constraintViolations = JAKARTA_VALIDATOR.validate(hendelse);
        if (!constraintViolations.isEmpty()) {
            throw new IllegalArgumentException("Ugyldig stønadsstatistikkHendelse: " + String.join("\n", constraintViolations.stream().map(cv -> cv.getPropertyPath() + " " + cv.getMessage()).toList()));
        }

        switch (hendelse) {
            case UngStønadstatistikkHendelse ung -> validerUngdomsprogramytelse(ung);
            case AktivitetspengerStønadstatistikkHendelse aktivitetspenger -> validerAktivitetspenger(aktivitetspenger);
        }
    }

    private void validerUngdomsprogramytelse(UngStønadstatistikkHendelse hendelse) {
        for (StønadsstatistikkSatsPeriode satsPeriode : hendelse.satsPerioder()) {
            krevNotNull(satsPeriode.grunnbeløpFaktor(), "grunnbeløpFaktor");
        }
    }

    private void validerAktivitetspenger(AktivitetspengerStønadstatistikkHendelse hendelse) {
        for (StønadsstatistikkSatsPeriode satsPeriode : hendelse.satsPerioder()) {
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
