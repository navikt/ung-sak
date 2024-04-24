package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.RequestScoped;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@RequestScoped
public class UttaksplanCache {

    private static final Logger logger = LoggerFactory.getLogger(UttaksplanCache.class);
    private static long SANITY_SJEKK_CACHE_ALDER_SEKUNDER = 30;
    private LocalDateTime opprettet = LocalDateTime.now();
    private Map<UUID, Uttaksplan> innhold = new HashMap<>();

    public void clear() {
        innhold.clear();
    }

    public boolean erUttaksplanICache(UUID behandlingUuid) {
        sanityCheckAlderAvCache();
        return innhold.containsKey(behandlingUuid);
    }

    public Uttaksplan getUttaksplan(UUID behandlingUuid) {
        sanityCheckAlderAvCache();
        if (innhold.containsKey(behandlingUuid)) {
            logger.info("Uttaksplan cache hit");
            return innhold.get(behandlingUuid);
        }
        logger.info("Uttaksplan cache miss");
        return null;
    }

    public void put(UUID behandlingUuid, Uttaksplan uttaksplan) {
        logger.info("Uttaksplan cache put");
        innhold.put(behandlingUuid, uttaksplan);
        if (innhold.size() > 4) { //forventer at det hentes plan for inneværende og forrige behandling, samt annen part
            logger.warn("Uttaksplan-cache har {} behandlinger. Forventer lavere tall, men kan gi mening hvis behandlingene er knyttet til samme sak eller relaterte saker. Behandlinger i cache: {}", innhold.size(), innhold.keySet());
        }
    }

    //for å sikre at det oppdages om noen endrer scope fra RequestScoped, eller evt. annet gjør at cache får feil bruk
    private void sanityCheckAlderAvCache() {
        if (LocalDateTime.now().isAfter(opprettet.plusSeconds(SANITY_SJEKK_CACHE_ALDER_SEKUNDER))) {
            throw new IllegalStateException("Gammel uttaksplancache. Enten er kall veldig tregt, eller feil med scope på eller bruk av cache.");
        }
    }
}
