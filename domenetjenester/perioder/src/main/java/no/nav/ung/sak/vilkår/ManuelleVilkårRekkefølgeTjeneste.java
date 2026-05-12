package no.nav.ung.sak.vilkår;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandlingModell;
import no.nav.ung.sak.behandlingskontroll.impl.BehandlingModellRepository;

import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class ManuelleVilkårRekkefølgeTjeneste {

    private final BehandlingModellRepository behandlingModellRepository;

    @Inject
    public ManuelleVilkårRekkefølgeTjeneste(BehandlingModellRepository behandlingModellRepository) {
        this.behandlingModellRepository = behandlingModellRepository;
    }

    // finner vilkår som er tidligere i prosessen enn vilkåret som sendes inn som parameter
    // finner bare vilkår som er knyttet til aksjonspunkt, så helautomatiske vilkår må identifieres på annen måte
    public Set<VilkårType> finnManuelleVilkårSomErFør(VilkårType vilkårType, FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        BehandlingModell modell = behandlingModellRepository.getModell(behandlingType, ytelseType);
        return modell.getAlleBehandlingStegTyper().stream()
            .filter(steg -> !steg.getAksjonspunktDefinisjoner().isEmpty())
            .takeWhile(steg -> steg.getAksjonspunktDefinisjoner().stream().noneMatch(ap->ap.getVilkårType() == vilkårType))
            .flatMap(steg -> steg.getAksjonspunktDefinisjoner().stream())
            .map(AksjonspunktDefinisjon::getVilkårType)
            .filter(v -> v != null)
            .collect(Collectors.toSet());
    }
}
