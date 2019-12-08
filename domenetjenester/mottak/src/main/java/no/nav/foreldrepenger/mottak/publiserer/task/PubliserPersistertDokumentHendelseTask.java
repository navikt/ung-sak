package no.nav.foreldrepenger.mottak.publiserer.task;


import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

public interface PubliserPersistertDokumentHendelseTask extends ProsessTaskHandler {

    String TASKTYPE = "mottak.publiserPersistertDokument";
    String MOTTATT_DOKUMENT_ID_KEY = "mottattDokumentId";

}
