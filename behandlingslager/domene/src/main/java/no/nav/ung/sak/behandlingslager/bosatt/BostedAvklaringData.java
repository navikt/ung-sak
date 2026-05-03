package no.nav.ung.sak.behandlingslager.bosatt;

import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;

import java.time.LocalDate;

/**
 * Value type for bostedsavklaring per vilkårsperiode.
 * {@code erBosattITrondheim} angir om bruker er bosatt ved skjæringstidspunktet.
 * {@code fraflyttingsDato} angir når bruker eventuelt flyttet ut (null = ikke aktuelt eller hele perioden).
 * {@code fraflyttingsÅrsak} angir årsak til fraflytting (null dersom bruker er bosatt hele perioden).
 */
public record BostedAvklaringData(boolean erBosattITrondheim, LocalDate fraflyttingsDato, FraflyttingsÅrsak fraflyttingsÅrsak) {
}
