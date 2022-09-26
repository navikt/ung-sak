package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import java.util.List;
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
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjonHolder;
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

        List<VurdertOpplæring> vurdertOpplæringList = vurdertOpplæringGrunnlag.getVurdertOpplæringHolder().getVurdertOpplæring();
        VurdertInstitusjonHolder vurdertInstitusjonHolder = vurdertOpplæringGrunnlag.getVurdertInstitusjonHolder();

        NavigableSet<DatoIntervallEntitet> godkjentInstitusjonPerioder = new TreeSet<>();
        NavigableSet<DatoIntervallEntitet> nødvendigOpplæringPerioder = new TreeSet<>();
        NavigableSet<DatoIntervallEntitet> vurdertePerioder = new TreeSet<>();

        for (VurdertOpplæring vurdertOpplæring : vurdertOpplæringList) {
            DatoIntervallEntitet datoIntervallEntitet = vurdertOpplæring.getPeriode();

            boolean godkjentInstitusjon = vurdertInstitusjonHolder.finnVurdertInstitusjon(vurdertOpplæring.getInstitusjon())
                .map(VurdertInstitusjon::getGodkjent)
                .orElse(false);

            if (godkjentInstitusjon) {
                godkjentInstitusjonPerioder.add(datoIntervallEntitet);
            }

            boolean nødvendigOpplæring = vurdertOpplæring.getNødvendigOpplæring();

            if (nødvendigOpplæring) {
                nødvendigOpplæringPerioder.add(datoIntervallEntitet);
            }

            vurdertePerioder.add(datoIntervallEntitet);
        }

        LocalDateTimeline<Boolean> godkjentInstitusjonTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(godkjentInstitusjonPerioder)
            .intersection(tidslinjeTilVurdering);

        LocalDateTimeline<Boolean> nødvendigOpplæringTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(nødvendigOpplæringPerioder)
            .intersection(tidslinjeTilVurdering);

        LocalDateTimeline<Boolean> relevanteVurdertePerioderTidslinje = new LocalDateTimeline<>(vurdertePerioder.stream()
            .map(vurdertPeriode -> new LocalDateSegment<>(vurdertPeriode.getFomDato(), vurdertPeriode.getTomDato(), true))
            .toList())
            .intersection(tidslinjeTilVurdering);

        vurdertePerioder = TidslinjeUtil.tilDatoIntervallEntiteter(relevanteVurdertePerioderTidslinje);

        vurderPerioder(vilkårBuilder, vurdertePerioder, nødvendigOpplæringTidslinje, godkjentInstitusjonTidslinje);

        return vurdertePerioder;
    }

    private void vurderPerioder(VilkårBuilder vilkårBuilder, NavigableSet<DatoIntervallEntitet> vurdertePerioder,
                                LocalDateTimeline<Boolean> nødvendigOpplæringTidslinje,
                                LocalDateTimeline<Boolean> godkjentInstitusjonTidslinje) {
        for (DatoIntervallEntitet periode : vurdertePerioder) {
            VilkårData vilkårData = vurderPeriode(periode, nødvendigOpplæringTidslinje, godkjentInstitusjonTidslinje);
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(vilkårData.getPeriode())
                .medUtfall(vilkårData.getUtfallType())
                .medRegelEvaluering(vilkårData.getRegelEvaluering())
                .medRegelInput(vilkårData.getRegelInput())
                .medAvslagsårsak(vilkårData.getAvslagsårsak()));
        }
    }

    private VilkårData vurderPeriode(DatoIntervallEntitet periodeTilVurdering,
                                    LocalDateTimeline<Boolean> nødvendigOpplæringTidslinje,
                                    LocalDateTimeline<Boolean> godkjentInstitusjonTidslinje) {
        NødvendighetVilkårGrunnlag vilkårGrunnlag = vilkårOversetter.oversettTilRegelModell(periodeTilVurdering, nødvendigOpplæringTidslinje, godkjentInstitusjonTidslinje);
        NødvendighetVilkårResultat resultat = new NødvendighetVilkårResultat();

        final Evaluation evaluation = new NødvendighetVilkår().evaluer(vilkårGrunnlag, resultat);

        final VilkårData vilkårData = utfallOversetter.oversett(VilkårType.NØDVENDIG_OPPLÆRING, evaluation, vilkårGrunnlag, periodeTilVurdering);
        vilkårData.setEkstraVilkårresultat(resultat);
        return vilkårData;
    }
}
