package no.nav.k9.sak.behandling.aksjonspunkt;

import java.util.ArrayList;
import java.util.List;

import no.nav.k9.felles.util.Tuple;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;

public class OppdateringResultat {

    private BehandlingStegType nesteSteg;
    private boolean skalRekjøreSteg = false;
    private boolean totrinnsKontroll = false;
    private final List<Tuple<AksjonspunktDefinisjon, AksjonspunktStatus>> ekstraAksjonspunktResultat = new ArrayList<>();


    private OppdateringResultat() {
    }

    /**
     * Klassisk resultat - uten spesiell håndtering annet enn å sette Aksjonspunkt til UTFO
     */
    public static OppdateringResultat nyttResultat() {
        return new OppdateringResultat();
    }

    /**
     * Brukes i tilfelle med behov for tilstandsavhengig håndtering av resultat
     */
    public static Builder builder() {
        return new Builder();
    }


    public BehandlingStegType getNesteSteg() {
        return nesteSteg;
    }


    public boolean kreverTotrinnsKontroll() {
        return totrinnsKontroll;
    }

    public List<Tuple<AksjonspunktDefinisjon, AksjonspunktStatus>> getEkstraAksjonspunktResultat() {
        return ekstraAksjonspunktResultat;
    }

    public boolean getSkalRekjøreSteg() {
        return skalRekjøreSteg;
    }

    public void rekjørSteg() {
        this.skalRekjøreSteg = true;
    }

    public void setSteg(BehandlingStegType håndtertISteg) {
        this.nesteSteg = håndtertISteg;
    }

    public static class Builder {
        private final OppdateringResultat resultat;

        Builder() {
            resultat = new OppdateringResultat();
        }

        /*
         * Sentral håndtering av totrinn.
         */
        public Builder medTotrinn() {
            resultat.totrinnsKontroll = true;
            return this;
        }

        /*
         * Sentral håndtering av totrinn.
         */
        public Builder medTotrinnHvis(boolean erTotrinn) {
            resultat.totrinnsKontroll = erTotrinn;
            return this;
        }

        /*
         * Brukes dersom man absolutt må endre status på andre aksjonspunkt enn det aktuelle for oppdatering/overstyring.
         * NB: Vil legge til dersom ikke finnes fra før. Bruk helst andre mekanismer.
         */
        public Builder medEkstraAksjonspunktResultat(AksjonspunktDefinisjon aksjonspunktDefinisjon, AksjonspunktStatus nyStatus) {
            resultat.ekstraAksjonspunktResultat.add(new Tuple<>(aksjonspunktDefinisjon, nyStatus));
            return this;
        }

        public OppdateringResultat build() {
            return resultat;
        }
    }

    @Override
    public String toString() {
        return "OppdateringResultat{" +
            "nesteSteg=" + nesteSteg +
            '}';
    }
}
