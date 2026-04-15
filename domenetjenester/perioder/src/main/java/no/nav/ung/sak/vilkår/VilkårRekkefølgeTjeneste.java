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
public class VilkårRekkefølgeTjeneste {

    private final BehandlingModellRepository behandlingModellRepository;

    @Inject
    public VilkårRekkefølgeTjeneste(BehandlingModellRepository behandlingModellRepository) {
        this.behandlingModellRepository = behandlingModellRepository;
    }

    public Set<VilkårType> finnVilkårSomErFør(VilkårType vilkårType, FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        BehandlingModell modell = behandlingModellRepository.getModell(behandlingType, ytelseType);
        return modell.getAlleBehandlingStegTyper().stream()
            .filter(steg -> !steg.getAksjonspunktDefinisjoner().isEmpty())
            .takeWhile(steg -> !steg.getAksjonspunktDefinisjoner().contains(vilkårType))
            .flatMap(steg -> steg.getAksjonspunktDefinisjoner().stream())
            .map(AksjonspunktDefinisjon::getVilkårType)
            .collect(Collectors.toSet());
    }
}
