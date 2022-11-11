package no.nav.k9.sak.perioder;

import java.util.NavigableSet;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface ForlengelseperiodeUtleder {

    public static ForlengelseperiodeUtleder finnUtleder(Instance<ForlengelseperiodeUtleder> instances, VilkårType vilkårType, FagsakYtelseType fagsakYtelseType) {
        var selected = instances.select(new VilkårTypeRef.VilkårTypeRefLiteral(vilkårType));
        if (selected.isAmbiguous()) {
            return FagsakYtelseTypeRef.Lookup.find(selected, fagsakYtelseType).orElseThrow(() -> new IllegalStateException("Har ikke ForlengelseperiodeUtleder for " + fagsakYtelseType));
        } else if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for vilkårtype:" + vilkårType);
        }

        var minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException(
                "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }

    public NavigableSet<DatoIntervallEntitet> utledForlengelseperioder(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode);
}
