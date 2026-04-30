package no.nav.ung.sak.behandlingslager.bosatt;

import java.time.LocalDate;

/**
 * Value type for bostedsavklaring per vilkårsperiode.
 * {@code erBosattITrondheim} angir om bruker er bosatt ved skjæringstidspunktet.
 * {@code fraflyttingsDato} angir når bruker eventuelt flyttet ut (null = ikke aktuelt eller hele perioden).
 */
public record BostedAvklaringData(boolean erBosattITrondheim, LocalDate fraflyttingsDato) {
}
