package no.nav.ung.sak.tilgangskontroll.tilganger;

import no.nav.k9.felles.util.LRUCache;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CacheOppfriskingHåndtererTest {

    private LRUCache<String, Integer> cache = new LRUCache<>(10, Duration.ofMinutes(10).toMillis());

    @Test
    void skal_hente_verdi_når_ikke_finnes_fra_før() {
        CacheOppfriskingHåndterer.hentOmIkkeICache("foo", cache, String::length);
        assertThat(cache.get("foo")).isEqualTo(3);
    }

    @Test
    void skal_ikke_hente_verdi_når_nøkkel_finnes_i_cache_fra_før() {
        cache.put("foo", 1);

        CacheOppfriskingHåndterer.hentOmIkkeICache("foo", cache, String::length);

        assertThat(cache.get("foo")).isEqualTo(1);
    }

    @Test
    void skal_hente_verdi_en_gang_når_flere_tråder_forsøker_å_hente_verdi_samtidig() throws InterruptedException {
        AtomicInteger antallOppdateringer = new AtomicInteger(0);

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            threads.add(new Thread( () -> CacheOppfriskingHåndterer.hentOmIkkeICache("foo", cache, key -> {
                antallOppdateringer.getAndIncrement();
                try {
                    Thread.sleep(10); //vente litt slik at flere tråder kan rekke å starte
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return 3;
            }),"testthread-" + i));
        }
        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(antallOppdateringer.get()).isEqualTo(1);

        assertThat(cache.get("foo")).isEqualTo(3);
    }

    @Test
    void skal_kunne_hente_verdi_for_ulike_nøkler_parallelt() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Thread threadFoo = new Thread( () -> CacheOppfriskingHåndterer.hentOmIkkeICache("foo", cache, key -> {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return 3;
            }),"thread-foo");
        threadFoo.start();

        //henter verdi på main thread, for en annen nøkkel, før thread-foo er ferdig (den venter på latch)
        Integer verdiPåMainThread = CacheOppfriskingHåndterer.hentOmIkkeICache("bar", cache, String::length);
        assertThat(verdiPåMainThread).isEqualTo(3);

        //thread-foo er ikke ferig å hente
        assertThat(cache.get("foo")).isNull();

        countDownLatch.countDown();
        threadFoo.join();
        assertThat(cache.get("foo")).isEqualTo(3);
    }


}
