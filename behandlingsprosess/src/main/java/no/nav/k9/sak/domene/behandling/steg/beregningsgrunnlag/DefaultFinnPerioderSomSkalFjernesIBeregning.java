package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.HashSet;
import java.util.Set;

public class DefaultFinnPerioderSomSkalFjernesIBeregning implements FinnPerioderSomSkalFjernesIBeregning {
    @Override
    public Set<DatoIntervallEntitet> finnPerioderMedAvslåtteVilkår(VilkårBuilder vilkårBuilder, Vilkårene vilkårene, BehandlingReferanse behandlingReferanse) {
        return new HashSet<>();
    }
}
