package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;

import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.AleneOmsorg;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.KroniskSyktBarn;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.MidlertidigAlene;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.UtvidetRett;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.omsorgsdager.RapidsBehovKlient;

@ApplicationScoped
public class UtvidetRettBehovKlient implements UtvidetRettKlient {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static final Map<Class<? extends UtvidetRett>, String> INNVILGET_BEHOV_NAVN = Map.of(
        KroniskSyktBarn.class, "InnvilgetKroniskSyktBarn",
        MidlertidigAlene.class, "InnvilgetMidlertidigAlene",
        AleneOmsorg.class, "InnvilgetAleneOmsorg"
    );

    private static final Map<Class<? extends UtvidetRett>, String> AVSLÅTT_BEHOV_NAVN = Map.of(
        KroniskSyktBarn.class, "AvslåttKroniskSyktBarn",
        MidlertidigAlene.class, "AvslåttMidlertidigAlene",
        AleneOmsorg.class, "AvslåttAleneOmsorg"
    );

    private RapidsBehovKlient rapidsBehovKlient;

    UtvidetRettBehovKlient() {}

    @Inject
    public UtvidetRettBehovKlient(
        RapidsBehovKlient rapidsBehovKlient) {
        this.rapidsBehovKlient = rapidsBehovKlient;
    }

    @Override
    public void innvilget(UtvidetRett innvilget) {
        var behovNavn = Optional.ofNullable(INNVILGET_BEHOV_NAVN.get(innvilget.getClass())).orElseThrow(() -> new IllegalStateException(("Støtter ikke " + innvilget.getClass())));
        rapidsBehovKlient.nyttBehov(behovNavn, validert(innvilget));
    }

    @Override
    public void avslått(UtvidetRett avslått) {
        var behovNavn = Optional.ofNullable(AVSLÅTT_BEHOV_NAVN.get(avslått.getClass())).orElseThrow(() -> new IllegalStateException(("Støtter ikke " + avslått.getClass())));
        rapidsBehovKlient.nyttBehov(behovNavn, validert(avslått));
    }

    private UtvidetRett validert(UtvidetRett utvidetRett) {
        var violations = VALIDATOR.validate(utvidetRett);
        if (violations.isEmpty()) return utvidetRett;
        else throw new IllegalStateException("Ugyldig vedtak om utvidet rett " + violations);
    }
}
