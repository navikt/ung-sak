package no.nav.k9.sak.perioder;

import java.util.List;
import java.util.Map;

import no.nav.k9.sak.behandling.BehandlingReferanse;

public interface VurderSøknadsfristTjeneste<T> {

    Map<Søknad, List<VurdertSøktPeriode<T>>> vurderSøknadsfrist(BehandlingReferanse referanse);

    Map<Søknad, List<SøktPeriode<T>>> hentPerioderTilVurdering(BehandlingReferanse referanse);

    /**
     * Kjøres på nytt etter løsing av aksjonspunkt for å sikre at alle periodene er tatt hensyn til.
     * NB! Husk å sjekk om periodene har blitt satt til OK eller IKKE OK av saksbehandler og sett status i henhold
     *
     * @param søknaderMedPerioder periodene
     *
     * @return resultatet
     */
    Map<Søknad, List<VurdertSøktPeriode<T>>> vurderSøknadsfrist(Map<Søknad, List<SøktPeriode<T>>> søknaderMedPerioder);
}
