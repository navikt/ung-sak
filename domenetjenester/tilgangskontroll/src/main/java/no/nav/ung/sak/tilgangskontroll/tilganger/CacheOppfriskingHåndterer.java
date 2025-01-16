package no.nav.ung.sak.tilgangskontroll.tilganger;

import no.nav.k9.felles.util.LRUCache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Brukes for å unngå at bare en tråd gjør operasjoner (eksempelvis eksternt kall) for å oppdatere en verdi i cachen,
 * resterende tråder som trenger samme verdi vil blokkere, og hente verdi fra cache når første tråd er ferdig.
 * Når tråder ser på ulike verdier, kan trådene oppfriske uavhengig av hverandre
 *
 */
public class CacheOppfriskingHåndterer {
    private static final ReentrantLock låsForMap = new ReentrantLock();
    private static final Map<Object, ReentrantLock> låsPerNøkkel = new HashMap<>();

    private CacheOppfriskingHåndterer() {
    }

    public static <K extends Comparable<?>, V> V hentOmIkkeICache(K key, LRUCache<K, V> cache, Function<K, V> supplier) {
        V cachetVerdi = cache.get(key);
        if (cachetVerdi != null) {
            return cachetVerdi;
        }

        ReentrantLock lås = finnLåsForHentingAvVerdiForNøkkel(key);

        V verdi = hentVerdiOppdaterCache(key, cache, supplier, lås);

        fjernLåsForHentingAvVerdiForNøkkel(key);

        return verdi;
    }

    private static <K extends Comparable<?>, V> V hentVerdiOppdaterCache(K key, LRUCache<K, V> cache, Function<K, V> supplier, ReentrantLock lås) {
        lås.lock();
        try {
            V cachetVerdi = cache.get(key);
            if (cachetVerdi != null) {
                return cachetVerdi;
            }
            V hentetVerdi = supplier.apply(key);
            cache.put(key, hentetVerdi);
            return hentetVerdi;
        } finally {
            lås.unlock();
        }
    }

    private static <K extends Comparable<?>> ReentrantLock finnLåsForHentingAvVerdiForNøkkel(K key ){
        låsForMap.lock();
        try {
            ReentrantLock låsForNøkkel = låsPerNøkkel.get(key);
            if (låsForNøkkel == null) {
                låsForNøkkel = new ReentrantLock();
                låsPerNøkkel.put(key, låsForNøkkel);
            }
            return låsForNøkkel;
        } finally {
            låsForMap.unlock();
        }
    }

    private static <K extends Comparable<?>> void fjernLåsForHentingAvVerdiForNøkkel(K key){
        låsForMap.lock();
        try {
            låsPerNøkkel.remove(key);
        } finally {
            låsForMap.unlock();
        }
    }
}
