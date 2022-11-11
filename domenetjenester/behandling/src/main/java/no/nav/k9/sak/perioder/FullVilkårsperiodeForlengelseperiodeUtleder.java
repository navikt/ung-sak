package no.nav.k9.sak.perioder;

import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef
@VilkårTypeRef
public class FullVilkårsperiodeForlengelseperiodeUtleder implements ForlengelseperiodeUtleder {


    @Override
    public NavigableSet<DatoIntervallEntitet> utledForlengelseperioder(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode) {
        return new TreeSet<>(Set.of(periode));
    }
}
