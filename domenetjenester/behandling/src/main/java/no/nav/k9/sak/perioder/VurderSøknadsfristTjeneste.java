package no.nav.k9.sak.perioder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.perioder.VurdertSøktPeriode.SøktPeriodeData;

public interface VurderSøknadsfristTjeneste<T extends SøktPeriodeData> {

    Map<KravDokument, List<VurdertSøktPeriode<T>>> vurderSøknadsfrist(BehandlingReferanse referanse);

    Map<KravDokument, List<SøktPeriode<T>>> hentPerioderTilVurdering(BehandlingReferanse referanse);

    /**
     * Kjøres på nytt etter løsing av aksjonspunkt for å sikre at alle periodene er tatt hensyn til.
     * NB! Husk å sjekk om periodene har blitt satt til OK eller IKKE OK av saksbehandler og sett status i henhold
     *
     * @param søknaderMedPerioder periodene
     *
     * @return resultatet
     */
    Map<KravDokument, List<VurdertSøktPeriode<T>>> vurderSøknadsfrist(Map<KravDokument, List<SøktPeriode<T>>> søknaderMedPerioder);

    /**
     * Henter ut kravdokumenter som har tilkommet i denne behandlingen
     * @param referanse referansen til behandlingen
     * @return kravdokumenter
     */
    Set<KravDokument> relevanteKravdokumentForBehandling(BehandlingReferanse referanse);
}
