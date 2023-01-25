package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.MANGLER_VURDERING;

import java.util.Objects;

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
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;

@Dependent
public class VurderNødvendighetTjeneste {

    private final VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final VurdertOpplæringRepository vurdertOpplæringRepository;
    private final UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private final VurderNødvendighetTidslinjeUtleder tidslinjeUtleder;

    @Inject
    public VurderNødvendighetTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                      @FagsakYtelseTypeRef(OPPLÆRINGSPENGER) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                      VurdertOpplæringRepository vurdertOpplæringRepository,
                                      UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository) {
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.tidslinjeUtleder = new VurderNødvendighetTidslinjeUtleder();
    }

    public Aksjon vurder(BehandlingReferanse referanse) {

        var tidslinje = hentTidslinjeMedVurdering(referanse);

        if (tidslinje.stream().anyMatch(segment -> Objects.equals(segment.getValue(), MANGLER_VURDERING))) {
            return Aksjon.TRENGER_AVKLARING;
        }

        return Aksjon.FORTSETT;
    }

    public void lagreVilkårsResultat(BehandlingReferanse referanse) {
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer());
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.NØDVENDIG_OPPLÆRING);

        var tidslinje = hentTidslinjeMedVurdering(referanse);
        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.NØDVENDIG_OPPLÆRING);
        var tidslinjeSomSkalFjernes = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering).disjoint(tidslinje);
        leggTilVilkårsresultatTidligereVilkår(vilkårBuilder, tidslinjeSomSkalFjernes);

        leggTilVilkårsresultatNødvendighet(vilkårBuilder, tidslinje);

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(referanse.getBehandlingId(), vilkårResultatBuilder.build());
    }

    private void leggTilVilkårsresultatTidligereVilkår(VilkårBuilder vilkårBuilder, LocalDateTimeline<Boolean> tidslinjeSomSkalFjernes) {
        tidslinjeSomSkalFjernes.compress().forEach(segment -> vilkårBuilder.tilbakestill(DatoIntervallEntitet.fra(segment.getLocalDateInterval())));
    }

    private void leggTilVilkårsresultatNødvendighet(VilkårBuilder vilkårBuilder, LocalDateTimeline<NødvendighetGodkjenningStatus> tidslinje) {
        var godkjentTidslinje = tidslinje.filterValue(value -> Objects.equals(value, GODKJENT));
        var ikkeGodkjentTidslinje = tidslinje.filterValue(value -> Objects.equals(value, IKKE_GODKJENT));

        leggTilVilkårResultat(vilkårBuilder, godkjentTidslinje, Utfall.OPPFYLT, Avslagsårsak.UDEFINERT);
        leggTilVilkårResultat(vilkårBuilder, ikkeGodkjentTidslinje, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_NØDVENDIG_OPPLÆRING);
    }

    private void leggTilVilkårResultat(VilkårBuilder vilkårBuilder, LocalDateTimeline<?> tidslinje, Utfall utfall, Avslagsårsak avslagsårsak) {
        TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje)
            .forEach(datoIntervallEntitet -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(datoIntervallEntitet)
                .medUtfall(utfall)
                .medAvslagsårsak(avslagsårsak)));
    }

    private LocalDateTimeline<NødvendighetGodkjenningStatus> hentTidslinjeMedVurdering(BehandlingReferanse referanse) {
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        var uttaksPerioderGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId()).orElseThrow();
        var perioderFraSøknad = uttaksPerioderGrunnlag.getRelevantSøknadsperioder().getPerioderFraSøknadene();

        var vurdertOpplæringGrunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(referanse.getBehandlingId());

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.NØDVENDIG_OPPLÆRING);
        var tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering);

        return tidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag.orElse(null), tidslinjeTilVurdering);
    }
}
