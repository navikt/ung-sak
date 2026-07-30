package no.nav.ung.sak.behandlingslager.bosatt;

import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;

import java.time.LocalDate;

/**
 * Value type for bostedsavklaring per vilkårsperiode.
 * {@code erBosattITrondheim} angir om bruker er bosatt ved skjæringstidspunktet.
 * {@code fraflyttingsDato} angir når bruker eventuelt flyttet ut (null = ikke aktuelt eller hele perioden).
 * {@code BostedsvilkårIkkeOppfyltÅrsak} angir årsak til fraflytting eller avslag (null dersom bruker er bosatt hele perioden).
 * {@code kilde} angir om fakta ble satt automatisk fra søknad eller manuelt av saksbehandler.
 */
public record BostedAvklaringData(boolean erBosattITrondheim, LocalDate fraflyttingsDato, BostedsvilkårIkkeOppfyltÅrsak fraflyttingsÅrsak, Kilde kilde) {
}
