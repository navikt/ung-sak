package no.nav.k9.sak.ytelse.omsorgspenger.behov;

import no.nav.vedtak.exception.IntegrasjonException;

public class BehovKafkaException extends IntegrasjonException {
    private static final String KODE = "K9-901300";
    BehovKafkaException(String msg, Throwable cause) {
        super(KODE, msg, cause);
    }
}
