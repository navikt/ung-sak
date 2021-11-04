package no.nav.k9.sak.hendelse.stønadstatistikk;

import java.util.UUID;

import no.nav.k9.sak.hendelse.stønadstatistikk.dto.StønadstatistikkHendelse;

public interface StønadstatistikkService {
    
    StønadstatistikkHendelse lagHendelse(UUID behandlingUuid);
    
}
