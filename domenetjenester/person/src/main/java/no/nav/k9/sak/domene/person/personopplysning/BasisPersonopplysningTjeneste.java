package no.nav.k9.sak.domene.person.personopplysning;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;

@ApplicationScoped
public class BasisPersonopplysningTjeneste extends AbstractPersonopplysningTjenesteImpl {

    BasisPersonopplysningTjeneste() {
        super();
    }

    @Inject
    public BasisPersonopplysningTjeneste(PersonopplysningRepository personopplysningRepository) {
        super(personopplysningRepository);
    }

}
