package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.IKKE_GODKJENT_REISETID;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått.OpplæringGodkjenningStatus.MANGLER_VURDERING;

import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.Aksjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;

@Dependent
public class GjennomgåttOpplæringTjeneste {

    private final VilkårResultatRepository vilkårResultatRepository;
    private final VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private final UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private final VurdertOpplæringRepository vurdertOpplæringRepository;
    private final GjennomgåttOpplæringTidslinjeUtleder tidslinjeUtleder;

    @Inject
    public GjennomgåttOpplæringTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                        @FagsakYtelseTypeRef(OPPLÆRINGSPENGER) VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                        UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                        VurdertOpplæringRepository vurdertOpplæringRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
        this.tidslinjeUtleder = new GjennomgåttOpplæringTidslinjeUtleder();
    }

    public Aksjon vurder(BehandlingReferanse referanse) {

        var tidslinje = hentTidslinjeMedVurdering(referanse);

        if (tidslinje.stream().anyMatch(it -> Objects.equals(it.getValue(), MANGLER_VURDERING))) {
            return Aksjon.TRENGER_AVKLARING;
        }

        return Aksjon.FORTSETT;
    }

    public void lagreVilkårsResultat(BehandlingReferanse referanse) {
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer());
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.GJENNOMGÅ_OPPLÆRING);

        var tidslinje = hentTidslinjeMedVurdering(referanse);
        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.GJENNOMGÅ_OPPLÆRING);
        var tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering);

        klippKortPerioderSomIkkeHarBehandlingsgrunnlag(vilkårBuilder, tidslinjeTilVurdering.disjoint(tidslinje));

        leggTilVilkårsresultatReisetid(vilkårBuilder, tidslinje);
        leggTilVilkårsresultatgjennomgåttOpplæring(vilkårBuilder, tidslinje);

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(referanse.getBehandlingId(), vilkårResultatBuilder.build());
    }

    private void klippKortPerioderSomIkkeHarBehandlingsgrunnlag(VilkårBuilder vilkårBuilder, LocalDateTimeline<Boolean> tidslinje) {
        // Klipper bort periodene, det er ingen grunn til å vurdere vilkåret der hvor man ikke har behandlingsgrunnlag
        tidslinje.compress().forEach(segment -> vilkårBuilder.tilbakestill(DatoIntervallEntitet.fra(segment.getLocalDateInterval())));
    }

    private void leggTilVilkårsresultatgjennomgåttOpplæring(VilkårBuilder vilkårBuilder, LocalDateTimeline<OpplæringGodkjenningStatus> tidslinje) {
        var godkjentTidslinje = tidslinje.filterValue(value -> Objects.equals(value, GODKJENT));
        var ikkeGodkjentTidslinje = tidslinje.filterValue(value -> Objects.equals(value, IKKE_GODKJENT));

        leggTilVilkårResultat(vilkårBuilder, ikkeGodkjentTidslinje, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_GJENNOMGÅTT_OPPLÆRING);
        leggTilVilkårResultat(vilkårBuilder, godkjentTidslinje, Utfall.OPPFYLT, Avslagsårsak.UDEFINERT);
    }

    private void leggTilVilkårsresultatReisetid(VilkårBuilder vilkårBuilder, LocalDateTimeline<OpplæringGodkjenningStatus> tidslinje) {
        var ikkeGodkjentReisetidTidslinje = tidslinje.filterValue(value -> Objects.equals(value, IKKE_GODKJENT_REISETID));

        leggTilVilkårResultat(vilkårBuilder, ikkeGodkjentReisetidTidslinje, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_PÅ_REISE);
    }

    private static void leggTilVilkårResultat(VilkårBuilder vilkårBuilder, LocalDateTimeline<?> tidslinje, Utfall utfall, Avslagsårsak avslagsårsak) {
        TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje)
            .forEach(datoIntervallEntitet -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(datoIntervallEntitet)
                .medUtfall(utfall)
                .medAvslagsårsak(avslagsårsak)));
    }

    private LocalDateTimeline<OpplæringGodkjenningStatus> hentTidslinjeMedVurdering(BehandlingReferanse referanse) {
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.GJENNOMGÅ_OPPLÆRING);
        var tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering);

        var uttaksPerioderGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId()).orElseThrow();
        var perioderFraSøknad = uttaksPerioderGrunnlag.getRelevantSøknadsperioder().getPerioderFraSøknadene();

        var vurdertOpplæringGrunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(referanse.getBehandlingId()).orElse(null);

        return tidslinjeUtleder.utled(vilkårene, perioderFraSøknad, vurdertOpplæringGrunnlag, tidslinjeTilVurdering);
    }
}
