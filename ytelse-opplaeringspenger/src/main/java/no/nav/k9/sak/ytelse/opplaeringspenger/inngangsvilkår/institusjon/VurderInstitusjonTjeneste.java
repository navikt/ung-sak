package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.InstitusjonGodkjenningStatus.MANGLER_VURDERING;

import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.Aksjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

@Dependent
public class VurderInstitusjonTjeneste {

    private final VurderInstitusjonTidslinjeUtleder tidslinjeUtleder;
    private final VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private final VurdertOpplæringRepository vurdertOpplæringRepository;
    private final UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private final VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public VurderInstitusjonTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                     GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste,
                                     @FagsakYtelseTypeRef(OPPLÆRINGSPENGER) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                     VurdertOpplæringRepository vurdertOpplæringRepository,
                                     UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository) {
        this.tidslinjeUtleder = new VurderInstitusjonTidslinjeUtleder(godkjentOpplæringsinstitusjonTjeneste);
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    public Aksjon vurder(BehandlingReferanse referanse) {
        Optional<UttaksPerioderGrunnlag> uttaksPerioderGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId());
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.NØDVENDIG_OPPLÆRING);
        Optional<VurdertOpplæringGrunnlag> vurdertOpplæringGrunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(referanse.getBehandlingId());

        Set<PerioderFraSøknad> perioderFraSøknad = uttaksPerioderGrunnlag.map(UttaksPerioderGrunnlag::getRelevantSøknadsperioder)
            .map(UttakPerioderHolder::getPerioderFraSøknadene)
            .orElse(Set.of());

        var tidslinje = tidslinjeUtleder.utled(perioderFraSøknad, vurdertOpplæringGrunnlag.orElse(null), perioderTilVurdering);

        if (tidslinje.filterValue(godkjenning -> Objects.equals(godkjenning, MANGLER_VURDERING)).stream()
            .findFirst()
            .isPresent()) {
            return Aksjon.TRENGER_AVKLARING;
        }

        lagreVilkårsResultat(referanse, tidslinje);

        return Aksjon.FORTSETT;
    }

    private void lagreVilkårsResultat(BehandlingReferanse referanse, LocalDateTimeline<InstitusjonGodkjenningStatus> tidslinje) {
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand());
        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON);

        var godkjentTidslinje = tidslinje.filterValue(godkjenning -> Objects.equals(godkjenning, GODKJENT));
        var ikkeGodkjentTidslinje = tidslinje.filterValue(godkjenning -> Objects.equals(godkjenning, IKKE_GODKJENT));

        leggTilVilkårResultat(vilkårBuilder, godkjentTidslinje, Utfall.OPPFYLT, Avslagsårsak.UDEFINERT);
        leggTilVilkårResultat(vilkårBuilder, ikkeGodkjentTidslinje, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_GODKJENT_INSTITUSJON);

        resultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(referanse.getBehandlingId(), resultatBuilder.build());
    }

    private void leggTilVilkårResultat(VilkårBuilder vilkårBuilder, LocalDateTimeline<InstitusjonGodkjenningStatus> tidslinje, Utfall utfall, Avslagsårsak avslagsårsak) {
        TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje)
            .forEach(datoIntervallEntitet -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(datoIntervallEntitet)
                .medUtfall(utfall)
                .medAvslagsårsak(avslagsårsak)));
    }
}
