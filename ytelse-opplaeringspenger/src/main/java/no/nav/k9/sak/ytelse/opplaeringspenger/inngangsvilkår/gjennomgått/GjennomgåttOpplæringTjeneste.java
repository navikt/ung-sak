package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;

import java.util.NavigableSet;

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
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.OppfyltVilkårTidslinjeUtleder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

@Dependent
public class GjennomgåttOpplæringTjeneste {

    private final VilkårResultatRepository vilkårResultatRepository;
    private final OpplæringPeriodeSomTrengerVurderingUtleder periodeUtleder;
    private final VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private final UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private final VurdertOpplæringRepository vurdertOpplæringRepository;

    @Inject
    public GjennomgåttOpplæringTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                        @FagsakYtelseTypeRef(OPPLÆRINGSPENGER) VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                        UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                        VurdertOpplæringRepository vurdertOpplæringRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
        this.periodeUtleder = new OpplæringPeriodeSomTrengerVurderingUtleder();
    }

    public Aksjon vurder(BehandlingReferanse referanse) {

        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.GJENNOMGÅ_OPPLÆRING);

        var uttaksPerioderGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId()).orElseThrow();

        var vurdertOpplæringGrunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(referanse.getBehandlingId()).orElse(null);

        if (periodeUtleder.trengerVurderingFraSaksbehandler(perioderTilVurdering, vilkårene, vurdertOpplæringGrunnlag, uttaksPerioderGrunnlag)) {
            return Aksjon.TRENGER_AVKLARING;
        }

        return Aksjon.FORTSETT;
    }

    public void lagreVilkårsResultat(BehandlingReferanse referanse) {

        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.GJENNOMGÅ_OPPLÆRING);

        var uttaksPerioderGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId()).orElseThrow();

        var vurdertOpplæringGrunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(referanse.getBehandlingId()).orElse(null);

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand());
        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.GJENNOMGÅ_OPPLÆRING);

        var tidslinjeTilVurdering = new TidslinjeTilVurdering(perioderTilVurdering);

        leggTilVilkårsresultatTidligereVilkår(vilkårene, vilkårBuilder, tidslinjeTilVurdering);

        leggTilVilkårsresultatOpplæring(vurdertOpplæringGrunnlag, vilkårBuilder, tidslinjeTilVurdering);

        leggTilVilkårsresultatReisetid(uttaksPerioderGrunnlag, vurdertOpplæringGrunnlag, vilkårBuilder, tidslinjeTilVurdering);

        resultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(referanse.getBehandlingId(), resultatBuilder.build());
    }

    private void leggTilVilkårsresultatTidligereVilkår(Vilkårene vilkårene, VilkårBuilder vilkårBuilder, TidslinjeTilVurdering tidslinjeTilVurdering) {
        var sykdomsTidslinje = OppfyltVilkårTidslinjeUtleder.utled(vilkårene, VilkårType.LANGVARIG_SYKDOM);
        var godkjentInstitusjonTidslinje = OppfyltVilkårTidslinjeUtleder.utled(vilkårene, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON);

        var tidslinjeUtenGodkjentInstitusjon = tidslinjeTilVurdering.get().disjoint(godkjentInstitusjonTidslinje);
        leggTilVilkårResultat(vilkårBuilder, tidslinjeUtenGodkjentInstitusjon, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_GODKJENT_INSTITUSJON);
        tidslinjeTilVurdering.registrerVurdert(tidslinjeUtenGodkjentInstitusjon);

        var tidslinjeUtenSykdomsvilkår = tidslinjeTilVurdering.get().disjoint(sykdomsTidslinje);
        leggTilVilkårResultat(vilkårBuilder, tidslinjeUtenSykdomsvilkår, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE);
        tidslinjeTilVurdering.registrerVurdert(tidslinjeUtenSykdomsvilkår);
    }

    private void leggTilVilkårsresultatOpplæring(VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, VilkårBuilder vilkårBuilder, TidslinjeTilVurdering tidslinjeTilVurdering) {
        if (vurdertOpplæringGrunnlag != null && vurdertOpplæringGrunnlag.getVurdertePerioder() != null) {
            var vurdertTidslinjeTilVurdering = vurdertOpplæringGrunnlag.getVurdertePerioder().getTidslinjeOpplæring().intersection(tidslinjeTilVurdering.get());
            var godkjentTidslinjeTilVurdering = vurdertTidslinjeTilVurdering.filterValue(VurdertOpplæringPeriode::getGjennomførtOpplæring);
            var ikkeGodkjentTidslinjeTilVurdering = vurdertTidslinjeTilVurdering.disjoint(godkjentTidslinjeTilVurdering);

            leggTilVilkårResultat(vilkårBuilder, godkjentTidslinjeTilVurdering, Utfall.OPPFYLT, Avslagsårsak.UDEFINERT);
            tidslinjeTilVurdering.registrerVurdert(godkjentTidslinjeTilVurdering);

            leggTilVilkårResultat(vilkårBuilder, ikkeGodkjentTidslinjeTilVurdering, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_GJENNOMGÅTT_OPPLÆRING);
            tidslinjeTilVurdering.registrerVurdert(ikkeGodkjentTidslinjeTilVurdering);
        }
    }

    private void leggTilVilkårsresultatReisetid(UttaksPerioderGrunnlag uttaksPerioderGrunnlag, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, VilkårBuilder vilkårBuilder, TidslinjeTilVurdering tidslinjeTilVurdering) {
        var godkjentReisetidTidslinje = ReisetidUtleder.utledGodkjentReisetid(uttaksPerioderGrunnlag, vurdertOpplæringGrunnlag).intersection(tidslinjeTilVurdering.get());
        leggTilVilkårResultat(vilkårBuilder, godkjentReisetidTidslinje, Utfall.OPPFYLT, Avslagsårsak.UDEFINERT);
        tidslinjeTilVurdering.registrerVurdert(godkjentReisetidTidslinje);

        var ikkeGodkjentReisetidTidslinje = ReisetidUtleder.finnOppgittReisetid(uttaksPerioderGrunnlag).intersection(tidslinjeTilVurdering.get());
        leggTilVilkårResultat(vilkårBuilder, ikkeGodkjentReisetidTidslinje, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_GODKJENT_REISETID);
        tidslinjeTilVurdering.registrerVurdert(ikkeGodkjentReisetidTidslinje);
    }

    private static void leggTilVilkårResultat(VilkårBuilder vilkårBuilder, LocalDateTimeline<?> tidslinje, Utfall utfall, Avslagsårsak avslagsårsak) {
        TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje)
            .forEach(datoIntervallEntitet -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(datoIntervallEntitet)
                .medUtfall(utfall)
                .medAvslagsårsak(avslagsårsak)));
    }

    private static class TidslinjeTilVurdering {
        private LocalDateTimeline<Boolean> tidslinje;

        TidslinjeTilVurdering(NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
            this.tidslinje = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering);
        }

        LocalDateTimeline<Boolean> get() {
            return tidslinje;
        }

        void registrerVurdert(LocalDateTimeline<?> vurdertTidslinje) {
            tidslinje = tidslinje.disjoint(vurdertTidslinje);
        }
    }
}
