package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
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

@ApplicationScoped
public class OmsorgenForDtoMapper {

    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;
    private OmsorgenForTjeneste omsorgenForTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private BehandlingRepository behandlingRepository;

    OmsorgenForDtoMapper() {
        // CDI
    }

    @Inject
    public OmsorgenForDtoMapper(@Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
            OmsorgenForGrunnlagRepository omsorgenForRepository,
            OmsorgenForTjeneste omsorgenForTjeneste,
            BehandlingRepository behandlingRepository) {
        this.omsorgenForGrunnlagRepository = omsorgenForRepository;
        this.omsorgenForTjeneste = omsorgenForTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
    }


    public OmsorgenForOversiktDto map(Long behandlingId, AktørId aktørId, AktørId optPleietrengendeAktørId) {
        final boolean tvingManuellVurdering = false;
        final var systemdata = omsorgenForTjeneste.hentSystemdata(behandlingId, aktørId, optPleietrengendeAktørId);

        final LocalDateTimeline<Boolean> tidslinjeTilVurdering = lagTidslinjeTilVurdering(behandlingId);

        final Optional<OmsorgenForGrunnlag> grunnlagOpt = omsorgenForGrunnlagRepository.hentHvisEksisterer(behandlingId);
        if (grunnlagOpt.isEmpty()) {
            return new OmsorgenForOversiktDto(systemdata.isRegistrertForeldrerelasjon(), systemdata.isRegistrertSammeBosted(), tvingManuellVurdering, true, List.of());
        }
        final boolean ikkeVurdertBlirOppfylt = systemdata.isRegistrertForeldrerelasjon() && !tvingManuellVurdering ;

        final var omsorgenForListe = toOmsorgenForDtoListe(grunnlagOpt.get().getOmsorgenFor().getPerioder(), ikkeVurdertBlirOppfylt, tidslinjeTilVurdering);
        final boolean kanLøseAksjonspunkt = omsorgenForListe.stream().allMatch(o -> o.getResultatEtterAutomatikk() != Resultat.IKKE_VURDERT);
        return new OmsorgenForOversiktDto(
            systemdata.isRegistrertForeldrerelasjon(),
            systemdata.isRegistrertSammeBosted(),
            tvingManuellVurdering,
            kanLøseAksjonspunkt,
            omsorgenForListe);
    }

    private LocalDateTimeline<Boolean> lagTidslinjeTilVurdering(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        VilkårsPerioderTilVurderingTjeneste vilkårsperioderTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType());
        LocalDateTimeline<Boolean> tidslinjeTilVurdering = SykdomUtils.toLocalDateTimeline(vilkårsperioderTjeneste.utled(behandlingId, VilkårType.OMSORGEN_FOR));
        return tidslinjeTilVurdering;
    }

    List<OmsorgenForDto> toOmsorgenForDtoListe(List<OmsorgenForPeriode> perioder, boolean ikkeVurdertBlirOppfylt, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {

        List<LocalDateSegment<OmsorgenForPeriode>> omsorgenForDateSegments = perioder.stream().map(p -> new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p)).collect(Collectors.toList());
        LocalDateTimeline<OmsorgenForPeriode> omsorgenForTimeline = new LocalDateTimeline<>(omsorgenForDateSegments);

        LocalDateTimeline<OmsorgenForDto> perioderMedIVurdering = omsorgenForTimeline.combine(tidslinjeTilVurdering, new LocalDateSegmentCombinator<OmsorgenForPeriode, Boolean, OmsorgenForDto>() {
            @Override
            public LocalDateSegment<OmsorgenForDto> combine(LocalDateInterval overlappendeIntervall, LocalDateSegment<OmsorgenForPeriode> eksisterendeOmsorgenForPeriode, LocalDateSegment<Boolean> periodeTilVurdering) {
                OmsorgenForPeriode p = eksisterendeOmsorgenForPeriode.getValue();
                final boolean readOnly = (periodeTilVurdering == null);
                OmsorgenForDto omsorgenForDto = new OmsorgenForDto(
                    toPeriode(overlappendeIntervall),
                    p.getBegrunnelse(),
                    p.getRelasjon(),
                    p.getRelasjonsbeskrivelse(),
                    readOnly,
                    p.getResultat(),
                    mapResultatEtterAutomatikk(p.getResultat(), ikkeVurdertBlirOppfylt));

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
}
