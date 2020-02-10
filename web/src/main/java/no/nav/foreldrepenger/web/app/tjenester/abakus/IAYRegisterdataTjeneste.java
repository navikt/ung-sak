package no.nav.foreldrepenger.web.app.tjenester.abakus;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.RegisterdataCallback;

@SuppressWarnings("unused")
@ApplicationScoped
public class IAYRegisterdataTjeneste {

    private static final Logger log = LoggerFactory.getLogger(IAYRegisterdataTjeneste.class);

    private InntektArbeidYtelseTjeneste iayTjeneste;

    public IAYRegisterdataTjeneste() {
    }

    /**
     * Standard ctor som injectes av CDI.
     */
    @Inject
    public IAYRegisterdataTjeneste(InntektArbeidYtelseTjeneste iayTjeneste) {
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
    }

    public void håndterCallback(RegisterdataCallback callback) {
        // TODO (Frode C.) - fortsett prosess
    }
}