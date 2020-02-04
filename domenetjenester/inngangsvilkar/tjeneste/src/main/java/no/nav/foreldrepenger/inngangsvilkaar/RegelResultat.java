package no.nav.foreldrepenger.inngangsvilkaar;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;

public class RegelResultat {
    private final Vilkårene vilkårene;
    private final List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner;

    private final Map<VilkårType, Map<DatoIntervallEntitet, Object>> ekstraResultater;

    public RegelResultat(Vilkårene vilkårene, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner,
                         Map<VilkårType, Map<DatoIntervallEntitet, Object>> ekstraResultater) {
        this.vilkårene = vilkårene;
        this.aksjonspunktDefinisjoner = aksjonspunktDefinisjoner;
        this.ekstraResultater = ekstraResultater;
    }

    public Vilkårene getVilkårene() {
        return vilkårene;
    }

    public Map<VilkårType, Object> getEkstraResultater() {
        return Collections.unmodifiableMap(ekstraResultater);
    }

    public Map<VilkårType, Map<DatoIntervallEntitet, Object>> getEkstraResultaterPerPeriode() {
        return Collections.unmodifiableMap(ekstraResultater);
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktDefinisjoner() {
        return aksjonspunktDefinisjoner;
    }

    public <V> Optional<V> getEkstraResultat(VilkårType vilkårType) {
        @SuppressWarnings("unchecked")
        V val = (V) ekstraResultater.get(vilkårType);
        return Optional.ofNullable(val);
    }
}
