package no.nav.k9.sak.perioder;

import no.nav.k9.sak.behandling.BehandlingReferanse;

import java.util.Map;
import java.util.Set;

public interface VurderSøknadsfristTjeneste<T> {

    Map<Søknad, Set<VurdertSøktPeriode<T>>> vurderSøknadsfrist(BehandlingReferanse referanse);

    Map<Søknad, Set<SøktPeriode<T>>> hentPerioderTilVurdering(BehandlingReferanse referanse);

    /**
     * Kjøres på nytt etter løsing av aksjonspunkt for å sikre at alle periodene er tatt hensyn til.
     * NB! Husk å sjekk om periodene har blitt satt til OK eller IKKE OK av saksbehandler og sett status i henhold
     *
     * @param søknaderMedPerioder periodene
     *
     * @return resultatet
     */
    Map<Søknad, Set<VurdertSøktPeriode<T>>> vurderSøknadsfrist(Map<Søknad, Set<SøktPeriode<T>>> søknaderMedPerioder);
}
