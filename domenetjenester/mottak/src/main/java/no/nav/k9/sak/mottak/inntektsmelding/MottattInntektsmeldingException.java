package no.nav.k9.sak.mottak.inntektsmelding;

import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.feil.Feil;

public class MottattInntektsmeldingException extends TekniskException {

    public MottattInntektsmeldingException(Feil feil) {
        super(feil);
    }

}
