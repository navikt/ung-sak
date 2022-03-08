package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.omsorgsdager;

import no.nav.k9.felles.exception.IntegrasjonException;

public class RapidsBehovKafkaException extends IntegrasjonException {
    private static final String KODE = "K9-901300";
    RapidsBehovKafkaException(String msg, Throwable cause) {
        super(KODE, msg, cause);
    }
}
