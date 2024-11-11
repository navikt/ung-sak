package no.nav.ung.sak.hendelse.stønadstatistikk;

import java.util.UUID;

import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkHendelse;

public interface StønadstatistikkHendelseBygger {

    StønadstatistikkHendelse lagHendelse(UUID behandlingUuid);

}
