package no.nav.ung.ytelse.aktivitetspenger.perioder;

import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;

import java.util.List;
import java.util.Set;

/**
 * @param perioder  innvilgede perioder som overlapper forespurt periode, slått sammen på tvers av fagsaker
 * @param saksnumre fagsakene som har innvilgede perioder i {@code perioder}
 */
public record InnvilgedePerioder(List<Periode> perioder, Set<Saksnummer> saksnumre) {
}
