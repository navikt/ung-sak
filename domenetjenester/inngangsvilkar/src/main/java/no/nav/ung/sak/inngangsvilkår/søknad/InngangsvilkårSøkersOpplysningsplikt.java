package no.nav.ung.sak.inngangsvilkår.søknad;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.inngangsvilkår.Inngangsvilkår;
import no.nav.ung.sak.inngangsvilkår.VilkårData;

@ApplicationScoped
@VilkårTypeRef(VilkårType.SØKERSOPPLYSNINGSPLIKT)
public class InngangsvilkårSøkersOpplysningsplikt implements Inngangsvilkår {

    @Inject
    public InngangsvilkårSøkersOpplysningsplikt() {
    }

    @Override
    public NavigableMap<DatoIntervallEntitet, VilkårData> vurderVilkår(BehandlingReferanse ref, Collection<DatoIntervallEntitet> perioder) {
        if (perioder.isEmpty()) {
            return Collections.emptyNavigableMap();
        }
        // TODO: fjern vilkår eller implementer logikk
        Map<DatoIntervallEntitet, VilkårData> resultat = perioder.stream()
            .collect(Collectors.toMap(p -> p, p -> new VilkårData(p, VilkårType.SØKERSOPPLYSNINGSPLIKT, Utfall.OPPFYLT, Collections.emptyList())));
        return new TreeMap<>(resultat);

    }

}
