package no.nav.ung.sak.behandlingskontroll.impl;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;

/**
 * Håndterer aksjonspunktresultat og oppretter/reaktiverer aksjonspunkt
 * Brukes fra StegVisitor og Endringskontroller for lik håndtering
 */
class AksjonspunktResultatOppretter {

    private final Behandling behandling;

    private final AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    private Map<AksjonspunktDefinisjon, Aksjonspunkt> eksisterende = new LinkedHashMap<>();

    AksjonspunktResultatOppretter(AksjonspunktKontrollRepository aksjonspunktKontrollRepository, Behandling behandling) {
        this.behandling = Objects.requireNonNull(behandling, "behandling");
        this.aksjonspunktKontrollRepository = aksjonspunktKontrollRepository;
        behandling.getAksjonspunkter().forEach(ap -> this.eksisterende.putIfAbsent(ap.getAksjonspunktDefinisjon(), ap));
    }

    /**
     * Lagrer nye aksjonspunkt, og gjenåpner dem hvis de alleerede står til avbrutt/utført
     */
    List<Aksjonspunkt> opprettAksjonspunkter(List<AksjonspunktResultat> apResultater, BehandlingStegType behandlingStegType) {

        if (!apResultater.isEmpty()) {
            List<Aksjonspunkt> endringAksjonspunkter = new ArrayList<>();
            endringAksjonspunkter.addAll(fjernGjensidigEkskluderendeAksjonspunkter(apResultater));
            endringAksjonspunkter.addAll(leggTilResultatPåBehandling(behandlingStegType, apResultater));
            return endringAksjonspunkter;
        } else {
            return new ArrayList<>();
        }
    }

    private List<Aksjonspunkt> fjernGjensidigEkskluderendeAksjonspunkter(List<AksjonspunktResultat> nyeApResultater) {
        List<Aksjonspunkt> avbrutteAksjonspunkter = new ArrayList<>();
        Set<String> nyeApDef = nyeApResultater.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).map(AksjonspunktDefinisjon::getKode).collect(toSet());
        eksisterende.values().stream()
            .filter(Aksjonspunkt::erÅpentAksjonspunkt)
            .filter(ap -> ap.getAksjonspunktDefinisjon().getUtelukkendeApdef().stream().anyMatch(nyeApDef::contains))
            .forEach(ap -> {
                aksjonspunktKontrollRepository.setTilAvbrutt(ap);
                avbrutteAksjonspunkter.add(ap);
            });
        return avbrutteAksjonspunkter;
    }

    private List<Aksjonspunkt> leggTilResultatPåBehandling(BehandlingStegType behandlingStegType, List<AksjonspunktResultat> resultat) {
        return resultat.stream()
            .map(ar -> oppdaterAksjonspunktMedResultat(behandlingStegType, ar))
            .collect(Collectors.toList());
    }

    private Aksjonspunkt oppdaterAksjonspunktMedResultat(BehandlingStegType behandlingStegType, AksjonspunktResultat resultat) {
        Aksjonspunkt oppdatert = eksisterende.get(resultat.getAksjonspunktDefinisjon());
        if (oppdatert == null) {
            oppdatert = aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, resultat.getAksjonspunktDefinisjon(), behandlingStegType);
            eksisterende.putIfAbsent(oppdatert.getAksjonspunktDefinisjon(), oppdatert);
        }
        if (oppdatert.erUtført() || oppdatert.erAvbrutt()) {
            aksjonspunktKontrollRepository.setReåpnet(oppdatert);
        }
        if (resultat.getFrist() != null) {
            aksjonspunktKontrollRepository.setFrist(oppdatert, resultat.getFrist(), resultat.getVenteårsak(), resultat.getVenteårsakVariant());
        }
        return oppdatert;
    }

}
