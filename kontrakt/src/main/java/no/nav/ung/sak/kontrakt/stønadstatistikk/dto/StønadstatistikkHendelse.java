package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.Saksnummer;

import java.util.List;

public sealed interface StønadstatistikkHendelse permits UngStønadstatistikkHendelse, AktivitetspengerStønadstatistikkHendelse {

    FagsakYtelseType ytelseType();

    Saksnummer saksnummer();

    List<StønadsstatistikkSatsPeriode> satsPerioder();
}
