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

        lagreVilkårsResultat(referanse.getBehandlingId(), vilkårene, perioderTilVurdering, vurdertOpplæringGrunnlag);

        return Aksjon.FORTSETT;
    }

    //TODO: Bør dette ligge et annet sted?
    private void lagreVilkårsResultat(Long behandlingId, Vilkårene vilkårene, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag) {
        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand());
        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.GJENNOMGÅ_OPPLÆRING);

        var tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering);

        var sykdomsTidslinje = OppfyltVilkårTidslinjeUtleder.utled(vilkårene, VilkårType.LANGVARIG_SYKDOM);
        var godkjentInstitusjonTidslinje = OppfyltVilkårTidslinjeUtleder.utled(vilkårene, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON);

        var tidslinjeUtenGodkjentInstitusjon = tidslinjeTilVurdering.disjoint(godkjentInstitusjonTidslinje);
        var tidslinjeUtenSykdomsvilkår = tidslinjeTilVurdering.disjoint(sykdomsTidslinje).disjoint(tidslinjeUtenGodkjentInstitusjon);

        leggTilVilkårResultat(vilkårBuilder, tidslinjeUtenGodkjentInstitusjon, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_GODKJENT_INSTITUSJON);
        leggTilVilkårResultat(vilkårBuilder, tidslinjeUtenSykdomsvilkår, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE);

        tidslinjeTilVurdering = tidslinjeTilVurdering.disjoint(tidslinjeUtenGodkjentInstitusjon).disjoint(tidslinjeUtenSykdomsvilkår);

        if (vurdertOpplæringGrunnlag != null && vurdertOpplæringGrunnlag.getVurdertePerioder() != null) {
            var vurdertTidslinjeTilVurdering = vurdertOpplæringGrunnlag.getVurdertePerioder().getTidslinje().intersection(tidslinjeTilVurdering);
            var godkjentTidslinjeTilVurdering = vurdertTidslinjeTilVurdering.filterValue(VurdertOpplæringPeriode::getGjennomførtOpplæring);
            var ikkeGodkjentTidslinjeTilVurdering = vurdertTidslinjeTilVurdering.disjoint(godkjentTidslinjeTilVurdering);

            leggTilVilkårResultat(vilkårBuilder, godkjentTidslinjeTilVurdering, Utfall.OPPFYLT, Avslagsårsak.UDEFINERT);
            leggTilVilkårResultat(vilkårBuilder, ikkeGodkjentTidslinjeTilVurdering, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_GJENNOMGÅTT_OPPLÆRING);
        }

        resultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, resultatBuilder.build());
    }

    private static void leggTilVilkårResultat(VilkårBuilder vilkårBuilder, LocalDateTimeline<?> tidslinje, Utfall utfall, Avslagsårsak avslagsårsak) {
        TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje)
            .forEach(datoIntervallEntitet -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(datoIntervallEntitet)
                .medUtfall(utfall)
                .medAvslagsårsak(avslagsårsak)));
    }
}
