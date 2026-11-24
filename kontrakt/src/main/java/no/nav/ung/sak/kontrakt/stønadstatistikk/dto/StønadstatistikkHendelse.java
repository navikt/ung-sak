package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

public sealed interface StønadstatistikkHendelse permits UngStønadstatistikkHendelse, AktivitetspengerStønadstatistikkHendelse {

    FagsakYtelseType ytelseType();
}
