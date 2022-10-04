package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
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
     * @param vurdertOpplæringGrunnlag  Vurdert opplæring
     * @param tidslinjeTilVurdering     Tidslinje til vurdering
     * @return                          Sett med de vurderte periodene
     */
    public NavigableSet<DatoIntervallEntitet> vurderPerioder(VilkårBuilder vilkårBuilder, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        Objects.requireNonNull(vilkårBuilder);
        Objects.requireNonNull(vurdertOpplæringGrunnlag);
        Objects.requireNonNull(tidslinjeTilVurdering);

        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = finnPerioderTilVurderingFraGrunnlag(vurdertOpplæringGrunnlag, tidslinjeTilVurdering);

        perioderTilVurdering.forEach(periode -> {
            final VilkårData vilkårData = vurderPeriode(periode, vurdertOpplæringGrunnlag);
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(vilkårData.getPeriode())
                .medUtfall(vilkårData.getUtfallType())
                .medRegelEvaluering(vilkårData.getRegelEvaluering())
                .medRegelInput(vilkårData.getRegelInput())
                .medAvslagsårsak(vilkårData.getAvslagsårsak()));
        });

        return perioderTilVurdering;
    }

    private VilkårData vurderPeriode(DatoIntervallEntitet periodeTilVurdering, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag) {
        NødvendighetVilkårGrunnlag vilkårGrunnlag = vilkårOversetter.oversettTilRegelModell(periodeTilVurdering, vurdertOpplæringGrunnlag);
        NødvendighetVilkårResultat resultat = new NødvendighetVilkårResultat();

        final Evaluation evaluation = new NødvendighetVilkår().evaluer(vilkårGrunnlag, resultat);

        final VilkårData vilkårData = utfallOversetter.oversett(VilkårType.NØDVENDIG_OPPLÆRING, evaluation, vilkårGrunnlag, periodeTilVurdering);
        vilkårData.setEkstraVilkårresultat(resultat);
        return vilkårData;
    }

    private NavigableSet<DatoIntervallEntitet> finnPerioderTilVurderingFraGrunnlag(VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        NavigableSet<DatoIntervallEntitet> perioderTilVurderingFraGrunnlag = new TreeSet<>();

        for (VurdertOpplæring vurdertOpplæring : vurdertOpplæringGrunnlag.getVurdertOpplæringHolder().getVurdertOpplæring()) {
            perioderTilVurderingFraGrunnlag.add(vurdertOpplæring.getPeriode());
        }

        LocalDateTimeline<Boolean> perioderTilVurderingTidslinje = new LocalDateTimeline<>(perioderTilVurderingFraGrunnlag.stream()
            .map(periode -> new LocalDateSegment<>(periode.getFomDato(), periode.getTomDato(), true))
            .toList())
            .intersection(tidslinjeTilVurdering);

        return TidslinjeUtil.tilDatoIntervallEntiteter(perioderTilVurderingTidslinje);
    }
}
