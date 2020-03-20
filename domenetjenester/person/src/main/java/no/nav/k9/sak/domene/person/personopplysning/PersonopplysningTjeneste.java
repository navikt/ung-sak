package no.nav.k9.sak.domene.person.personopplysning;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
public class PersonopplysningTjeneste extends AbstractPersonopplysningTjenesteImpl {

    PersonopplysningTjeneste() {
        super();
        // CDI
    }

    @Inject
    public PersonopplysningTjeneste(PersonopplysningRepository personopplysningRepository) {
        super(personopplysningRepository);
    }

    public void aksjonspunktAvklarSaksopplysninger(Long behandlingId, AktørId aktørId, PersonopplysningAksjonspunkt adapter) {
        new AvklarSaksopplysningerAksjonspunkt(getPersonopplysningRepository()).oppdater(behandlingId, aktørId, adapter);
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
