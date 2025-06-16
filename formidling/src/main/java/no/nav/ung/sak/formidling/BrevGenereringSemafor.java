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
        try {
            long t0 = System.nanoTime();
            SEMAFOR.acquire();
            long t1 = System.nanoTime();
            long ventetidMillis = (t1 - t0) / 1_000_000;
            if (ventetidMillis > 2000) {
                LOG.warn("Ventet i {} ms på å få semafor for brevbestilling, skjer dette ofte bør semaforens antall og nodens minne økes for bedre ytelse", ventetidMillis);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            return supplier.get();
        } finally {
            SEMAFOR.release();
        }

    }

}
