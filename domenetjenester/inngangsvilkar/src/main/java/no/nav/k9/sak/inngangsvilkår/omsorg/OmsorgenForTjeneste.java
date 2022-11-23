package no.nav.k9.sak.inngangsvilkår.omsorg;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.BostedsAdresse;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForVilkår;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForVilkårGrunnlag;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.Relasjon;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.RelasjonsRolle;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenForGrunnlag;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenForGrunnlagRepository;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
public class OmsorgenForTjeneste {

    private VilkårUtfallOversetter utfallOversetter;
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    OmsorgenForTjeneste() {
        // CDI
    }

    @Inject
    public OmsorgenForTjeneste(OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository,
                               BehandlingRepository behandlingRepository,
                               BasisPersonopplysningTjeneste personopplysningTjeneste,
                               @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester) {

        this.utfallOversetter = new VilkårUtfallOversetter();
        this.omsorgenForGrunnlagRepository = omsorgenForGrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
    }

    public List<VilkårData> vurderPerioder(LocalDateTimeline<OmsorgenForVilkårGrunnlag> samletOmsorgenForTidslinje) {
        final List<VilkårData> resultat = new ArrayList<>();
        for (LocalDateSegment<OmsorgenForVilkårGrunnlag> s : samletOmsorgenForTidslinje.toSegments()) {
            final var evaluation = new OmsorgenForVilkår().evaluer(s.getValue());
            final var vilkårData = utfallOversetter.oversett(VilkårType.OMSORGEN_FOR, evaluation, s.getValue(), DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()));
            resultat.add(vilkårData);
        }

