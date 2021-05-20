package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.omsorg.OmsorgenForDto;
import no.nav.k9.sak.kontrakt.omsorg.OmsorgenForOversiktDto;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg.OmsorgenForPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;

@ApplicationScoped
class OmsorgenForDtoMapper {

    private SøknadsperiodeRepository søknadsperiodeRepository;
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private BehandlingRepository behandlingRepository;

    OmsorgenForDtoMapper() {
        // CDI
    }

    @Inject
    public OmsorgenForDtoMapper(@Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste, OmsorgenForGrunnlagRepository omsorgenForRepository, BasisPersonopplysningTjeneste personopplysningTjeneste, SøknadsperiodeRepository søknadsperiodeRepository, BehandlingRepository behandlingRepository) {
        this.omsorgenForGrunnlagRepository = omsorgenForRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
    }


    public OmsorgenForOversiktDto map(Long behandlingId, AktørId aktørId, AktørId optPleietrengendeAktørId) {
        final boolean tvingManuellVurdering = false;
        final var systemdata = hentSystemdata(behandlingId, aktørId, optPleietrengendeAktørId);

        final LocalDateTimeline<Boolean> tidslinjeTilVurdering = lagTidslinjeTilVurdering(behandlingId);

        final Optional<OmsorgenForGrunnlag> grunnlagOpt = omsorgenForGrunnlagRepository.hentHvisEksisterer(behandlingId);
        if (grunnlagOpt.isEmpty()) {
            return new OmsorgenForOversiktDto(systemdata.isRegistrertForeldrerelasjon(), systemdata.isRegistrertSammeBosted(), tvingManuellVurdering, List.of());
        }
        final boolean ikkeVurdertBlirOppfylt = systemdata.isRegistrertForeldrerelasjon() && !tvingManuellVurdering ;
        return new OmsorgenForOversiktDto(
            systemdata.isRegistrertForeldrerelasjon(),
            systemdata.isRegistrertSammeBosted(),
            tvingManuellVurdering,
            toOmsorgenForDtoListe(grunnlagOpt.get().getOmsorgenFor().getPerioder(), ikkeVurdertBlirOppfylt, tidslinjeTilVurdering));
    }

    private LocalDateTimeline<Boolean> lagTidslinjeTilVurdering(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        VilkårsPerioderTilVurderingTjeneste vilkårsperioderTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType());
        LocalDateTimeline<Boolean> tidslinjeTilVurdering = SykdomUtils.toLocalDateTimeline(vilkårsperioderTjeneste.utled(behandlingId, VilkårType.OMSORGEN_FOR));
        return tidslinjeTilVurdering;
    }

    private Systemdata hentSystemdata(Long behandlingId, AktørId aktørId, AktørId optPleietrengendeAktørId) {
        final Optional<SøknadsperiodeGrunnlag> søknadsgrunnlag = søknadsperiodeRepository.hentGrunnlag(behandlingId);
        var pleietrengende = Optional.ofNullable(optPleietrengendeAktørId);
        if (søknadsgrunnlag.isEmpty()
                || søknadsgrunnlag.get().getOppgitteSøknadsperioder() == null
                || søknadsgrunnlag.get().getOppgitteSøknadsperioder().getPerioder() == null
                || pleietrengende.isEmpty()) {
            return new Systemdata(false, false);
        }
        var søknadsperioder = søknadsgrunnlag.get().getOppgitteSøknadsperioder();
        var periode = mapTilPeriode(søknadsperioder.getPerioder());

        var optAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, periode);
        if (!optAggregat.isPresent()) {
            return new Systemdata(false, false);
        }
        var aggregat = optAggregat.get();
        var pleietrengendeAktørId = pleietrengende.get();
        var relasjon = aggregat.getSøkersRelasjoner().stream().filter(it -> it.getTilAktørId().equals(pleietrengendeAktørId)).collect(Collectors.toList());

        var registrertForeldrerelasjon = relasjon.stream().anyMatch(it -> RelasjonsRolleType.BARN.equals(it.getRelasjonsrolle()));
        var registrertSammeBosted = aggregat.harSøkerSammeAdresseSom(pleietrengendeAktørId, RelasjonsRolleType.BARN);

        return new Systemdata(registrertForeldrerelasjon, registrertSammeBosted);

    }

    List<OmsorgenForDto> toOmsorgenForDtoListe(List<OmsorgenForPeriode> perioder, boolean ikkeVurdertBlirOppfylt, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {

        List omsorgenForDateSegments = perioder.stream().map(p -> new LocalDateSegment(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p)).collect(Collectors.toList());
        LocalDateTimeline<OmsorgenForPeriode> omsorgenForTimeline = new LocalDateTimeline<>(omsorgenForDateSegments);

        LocalDateTimeline<OmsorgenForDto> perioderMedIVurdering = omsorgenForTimeline.combine(tidslinjeTilVurdering, new LocalDateSegmentCombinator<OmsorgenForPeriode, Boolean, OmsorgenForDto>() {
            @Override
            public LocalDateSegment<OmsorgenForDto> combine(LocalDateInterval overlappendeIntervall, LocalDateSegment<OmsorgenForPeriode> eksisterendeOmsorgenForPeriode, LocalDateSegment<Boolean> periodeTilVurdering) {
                if (eksisterendeOmsorgenForPeriode == null) {
                    return null;
                }
                OmsorgenForPeriode p = eksisterendeOmsorgenForPeriode.getValue();
                OmsorgenForDto omsorgenForDto;
                if (periodeTilVurdering == null) {
                    omsorgenForDto = new OmsorgenForDto(
                        toPeriode(overlappendeIntervall),
                        p.getBegrunnelse(),
                        p.getRelasjon(),
                        p.getRelasjonsbeskrivelse(),
                        true,
                        p.getResultat(),
                        mapResultatEtterAutomatikk(p.getResultat(), ikkeVurdertBlirOppfylt));
                } else {
                    omsorgenForDto = new OmsorgenForDto(
                        toPeriode(overlappendeIntervall),
                        p.getBegrunnelse(),
                        p.getRelasjon(),
                        p.getRelasjonsbeskrivelse(),
                        false,
                        p.getResultat(),
                        mapResultatEtterAutomatikk(p.getResultat(), ikkeVurdertBlirOppfylt));
                }
                return new LocalDateSegment<OmsorgenForDto>(overlappendeIntervall, omsorgenForDto);
            }
        }, LocalDateTimeline.JoinStyle.LEFT_JOIN);

        return perioderMedIVurdering.stream().map(d -> d.getValue()).collect(Collectors.toList());
    }

    private Resultat mapResultatEtterAutomatikk(Resultat resultat, boolean ikkeVurdertBlirOppfylt) {
        return (resultat == Resultat.IKKE_VURDERT && ikkeVurdertBlirOppfylt) ? Resultat.OPPFYLT : resultat;
    }

    private Periode toPeriode(LocalDateInterval i) {
        return new Periode(i.getFomDato(), i.getTomDato());
    }

    private DatoIntervallEntitet mapTilPeriode(Set<Søknadsperioder> søknadsperioder) {
        final List<DatoIntervallEntitet> perioder = søknadsperioder.stream()
                .map(p -> p.getPerioder())
                .flatMap(Set::stream)
                .map(s -> s.getPeriode())
                .collect(Collectors.toList());

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

    private static class Systemdata {
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
