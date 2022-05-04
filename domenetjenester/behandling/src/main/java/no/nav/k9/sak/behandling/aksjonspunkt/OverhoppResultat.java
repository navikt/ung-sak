package no.nav.k9.sak.behandling.aksjonspunkt;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.felles.util.Tuple;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.sak.behandlingskontroll.impl.transisjoner.Transisjoner;
import no.nav.k9.sak.behandlingskontroll.transisjoner.TransisjonIdentifikator;

public class OverhoppResultat {
    Set<OppdateringResultat> oppdatereResultater = new LinkedHashSet<>();

    public static OverhoppResultat tomtResultat() {
        return new OverhoppResultat();
    }

    public Set<OppdateringResultat> getOppdatereResultater() {
        return oppdatereResultater;
    }

    public void leggTil(OppdateringResultat delresultat) {
        oppdatereResultater.add(delresultat);
    }

    public boolean skalRekjøreSteg() {
        return oppdatereResultater.stream().anyMatch(OppdateringResultat::getSkalRekjøreSteg);
    }

    public BehandlingStegType stegSomSkalRekjøres() {
        var stegSomSkalRekjøres = oppdatereResultater.stream().filter(OppdateringResultat::getSkalRekjøreSteg).map(OppdateringResultat::getNesteSteg).collect(Collectors.toSet());
        if (stegSomSkalRekjøres.isEmpty()) {
            throw new IllegalStateException("Forventet å finne minst et steg vi skal hoppe til ");
        } else if (stegSomSkalRekjøres.size() == 1) {
            return stegSomSkalRekjøres.iterator().next();
        }
        throw new IllegalStateException("Støtter bare et steg ved løsning av aksjonspunkt ..");
    }

    public boolean skalOppdatereGrunnlag() {
        return oppdatereResultater.stream().anyMatch(delresultat -> delresultat.getOverhoppKontroll().equals(OverhoppKontroll.OPPDATER));
    }

    public boolean finnTotrinn() {
        return oppdatereResultater.stream().anyMatch(OppdateringResultat::kreverTotrinnsKontroll);
    }

    public Optional<TransisjonIdentifikator> finnFremoverTransisjon(Comparator<BehandlingStegType> stegSammenligner) {
        return oppdatereResultater.stream()
            .filter(delresultat -> delresultat.getOverhoppKontroll().equals(OverhoppKontroll.FREMOVERHOPP))
            .map(OppdateringResultat::getTransisjon)
            .max(Comparator.comparing(t -> Transisjoner.finnTransisjon(t).getMålstegHvisFremoverhopp().orElseThrow(), stegSammenligner));
    }

    public Set<Tuple<AksjonspunktDefinisjon, AksjonspunktStatus>> finnEkstraAksjonspunktResultat() {
        return oppdatereResultater.stream().flatMap(res -> res.getEkstraAksjonspunktResultat().stream()).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "OverhoppResultat{" +
            "oppdatereResultater=" + oppdatereResultater +
            '}';
    }
}
