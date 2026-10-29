package no.nav.ung.sak.behandlingslager.fagsak;

import java.time.LocalDateTime;

import no.nav.k9.prosesstask.api.ProsessTaskStatus;

/**
 * En rad i FAGSAK_PROSESS_TASK, med status fra PROSESS_TASK dersom prosesstasken finnes.
 * Prosesstasken mangler når den er slettet uten at koblingen er fjernet.
 */
public record FagsakProsessTaskKobling(Long id,
                                       Long prosessTaskId,
                                       Long behandlingId,
                                       String taskType,
                                       Long gruppeSekvensNr,
                                       LocalDateTime opprettetTid,
                                       boolean prosessTaskFinnes,
                                       ProsessTaskStatus prosessTaskStatus,
                                       Long blokkertAv) {

    public boolean prosessTaskMangler() {
        return !prosessTaskFinnes;
    }
}
