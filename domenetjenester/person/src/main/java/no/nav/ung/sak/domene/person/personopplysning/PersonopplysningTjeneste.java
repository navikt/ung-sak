package no.nav.ung.sak.domene.person.personopplysning;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.diff.DiffResult;

@Dependent
public class PersonopplysningTjeneste extends AbstractPersonopplysningTjenesteImpl {

    PersonopplysningTjeneste() {
        super();
        // CDI
    }

    @Inject
    public PersonopplysningTjeneste(PersonopplysningRepository personopplysningRepository) {
        super(personopplysningRepository);
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        Optional<Long> funnetId = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId).map(PersonopplysningGrunnlagEntitet::getId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(PersonInformasjonEntitet.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(PersonInformasjonEntitet.class));
    }

    public DiffResult diffResultat(EndringsresultatDiff idDiff, boolean kunSporedeEndringer) {
        PersonopplysningGrunnlagEntitet grunnlag1 = getPersonopplysningRepository().hentPersonopplysningerPåId((Long) idDiff.getGrunnlagId1());
        PersonopplysningGrunnlagEntitet grunnlag2 = getPersonopplysningRepository().hentPersonopplysningerPåId((Long) idDiff.getGrunnlagId2());
        return getPersonopplysningRepository().diffResultat(grunnlag1, grunnlag2, kunSporedeEndringer);
    }

}
