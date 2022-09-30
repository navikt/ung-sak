package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.NødvendighetVilkår;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.NødvendighetVilkårGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.NødvendighetVilkårResultat;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;

public class NødvendighetVilkårTjeneste {

    private final NødvendighetVilkårOversetter vilkårOversetter = new NødvendighetVilkårOversetter();
    private final VilkårUtfallOversetter utfallOversetter = new VilkårUtfallOversetter();

    /**
     * Vurderer perioder mot vilkårsreglene og oppdaterer builder med resultatet. Returnerer de vurderte periodene.
     *
     * @param vilkårBuilder             Vilkårbuilder
     * @param vurdertOpplæringGrunnlag  Vurdert opplæring @Nullable
     * @param tidslinjeTilVurdering     Tidslinje til vurdering
     * @param sykdomsTidslinje          Tidslinje med godkjent sykdomsvilkår
     * @return                          Sett med de vurderte periodene
     */
    public NavigableSet<DatoIntervallEntitet> vurderPerioder(VilkårBuilder vilkårBuilder, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDateTimeline<Boolean> tidslinjeTilVurdering, LocalDateTimeline<Boolean> sykdomsTidslinje) {
        Objects.requireNonNull(vilkårBuilder);
        Objects.requireNonNull(tidslinjeTilVurdering);
        Objects.requireNonNull(sykdomsTidslinje);

        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = new TreeSet<>();

        if (vurdertOpplæringGrunnlag != null) {
            for (VurdertOpplæring vurdertOpplæring : vurdertOpplæringGrunnlag.getVurdertOpplæringHolder().getVurdertOpplæring()) {
                perioderTilVurdering.add(vurdertOpplæring.getPeriode());
            }
        }

        LocalDateTimeline<Boolean> perioderTilVurderingTidslinje = new LocalDateTimeline<>(perioderTilVurdering.stream()
            .map(periode -> new LocalDateSegment<>(periode.getFomDato(), periode.getTomDato(), true))
            .toList())
            .intersection(tidslinjeTilVurdering);

        LocalDateTimeline<Boolean> tidslinjeTilVurderingMedSykdom = perioderTilVurderingTidslinje.intersection(sykdomsTidslinje);
        LocalDateTimeline<Boolean> tidslinjeTilVurderingUtenSykdom = perioderTilVurderingTidslinje.disjoint(sykdomsTidslinje);
        LocalDateTimeline<Boolean> tidslinjeTilVurderingUtenSykdomOgUtenVurdertOpplæring = tidslinjeTilVurdering.disjoint(sykdomsTidslinje).disjoint(perioderTilVurderingTidslinje);

        perioderTilVurderingTidslinje = tidslinjeTilVurderingMedSykdom
            .union(tidslinjeTilVurderingUtenSykdom, StandardCombinators::coalesceRightHandSide)
            .union(tidslinjeTilVurderingUtenSykdomOgUtenVurdertOpplæring, StandardCombinators::coalesceRightHandSide);

        perioderTilVurdering = TidslinjeUtil.tilDatoIntervallEntiteter(perioderTilVurderingTidslinje);

        perioderTilVurdering.forEach(periode -> {
            final VilkårData vilkårData = vurderPeriode(periode, vurdertOpplæringGrunnlag, sykdomsTidslinje);
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(vilkårData.getPeriode())
                .medUtfall(vilkårData.getUtfallType())
                .medRegelEvaluering(vilkårData.getRegelEvaluering())
                .medRegelInput(vilkårData.getRegelInput())
                .medAvslagsårsak(vilkårData.getAvslagsårsak()));
        });

        return perioderTilVurdering;
    }

    private VilkårData vurderPeriode(DatoIntervallEntitet periodeTilVurdering, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDateTimeline<Boolean> sykdomsTidslinje) {
        NødvendighetVilkårGrunnlag vilkårGrunnlag = vilkårOversetter.oversettTilRegelModell(periodeTilVurdering, vurdertOpplæringGrunnlag, sykdomsTidslinje);
        NødvendighetVilkårResultat resultat = new NødvendighetVilkårResultat();

        final Evaluation evaluation = new NødvendighetVilkår().evaluer(vilkårGrunnlag, resultat);

        final VilkårData vilkårData = utfallOversetter.oversett(VilkårType.NØDVENDIG_OPPLÆRING, evaluation, vilkårGrunnlag, periodeTilVurdering);
        vilkårData.setEkstraVilkårresultat(resultat);
        return vilkårData;
    }
}
