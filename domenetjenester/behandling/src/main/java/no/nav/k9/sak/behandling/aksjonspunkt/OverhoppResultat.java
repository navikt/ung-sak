package no.nav.k9.sak.behandling.aksjonspunkt;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.sak.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.vedtak.util.Tuple;

public class OverhoppResultat {
    Set<OppdateringResultat> oppdatereResultater = new LinkedHashSet<>();

    public static OverhoppResultat tomtResultat() {
        return new OverhoppResultat();
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

    public Optional<TransisjonIdentifikator> finnFremoverTransisjon() {
        return oppdatereResultater.stream()
            .filter(delresultat -> delresultat.getOverhoppKontroll().equals(OverhoppKontroll.FREMOVERHOPP))
            .map(OppdateringResultat::getTransisjon)
            .findFirst(); // TODO (essv): Sorter steg ut fra deres rekkefølge
    }

    public Optional<OppdateringResultat> finnHenleggelse() {
        return oppdatereResultater.stream()
            .filter(delresultat -> delresultat.getOverhoppKontroll().equals(OverhoppKontroll.HENLEGGELSE))
            .findFirst();
    }

    public Set<Tuple<AksjonspunktDefinisjon, AksjonspunktStatus>> finnEkstraAksjonspunktResultat() {
        Set<Tuple<AksjonspunktDefinisjon, AksjonspunktStatus>> resultater = new HashSet<>();
        oppdatereResultater.stream().forEach(res -> resultater.addAll(res.getEkstraAksjonspunktResultat()));
        return resultater;
    }

    @Override
    public String toString() {
        return "OverhoppResultat{" +
            "oppdatereResultater=" + oppdatereResultater +
            '}';
    }
}
