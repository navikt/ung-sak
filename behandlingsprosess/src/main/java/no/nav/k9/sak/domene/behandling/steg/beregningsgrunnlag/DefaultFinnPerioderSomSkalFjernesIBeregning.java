package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.Set;

public class DefaultFinnPerioderSomSkalFjernesIBeregning implements FinnPerioderSomSkalFjernesIBeregning {
    @Override
    public Set<DatoIntervallEntitet> finnPerioderSomSkalFjernes(Vilkårene vilkårene, BehandlingReferanse behandlingReferanse, Set<DatoIntervallEntitet> vilkårperioder) {
        return Set.of();
    }
}
