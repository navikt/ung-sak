package no.nav.k9.sak.behandling.hendelse.produksjonsstyring;

/**
 * Det er et behov i LOS for å motta hendelser innenfor en behandling i sekvens. Dette løses her ved å benytte
 * gruppe+sekvensnummer-funksjonalitet i task-rammeverket. Det er fortsatt teoretisk mulig å få hendelser i feil
 * rekkefølge, men da må enten
 * 1. behandling fortsette veldig kjapt på en annen node, og klokker mellom noder være ute av sync
 * 2. rekkefølge endres ved at task som sender til kafka ikke committer/flusher til kafka før neste kjøres på en annen node
 */
public class LosTaskSekvensGenerator {

    private static final ThreadLocal<Long> tidsstempel = ThreadLocal.withInitial(() -> 0L);
    private static final ThreadLocal<Integer> sekvens = ThreadLocal.withInitial(() -> 0);

    private LosTaskSekvensGenerator() {
    }

    public static String nesteSekvens() {
        long millis = System.currentTimeMillis();
        if (millis != tidsstempel.get()) {
            tidsstempel.set(millis);
            sekvens.set(0);
        } else {
            sekvens.set(sekvens.get() + 1);
        }
        return String.format("%d-%d", tidsstempel.get(), sekvens.get());
    }

    public static String gruppeForBehandling(Long behandlingId) {
        return "LOS-behandling-" + behandlingId;
    }
}
