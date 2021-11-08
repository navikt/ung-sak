package no.nav.k9.sak.hendelse.stønadstatistikk;

import java.util.UUID;

import no.nav.k9.sak.hendelse.stønadstatistikk.dto.StønadstatistikkHendelse;

public interface StønadstatistikkHendelseBygger {
    
    StønadstatistikkHendelse lagHendelse(UUID behandlingUuid);
    
}
