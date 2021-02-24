package no.nav.k9.sak.domene.person.personopplysning;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

public abstract class AbstractPersonopplysningTjenesteImpl implements StandardPersonopplysningTjeneste {

    private PersonopplysningRepository personopplysningRepository;

    AbstractPersonopplysningTjenesteImpl() {
        // CDI
    }

    public AbstractPersonopplysningTjenesteImpl(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

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
            final Map<Landkoder, Region> landkoderRegionMap = getLandkoderOgRegion(grunnlagOpt.get());
            tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
            return new PersonopplysningerAggregat(grunnlagOpt.get(), aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(tidspunkt, tidspunkt.plusDays(1)),
                landkoderRegionMap);
        }
        throw new IllegalStateException("Utvikler feil: Har ikke innhentet opplysninger fra register enda.");
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(Long behandlingId, AktørId aktørId,
                                                                                                        LocalDate tidspunkt) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId);
        if (grunnlagOpt.isPresent()) {
            final Map<Landkoder, Region> landkoderRegionMap = getLandkoderOgRegion(grunnlagOpt.get());
            tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
            return Optional.of(new PersonopplysningerAggregat(grunnlagOpt.get(), aktørId,
                DatoIntervallEntitet.fraOgMedTilOgMed(tidspunkt, tidspunkt.plusDays(1)), landkoderRegionMap));
        }
        return Optional.empty();
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(Long behandlingId, AktørId aktørId,
                                                                                                       DatoIntervallEntitet forPeriode) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId);
        if (grunnlagOpt.isPresent()) {
            final Map<Landkoder, Region> landkoderRegionMap = getLandkoderOgRegion(grunnlagOpt.get());
            return Optional.of(new PersonopplysningerAggregat(grunnlagOpt.get(), aktørId, forPeriode, landkoderRegionMap));
        }
        return Optional.empty();
    }

    protected Map<Landkoder, Region> getLandkoderOgRegion(PersonopplysningGrunnlagEntitet grunnlag) {
        final List<Landkoder> landkoder = grunnlag.getRegisterVersjon()
            .map(PersonInformasjonEntitet::getStatsborgerskap)
            .orElse(Collections.emptyList())
            .stream()
            .map(StatsborgerskapEntitet::getStatsborgerskap)
            .collect(toList());
        return Region.finnRegionForStatsborgerskap(landkoder);
    }

    protected PersonopplysningerAggregat mapTilAggregat(AktørId aktørId, LocalDate tidspunkt, PersonopplysningGrunnlagEntitet grunnlag) {
        final Map<Landkoder, Region> landkoderRegionMap = getLandkoderOgRegion(grunnlag);
        tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
        return new PersonopplysningerAggregat(grunnlag, aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(tidspunkt, tidspunkt.plusDays(1)), landkoderRegionMap);
    }

    protected PersonopplysningRepository getPersonopplysningRepository() {
        return personopplysningRepository;
    }

}
