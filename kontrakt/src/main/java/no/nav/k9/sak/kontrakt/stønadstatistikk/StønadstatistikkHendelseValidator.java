package no.nav.k9.sak.kontrakt.stønadstatistikk;

import java.util.Objects;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkHendelse;
import no.nav.k9.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkPeriode;

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
        if (stønadstatistikkHendelse.getPleietrengende() != null) {
            throw new IllegalArgumentException("Skal ikke sette pleietrengende for OMP");
        }
        if (!stønadstatistikkHendelse.getDiagnosekoder().isEmpty()) {
            throw new IllegalArgumentException("Skal ikke sette diagnosekoder for OMP");
        }

        for (StønadstatistikkPeriode periode : stønadstatistikkHendelse.getPerioder()) {
            validerPeriodeOmp(periode);
        }
    }

    private void validerPeriodeOmp(StønadstatistikkPeriode periode) {
        krevNull(periode.getSøkersTapteArbeidstid(), "SøkersTapteArbeidstid");
        krevNull(periode.getOppgittTilsyn(), "OppgittTilsyn");
        krevNull(periode.getPleiebehov(), "Pleiebehov");
        krevNull(periode.getGraderingMotTilsyn(), "GraderingMotTilsyn");
        krevNull(periode.getNattevåk(), "Nattevåk");
        krevNull(periode.getBeredskap(), "Beredskap");
        krevNull(periode.getSøkersTapteTimer(), "SøkersTapteTimer");
    }

    private void validerPleiepenger(StønadstatistikkHendelse stønadstatistikkHendelse) {
        for (StønadstatistikkPeriode periode : stønadstatistikkHendelse.getPerioder()) {
            validerPeriodePleiepenger(periode);
        }
    }

    private void validerPeriodePleiepenger(StønadstatistikkPeriode periode) {
        krevNotNull(periode.getSøkersTapteArbeidstid(), "SøkersTapteArbeidstid");
        krevNotNull(periode.getOppgittTilsyn(), "OppgittTilsyn");
        krevNotNull(periode.getPleiebehov(), "Pleiebehov");
        krevNotNull(periode.getUtfall(), "Utfall");
        krevNotNull(periode.getUttaksgrad(), "Uttaksgrad");
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
