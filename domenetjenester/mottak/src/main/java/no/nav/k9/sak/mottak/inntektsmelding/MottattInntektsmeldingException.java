package no.nav.k9.sak.mottak.inntektsmelding;

import no.nav.k9.sak.mottak.dokumentmottak.DokumentValideringException;
import no.nav.vedtak.feil.Feil;

public class MottattInntektsmeldingException extends DokumentValideringException {

    public MottattInntektsmeldingException(Feil feil) {
        super(feil);
    }

}
