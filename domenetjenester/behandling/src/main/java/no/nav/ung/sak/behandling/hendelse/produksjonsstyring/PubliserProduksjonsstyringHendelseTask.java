package no.nav.ung.sak.behandling.hendelse.produksjonsstyring;

import no.nav.k9.prosesstask.api.ProsessTaskHandler;

public interface PubliserProduksjonsstyringHendelseTask extends ProsessTaskHandler {
    String TASKTYPE = "produksjonsstyring.publiserHendelse";
    String PROPERTY_KEY = "topicKey";
}
