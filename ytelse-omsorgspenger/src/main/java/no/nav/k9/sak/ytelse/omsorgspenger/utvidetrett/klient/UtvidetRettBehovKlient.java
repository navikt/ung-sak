package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient;

import no.nav.k9.sak.ytelse.omsorgspenger.behov.BehovKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.KroniskSyktBarn;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.MidlertidigAlene;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.UtvidetRett;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class UtvidetRettBehovKlient implements UtvidetRettKlient {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static final Map<Class<? extends UtvidetRett>, String> INNVILGET_BEHOV_NAVN = Map.of(
        KroniskSyktBarn.class, "InnvilgetKroniskSyktBarn",
        MidlertidigAlene.class, "InnvilgetMidlertidigAlene"
    );

    private static final Map<Class<? extends UtvidetRett>, String> AVSLÅTT_BEHOV_NAVN = Map.of(
        KroniskSyktBarn.class, "AvslåttKroniskSyktBarn",
        MidlertidigAlene.class, "AvslåttMidlertidigAlene"
    );

    private BehovKlient behovKlient;

    private UtvidetRettBehovKlient() {}

    @Inject
    public UtvidetRettBehovKlient(
        BehovKlient behovKlient) {
        this.behovKlient = behovKlient;
    }

    @Override
    public void innvilget(UtvidetRett innvilget) {
        var behovNavn = Optional.ofNullable(INNVILGET_BEHOV_NAVN.get(innvilget.getClass())).orElseThrow(() -> new IllegalStateException(("Støtter ikke " + innvilget.getClass())));
        behovKlient.nyttBehov(behovNavn, validert(innvilget));
    }

    @Override
    public void avslått(UtvidetRett avslått) {
        var behovNavn = Optional.ofNullable(AVSLÅTT_BEHOV_NAVN.get(avslått.getClass())).orElseThrow(() -> new IllegalStateException(("Støtter ikke " + avslått.getClass())));
        behovKlient.nyttBehov(behovNavn, validert(avslått));
    }

    private UtvidetRett validert(UtvidetRett utvidetRett) {
        var violations = VALIDATOR.validate(utvidetRett);
        if (violations.isEmpty()) return utvidetRett;
        else throw new IllegalStateException("Ugyldig vedtak om utvidet rett " + violations);
    }
}
