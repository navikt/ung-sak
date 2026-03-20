package no.nav.ung.sak.domene.person.personopplysning;

import java.util.Optional;

import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;

public interface StandardPersonopplysningTjeneste {

    /**
     * Gi de gjeldende  personopplysningene
     *
     * @return personopplysninger
     */
    PersonopplysningerAggregat hentPersonopplysninger(BehandlingReferanse ref);

    Optional<PersonopplysningerAggregat> hentPersonopplysningerHvisEksisterer(BehandlingReferanse ref);

}
