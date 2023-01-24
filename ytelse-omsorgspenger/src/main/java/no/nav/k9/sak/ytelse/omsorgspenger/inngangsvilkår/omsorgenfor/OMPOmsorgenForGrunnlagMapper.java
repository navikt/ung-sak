package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.omsorgenfor;

import static java.util.Collections.emptyList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.BostedsAdresse;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.Fosterbarn;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForGrunnlagMapper;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForVilkårGrunnlag;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.FosterbarnGrunnlag;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.FosterbarnRepository;

@FagsakYtelseTypeRef(FagsakYtelseType.OMSORGSPENGER)
@ApplicationScoped
public class OMPOmsorgenForGrunnlagMapper implements OmsorgenForGrunnlagMapper {

    private PersonopplysningTjeneste personopplysningTjeneste;
    private FosterbarnRepository fosterbarnRepository;


    OMPOmsorgenForGrunnlagMapper() {
        // CDI
    }

    @Inject
    public OMPOmsorgenForGrunnlagMapper(PersonopplysningTjeneste personopplysningTjeneste, FosterbarnRepository fosterbarnRepository) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.fosterbarnRepository = fosterbarnRepository;
    }

    @Override
    public Map<DatoIntervallEntitet, OmsorgenForVilkårGrunnlag> map(BehandlingReferanse referanse, NavigableSet<DatoIntervallEntitet> vilkårsperioder) {
        Map<DatoIntervallEntitet, OmsorgenForVilkårGrunnlag> result = new HashMap<>();
        Optional<FosterbarnGrunnlag> fosterbarnGrunnlag = fosterbarnRepository.hentHvisEksisterer(referanse.getBehandlingId());

        for (DatoIntervallEntitet vilkårsperiode : vilkårsperioder) {
            result.put(vilkårsperiode, mapGrunnlagForPeriode(referanse, vilkårsperiode, fosterbarnGrunnlag));
        }

        return result;
    }

    private OmsorgenForVilkårGrunnlag mapGrunnlagForPeriode(BehandlingReferanse referanse, DatoIntervallEntitet vilkårsperiode, Optional<FosterbarnGrunnlag> fosterbarnGrunnlag) {
        var behandlingId = referanse.getBehandlingId();
        var aktørId = referanse.getAktørId();
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, vilkårsperiode).orElseThrow();

        final var søkerBostedsadresser = personopplysningerAggregat.getAdresserFor(aktørId)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());

        List<BostedsAdresse> søkersBostedsadresserIPerioden = utledAdresseperioderIVilkårsperioden(søkerBostedsadresser, vilkårsperiode);

        List<PersonopplysningEntitet> barna = personopplysningerAggregat.getBarnaTil(aktørId);
        List<BostedsAdresse> barnasBostedsadresser = emptyList();
        List<BostedsAdresse> barnasDeltBostedsadresser = emptyList();

        for (PersonopplysningEntitet barn : barna) {
            barnasBostedsadresser.addAll(utledAdresseperioderIVilkårsperioden(hentAdresserForBarn(personopplysningerAggregat, barn, AdresseType.BOSTEDSADRESSE), vilkårsperiode));
            barnasDeltBostedsadresser.addAll(utledAdresseperioderIVilkårsperioden(hentAdresserForBarn(personopplysningerAggregat, barn, AdresseType.DELT_BOSTEDSADRESSE), vilkårsperiode));
        }

        var fosterbarna = fosterbarnGrunnlag
            .map(grunnlag -> grunnlag.getFosterbarna().getFosterbarn().stream()
                .map(fosterbarn -> mapFosterbarn(behandlingId, fosterbarn.getAktørId(), vilkårsperiode))
                .collect(Collectors.toList()))
            .orElse(List.of());
        List<Fosterbarn> fosterbarnIPerioden = utledFosterbarnIPerioden(vilkårsperiode, fosterbarna);

        return new OmsorgenForVilkårGrunnlag(vilkårsperiode, null, søkersBostedsadresserIPerioden, barnasBostedsadresser, null, fosterbarnIPerioden, barnasDeltBostedsadresser);
    }

    private List<PersonAdresseEntitet> hentAdresserForBarn(PersonopplysningerAggregat personopplysningerAggregat, PersonopplysningEntitet barn,
                                                           AdresseType adresseType) {
        List<PersonAdresseEntitet> barnetsAdresser = personopplysningerAggregat.getAdresserFor(barn.getAktørId());
        return barnetsAdresser.stream().filter(s -> adresseType.equals(s.getAdresseType())).toList();
    }

    private List<BostedsAdresse> utledAdresseperioderIVilkårsperioden(List<PersonAdresseEntitet> adresser, DatoIntervallEntitet vilkårsperiode) {
        LocalDateTimeline<PersonAdresseEntitet> søkerBostedsadresserTidslinje = new LocalDateTimeline<>(adresser.stream()
            .map(bostedAdresse -> new LocalDateSegment<>(bostedAdresse.getPeriode().getFomDato(), bostedAdresse.getPeriode().getTomDato(), bostedAdresse))
            .collect(Collectors.toList()));

        LocalDateTimeline<PersonAdresseEntitet> bostedPerioderTidslinje = søkerBostedsadresserTidslinje.intersection(new LocalDateInterval(vilkårsperiode.getFomDato(), vilkårsperiode.getTomDato()));
        List<PersonAdresseEntitet> aktuelleAdresser = bostedPerioderTidslinje.stream().map(LocalDateSegment::getValue).toList();

        return aktuelleAdresser.stream()
            .map(adresse -> new BostedsAdresse(adresse.getAktørId().getId(), adresse.getAdresselinje1(), adresse.getAdresselinje2(), adresse.getAdresselinje3(), adresse.getPostnummer(), adresse.getLand(), adresse.getPeriode()))
            .collect(Collectors.toList());
    }

    private Fosterbarn mapFosterbarn(Long behandlingId, AktørId aktørId, DatoIntervallEntitet vilkårsperiode) {
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, vilkårsperiode).orElseThrow();
        PersonopplysningEntitet personopplysning = personopplysningerAggregat.getPersonopplysning(aktørId);
        return new Fosterbarn(aktørId.getId(), personopplysning.getFødselsdato(), personopplysning.getDødsdato());
    }

    private List<Fosterbarn> utledFosterbarnIPerioden(DatoIntervallEntitet vilkårsperiode, List<Fosterbarn> fosterbarna) {
        return fosterbarna.stream().filter(barn -> DatoIntervallEntitet.fraOgMedTilOgMed(barn.getFødselsdato(), barn.getDødsdato() != null ? barn.getDødsdato() : Tid.TIDENES_ENDE).overlapper(vilkårsperiode)).toList();
    }
}
