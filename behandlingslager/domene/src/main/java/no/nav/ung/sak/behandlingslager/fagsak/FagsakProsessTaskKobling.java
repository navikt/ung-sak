package no.nav.ung.sak.behandlingslager.fagsak;

import java.time.LocalDateTime;

import no.nav.k9.prosesstask.api.ProsessTaskStatus;

/**
 * En rad i FAGSAK_PROSESS_TASK, med status fra PROSESS_TASK dersom prosesstasken finnes.
 * Koblingen er foreldreløs når prosesstasken ikke lenger finnes.
 */
public record FagsakProsessTaskKobling(Long id,
                                       Long prosessTaskId,
                                       String behandlingId,
                                       String taskType,
                                       Long gruppeSekvensNr,
                                       LocalDateTime opprettetTid,
                                       boolean prosessTaskFinnes,
                                       ProsessTaskStatus prosessTaskStatus,
                                       Long blokkertAv) {

    public boolean erForeldreløs() {
        return !prosessTaskFinnes;
    }
}
