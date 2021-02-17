package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient;

import no.nav.vedtak.exception.IntegrasjonException;

public class UtvidetRettRestException extends IntegrasjonException {

    public UtvidetRettRestException(String kode, String msg, Throwable t) {
        super(kode, msg, t);
    }

}
