package no.nav.ung.sak.formidling;

import no.nav.k9.felles.konfigurasjon.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

public class BrevGenereringSemafor {

    private static final Logger LOG = LoggerFactory.getLogger(BrevGenereringSemafor.class);

    private static final Integer MAX_SAMTIDIGE = Environment.current().getProperty("BREVGENERERING_MAX_ANTALL_SAMTIDIGE", Integer.class, 2);
    private static final Semaphore SEMAFOR = new Semaphore(MAX_SAMTIDIGE);

    private BrevGenereringSemafor() {
    }

    public static <T> T begrensetParallellitet(Supplier<T> supplier) {
        boolean fikkSemafor = SEMAFOR.tryAcquire();
        if (!fikkSemafor) {
            throw new BrevGenereringSemaforIkkeTilgjengeligException();
        }
        try {
            return supplier.get();
        } finally {
            SEMAFOR.release();
        }

    }

    public static class BrevGenereringSemaforIkkeTilgjengeligException extends RuntimeException {
        public BrevGenereringSemaforIkkeTilgjengeligException() {
            super("Alle semaforer for brevgenerering er opptatt, prøv igjen senere");
        }
    }

}
