package no.nav.foreldrepenger.behandlingskontroll.impl;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;

/**
 * Håndterer aksjonspunktresultat og oppretter/reaktiverer aksjonspunkt
 * Brukes fra StegVisitor og Endringskontroller for lik håndtering
 */
public class AksjonspunktResultatOppretter {

    private final Behandling behandling;

    private final AksjonspunktRepository aksjonspunktRepository;

    public AksjonspunktResultatOppretter(AksjonspunktRepository aksjonspunktRepository, Behandling behandling) {
        this.behandling = Objects.requireNonNull(behandling, "behandling");
        this.aksjonspunktRepository = aksjonspunktRepository;
    }


    /**
     * Lagrer nye aksjonspunkt, og gjenåpner dem hvis de alleerede står til avbrutt/utført
     */
    public List<Aksjonspunkt> opprettAksjonspunkter(List<AksjonspunktResultat> apResultater, BehandlingStegType behandlingStegType) {

        if (!apResultater.isEmpty()) {
            List<Aksjonspunkt> funnetAksjonspunkter = new ArrayList<>();
            fjernGjensidigEkskluderendeAksjonspunkter(apResultater);
            funnetAksjonspunkter.addAll(leggTilNyeAksjonspunkterPåBehandling(behandlingStegType, apResultater, behandling));
            funnetAksjonspunkter.addAll(reåpneAvbrutteOgUtførteAksjonspunkter(apResultater, behandling));
            return funnetAksjonspunkter;
        } else {
            return new ArrayList<>();
        }
    }

    private void fjernGjensidigEkskluderendeAksjonspunkter(List<AksjonspunktResultat> nyeApResultater) {
        Set<String> nyeApDef = nyeApResultater.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).map(AksjonspunktDefinisjon::getKode).collect(toSet());
        behandling.getÅpneAksjonspunkter().stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().getUtelukkendeApdef().stream().anyMatch(nyeApDef::contains))
            .forEach(aksjonspunktRepository::setTilAvbrutt);
    }

    private List<Aksjonspunkt> reåpneAvbrutteOgUtførteAksjonspunkter(List<AksjonspunktResultat> nyeDefinisjoner,
                                                                     Behandling behandling) {
        List<Aksjonspunkt> reåpnedeAksjonspunkter = new ArrayList<>();

        Map<AksjonspunktDefinisjon, AksjonspunktResultat> aksjonspunktResultatMap = nyeDefinisjoner.stream()
            .collect(Collectors.toMap(AksjonspunktResultat::getAksjonspunktDefinisjon, Function.identity()));

        Set<Aksjonspunkt> skalReåpnes = behandling.getAksjonspunkter().stream()
            .filter(ap -> ap.erUtført() || ap.erAvbrutt())
            .filter(ap -> aksjonspunktResultatMap.get(ap.getAksjonspunktDefinisjon()) != null)
            .collect(Collectors.toSet());


        skalReåpnes.forEach((Aksjonspunkt ap) -> {
            aksjonspunktRepository.setReåpnet(ap);
            AksjonspunktResultat aksjonspunktResultat = aksjonspunktResultatMap.get(ap.getAksjonspunktDefinisjon());
            if (aksjonspunktResultat.getFrist() != null) {
                aksjonspunktRepository.setFrist(ap, aksjonspunktResultat.getFrist(), aksjonspunktResultat.getVenteårsak());
            }
            reåpnedeAksjonspunkter.add(ap);
        });

        return reåpnedeAksjonspunkter;
    }

    private List<Aksjonspunkt> leggTilNyeAksjonspunkterPåBehandling(BehandlingStegType behandlingStegType,
                                                                    List<AksjonspunktResultat> nyeDefinisjoner,
                                                                    Behandling behandling) {

        List<AksjonspunktDefinisjon> eksisterendeDefinisjoner = behandling.getAksjonspunkter().stream()
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .collect(Collectors.toList());

        List<AksjonspunktResultat> nyeAksjonspunkt = nyeDefinisjoner.stream()
            .filter(apDefWrapper -> !eksisterendeDefinisjoner.contains(apDefWrapper.getAksjonspunktDefinisjon()))
            .collect(Collectors.toList());

        return leggTilAksjonspunkt(behandlingStegType, behandling, nyeAksjonspunkt);
    }

    private List<Aksjonspunkt> leggTilAksjonspunkt(BehandlingStegType behandlingStegType, Behandling behandling,
                                                   List<AksjonspunktResultat> nyeAksjonspunkt) {

        List<Aksjonspunkt> aksjonspunkter = new ArrayList<>();
        nyeAksjonspunkt.forEach((AksjonspunktResultat apResultat) -> {

            Aksjonspunkt aksjonspunkt = aksjonspunktRepository.leggTilAksjonspunkt(behandling, apResultat.getAksjonspunktDefinisjon(),
                behandlingStegType);

            if (apResultat.getFrist() != null) {
                aksjonspunktRepository.setFrist(aksjonspunkt, apResultat.getFrist(), apResultat.getVenteårsak());
            }

            aksjonspunkter.add(aksjonspunkt);
        });
        return aksjonspunkter;
    }

}