        return resultat;
    }

    public LocalDateTimeline<OmsorgenForVilkårGrunnlag> mapGrunnlag(BehandlingskontrollKontekst kontekst, NavigableSet<DatoIntervallEntitet> perioder) {
        return mapGrunnlag(kontekst.getBehandlingId(), kontekst.getAktørId(), perioder);
    }

    public LocalDateTimeline<OmsorgenForVilkårGrunnlag> mapGrunnlag(long behandlingId, AktørId aktørId, NavigableSet<DatoIntervallEntitet> perioder) {
        final OmsorgenForVilkårGrunnlag systemgrunnlag = oversettSystemdataTilRegelModellOmsorgen(behandlingId, aktørId, perioder);
        final var vurdertOmsorgenForTidslinje = oversettTilRegelModellOmsorgenForVurderinger(behandlingId);

        return slåSammenGrunnlagFraSystemOgVurdering(perioder, systemgrunnlag, vurdertOmsorgenForTidslinje);
    }

    private LocalDateTimeline<OmsorgenForVilkårGrunnlag> slåSammenGrunnlagFraSystemOgVurdering(final NavigableSet<DatoIntervallEntitet> perioder,
                                                                                               final OmsorgenForVilkårGrunnlag systemgrunnlag,
                                                                                               final LocalDateTimeline<OmsorgenForVilkårGrunnlag> vurdertOmsorgenForTidslinje) {
        final var systemdatatidslinje = new LocalDateTimeline<>(perioder.stream().map(p -> new LocalDateSegment<>(p.toLocalDateInterval(), systemgrunnlag)).collect(Collectors.toList()));
        return vurdertOmsorgenForTidslinje.combine(systemdatatidslinje, (datoInterval, datoSegment, datoSegment2) -> {
            if (datoSegment == null) {
                return new LocalDateSegment<>(datoInterval, datoSegment2.getValue());
            }

            final OmsorgenForVilkårGrunnlag sg = datoSegment2.getValue();
            final OmsorgenForVilkårGrunnlag sammensatt = new OmsorgenForVilkårGrunnlag(sg.getRelasjonMellomSøkerOgPleietrengende(), sg.getSøkersAdresser(), sg.getPleietrengendeAdresser(), datoSegment.getValue().getErOmsorgsPerson());
            return new LocalDateSegment<>(datoInterval, sammensatt);
        }, JoinStyle.RIGHT_JOIN);
    }

    private LocalDateTimeline<OmsorgenForVilkårGrunnlag> oversettTilRegelModellOmsorgenForVurderinger(long behandlingId) {
        final Optional<OmsorgenForGrunnlag> omsorgenForGrunnlag = omsorgenForGrunnlagRepository.hentHvisEksisterer(behandlingId);
        return new LocalDateTimeline<>(
            omsorgenForGrunnlag.map(og -> og.getOmsorgenFor().getPerioder()).orElse(List.of())
                .stream()
                .map(ofp -> new LocalDateSegment<>(ofp.getPeriode().getFomDato(), ofp.getPeriode().getTomDato(), new OmsorgenForVilkårGrunnlag(null, null, null, mapToErOmsorgsperson(ofp.getResultat()))))
                .collect(Collectors.toList())
        );
    }

    private Boolean mapToErOmsorgsperson(Resultat resultat) {
        return switch (resultat) {
            case OPPFYLT -> true;
            case IKKE_OPPFYLT -> false;
            case IKKE_VURDERT -> null;
        };
    }

    public OmsorgenForVilkårGrunnlag oversettSystemdataTilRegelModellOmsorgen(Long behandlingId, AktørId aktørId, NavigableSet<DatoIntervallEntitet> perioder) {
        final var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, omsluttendePeriode(perioder)).orElseThrow();
        final var pleietrengende = behandlingRepository.hentBehandling(behandlingId).getFagsak().getPleietrengendeAktørId();

        // Lar denne stå her inntil videre selv om vi ikke bruker den:
        final var søkerBostedsadresser = personopplysningerAggregat.getAdresserFor(aktørId)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());
        final var pleietrengendeBostedsadresser = personopplysningerAggregat.getAdresserFor(pleietrengende)
            .stream()
            .filter(it -> AdresseType.BOSTEDSADRESSE.equals(it.getAdresseType()))
            .collect(Collectors.toList());

        return new OmsorgenForVilkårGrunnlag(mapReleasjonMellomPleietrengendeOgSøker(personopplysningerAggregat, pleietrengende),
            mapAdresser(søkerBostedsadresser), mapAdresser(pleietrengendeBostedsadresser), null);
    }

    private DatoIntervallEntitet omsluttendePeriode(final NavigableSet<DatoIntervallEntitet> perioder) {
        final var startDato = perioder.stream().map(DatoIntervallEntitet::getFomDato).min(LocalDate::compareTo).orElse(LocalDate.now());
        final var sluttDato = perioder.stream().map(DatoIntervallEntitet::getTomDato).max(LocalDate::compareTo).orElse(LocalDate.now());
        return DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDato);
    }

    private List<BostedsAdresse> mapAdresser(List<PersonAdresseEntitet> pleietrengendeBostedsadresser) {
        return pleietrengendeBostedsadresser.stream()
            .map(it -> new BostedsAdresse(it.getAktørId().getId(), it.getAdresselinje1(), it.getAdresselinje2(), it.getAdresselinje3(), it.getPostnummer(), it.getLand()))
            .collect(Collectors.toList());
    }

    private Relasjon mapReleasjonMellomPleietrengendeOgSøker(PersonopplysningerAggregat aggregat, AktørId pleietrengende) {
        final var relasjoner = aggregat.getSøkersRelasjoner().stream().filter(it -> it.getTilAktørId().equals(pleietrengende)).collect(Collectors.toSet());
        if (relasjoner.size() > 1) {
            throw new IllegalStateException("Fant flere relasjoner til barnet. Vet ikke hvilken som skal prioriteres");
        } else if (relasjoner.size() == 1) {
            final var relasjonen = relasjoner.iterator().next();
            return new Relasjon(relasjonen.getAktørId().getId(), relasjonen.getTilAktørId().getId(), RelasjonsRolle.find(relasjonen.getRelasjonsrolle().getKode()), relasjonen.getHarSammeBosted());
        } else {
            return null;
        }
    }

    public Systemdata hentSystemdata(Long behandlingId, AktørId aktørId, AktørId optPleietrengendeAktørId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var vilkårsPerioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
        var fullstendigePerioder = vilkårsPerioderTilVurderingTjeneste.utledFullstendigePerioder(behandlingId);
        var pleietrengende = Optional.ofNullable(optPleietrengendeAktørId);
        if (fullstendigePerioder.isEmpty() || pleietrengende.isEmpty()) {
            return new Systemdata(false, false);
        }
        var periode = mapTilPeriode(fullstendigePerioder);

        var optAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periode);
        if (optAggregat.isEmpty()) {
            return new Systemdata(false, false);
        }
        var aggregat = optAggregat.get();
        var pleietrengendeAktørId = pleietrengende.get();
        var relasjon = aggregat.getSøkersRelasjoner().stream().filter(it -> it.getTilAktørId().equals(pleietrengendeAktørId)).toList();

        var registrertForeldrerelasjon = relasjon.stream().anyMatch(it -> RelasjonsRolleType.BARN.equals(it.getRelasjonsrolle()));
        var registrertSammeBosted = aggregat.harSøkerSammeAdresseSom(pleietrengendeAktørId, RelasjonsRolleType.BARN);

        return new Systemdata(registrertForeldrerelasjon, registrertSammeBosted);

    }

    private DatoIntervallEntitet mapTilPeriode(NavigableSet<DatoIntervallEntitet> perioder) {
        final var fom = perioder
            .stream()
            .map(DatoIntervallEntitet::getFomDato)
            .min(LocalDate::compareTo)
            .orElseThrow();

        final var tom = perioder
            .stream()
            .map(DatoIntervallEntitet::getTomDato)
            .max(LocalDate::compareTo)
            .orElseThrow();

        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    public static class Systemdata {
        private final boolean registrertForeldrerelasjon;
        private final boolean registrertSammeBosted;


        public Systemdata(boolean registrertForeldrerelasjon, boolean registrertSammeBosted) {
            this.registrertForeldrerelasjon = registrertForeldrerelasjon;
            this.registrertSammeBosted = registrertSammeBosted;
        }


        public boolean isRegistrertForeldrerelasjon() {
            return registrertForeldrerelasjon;
        }

        public boolean isRegistrertSammeBosted() {
            return registrertSammeBosted;
        }
    }
}
