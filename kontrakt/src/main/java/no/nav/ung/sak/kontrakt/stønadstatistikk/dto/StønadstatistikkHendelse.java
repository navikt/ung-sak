package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.Saksnummer;


public sealed interface StønadstatistikkHendelse permits UngStønadstatistikkHendelse, AktivitetspengerStønadstatistikkHendelse {

    FagsakYtelseType ytelseType();

    Saksnummer saksnummer();
}
