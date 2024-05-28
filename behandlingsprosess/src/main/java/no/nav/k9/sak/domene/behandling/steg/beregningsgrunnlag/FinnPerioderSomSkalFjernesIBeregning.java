package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.Set;

public interface FinnPerioderSomSkalFjernesIBeregning {
    static FinnPerioderSomSkalFjernesIBeregning getFinnPerioderSkalIgnoreresIBeregning(BehandlingReferanse ref) {
        return FagsakYtelseTypeRef.Lookup.find(FinnPerioderSomSkalFjernesIBeregning.class, ref.getFagsakYtelseType())
            .orElse(new DefaultFinnPerioderSomSkalFjernesIBeregning());
    }

    Set<DatoIntervallEntitet> finnPerioderSomSkalFjernes(Vilkårene vilkårene, BehandlingReferanse behandlingReferanse);
}
