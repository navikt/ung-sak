package no.nav.ung.sak.domene.person.personopplysning;

import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDate;
import java.util.Optional;

public abstract class AbstractPersonopplysningTjenesteImpl implements StandardPersonopplysningTjeneste {

    private PersonopplysningRepository personopplysningRepository;

    AbstractPersonopplysningTjenesteImpl() {
        // CDI
    }

    public AbstractPersonopplysningTjenesteImpl(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentPersonopplysningerHvisEksisterer(BehandlingReferanse ref) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository()
            .hentPersonopplysningerHvisEksisterer(ref.getBehandlingId());
        return grunnlagOpt.map(personopplysningGrunnlagEntitet -> mapTilAggregat(ref.getAktørId(), personopplysningGrunnlagEntitet));
    }

    @Override
    public PersonopplysningerAggregat hentPersonopplysninger(BehandlingReferanse ref) {
        return hentPersonopplysningerHvisEksisterer(ref).orElseThrow(() -> new IllegalStateException("Utvikler feil: Har ikke innhentet opplysninger fra register enda."));
    }


    protected PersonopplysningerAggregat mapTilAggregat(AktørId aktørId, PersonopplysningGrunnlagEntitet grunnlag) {
        return new PersonopplysningerAggregat(grunnlag, aktørId);
    }

    protected PersonopplysningRepository getPersonopplysningRepository() {
        return personopplysningRepository;
    }

}
