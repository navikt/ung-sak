package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår;

import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;

public class OppfyltVilkårTidslinjeUtleder {

    public static LocalDateTimeline<Boolean> utled(Vilkårene vilkårene, VilkårType vilkårType) {
        NavigableSet<DatoIntervallEntitet> oppfyltVilkårPerioder = vilkårene.getVilkår(vilkårType)
            .orElseThrow()
            .getPerioder()
            .stream()
            .filter(it -> Objects.equals(Utfall.OPPFYLT, it.getGjeldendeUtfall()))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet<DatoIntervallEntitet>::new));
        return TidslinjeUtil.tilTidslinjeKomprimert(oppfyltVilkårPerioder);
    }
}
