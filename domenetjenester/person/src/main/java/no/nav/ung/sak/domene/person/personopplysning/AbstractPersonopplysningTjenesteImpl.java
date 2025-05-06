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
    public PersonopplysningerAggregat hentPersonopplysninger(BehandlingReferanse ref, LocalDate vurderingspunkt) {
        return hentGjeldendePersoninformasjonPåTidspunkt(ref.getBehandlingId(), ref.getAktørId(), vurderingspunkt);
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentPersonopplysningerHvisEksisterer(BehandlingReferanse ref, LocalDate vurderingspunkt) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository()
            .hentPersonopplysningerHvisEksisterer(ref.getBehandlingId());
        return grunnlagOpt.map(personopplysningGrunnlagEntitet -> mapTilAggregat(ref.getAktørId(), vurderingspunkt, personopplysningGrunnlagEntitet));
    }

    @Override
    public PersonopplysningerAggregat hentGjeldendePersoninformasjonPåTidspunkt(Long behandlingId, AktørId aktørId, LocalDate tidspunkt) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId);
        if (grunnlagOpt.isPresent()) {
            tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
            return new PersonopplysningerAggregat(grunnlagOpt.get(), aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(tidspunkt, tidspunkt.plusDays(1)));
        }
        throw new IllegalStateException("Utvikler feil: Har ikke innhentet opplysninger fra register enda.");
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(Long behandlingId, AktørId aktørId,
                                                                                                        LocalDate tidspunkt) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId);
        if (grunnlagOpt.isPresent()) {
            tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
            return Optional.of(new PersonopplysningerAggregat(grunnlagOpt.get(), aktørId,
                DatoIntervallEntitet.fraOgMedTilOgMed(tidspunkt, tidspunkt.plusDays(1))));
        }
        return Optional.empty();
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(Long behandlingId, AktørId aktørId,
                                                                                                       DatoIntervallEntitet forPeriode) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId);
        if (grunnlagOpt.isPresent()) {
            return Optional.of(new PersonopplysningerAggregat(grunnlagOpt.get(), aktørId, forPeriode));
        }
        return Optional.empty();
    }

    protected PersonopplysningerAggregat mapTilAggregat(AktørId aktørId, LocalDate tidspunkt, PersonopplysningGrunnlagEntitet grunnlag) {
        tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
        return new PersonopplysningerAggregat(grunnlag, aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(tidspunkt, tidspunkt.plusDays(1)));
    }

    protected PersonopplysningRepository getPersonopplysningRepository() {
        return personopplysningRepository;
    }

}
