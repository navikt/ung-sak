package no.nav.k9.sak.domene.person.personopplysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;

@Dependent
public class BasisPersonopplysningTjeneste extends AbstractPersonopplysningTjenesteImpl {

    BasisPersonopplysningTjeneste() {
        super();
    }

    @Inject
    public BasisPersonopplysningTjeneste(PersonopplysningRepository personopplysningRepository) {
        super(personopplysningRepository);
    }

}
