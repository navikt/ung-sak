package no.nav.ung.sak.behandlingskontroll;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.ung.sak.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;

public class BehandleStegResultat {
    private final List<AksjonspunktResultat> aksjonspunktListe;
    private final BehandlingStegType stegType;
    private final TransisjonIdentifikator transisjon;

    private BehandleStegResultat(TransisjonIdentifikator transisjon, List<AksjonspunktResultat> aksjonspunktListe) {
        this.aksjonspunktListe = aksjonspunktListe;
        this.stegType = null;
        this.transisjon = transisjon;
    }

    private BehandleStegResultat(TransisjonIdentifikator transisjon, BehandlingStegType tilSteg) {
        this.aksjonspunktListe = List.of();
        this.stegType = tilSteg;
        this.transisjon = transisjon;
    }

    public static BehandleStegResultat utførtMedAksjonspunktResultater(AksjonspunktResultat aksjonspunktResultater) {
        Objects.requireNonNull(aksjonspunktResultater);
        return new BehandleStegResultat(FellesTransisjoner.UTFØRT, List.of(aksjonspunktResultater));
    }

    /**
     * Factory-metode basert på liste av {@link AksjonspunktResultat}, støtter callback for å modifisere
     * {@link Aksjonspunkt}
     */
    public static BehandleStegResultat utførtMedAksjonspunktResultater(List<AksjonspunktResultat> aksjonspunktResultater) {
        return new BehandleStegResultat(FellesTransisjoner.UTFØRT, aksjonspunktResultater);
    }

    /**
     * Setter at behandlingen ligger på angitt steg uten å kjøre videre.
     * Den som kaller på denne metoden må selv leggge inn en FortsettBehandlingTask.
     */
    public static BehandleStegResultat tilbakeførtTilStegUtenVidereKjøring(BehandlingStegType tilSteg) {
        return new BehandleStegResultat(FellesTransisjoner.TILBAKEFØRT_TIL_STEG, Objects.requireNonNull(tilSteg));
    }

    /**
     * Factory-metode for liste av {@link AksjonspunktDefinisjon}. Ingen callback for consumer.
     */
    public static BehandleStegResultat utførtMedAksjonspunkter(List<AksjonspunktDefinisjon> aksjonspunktListe) {
        List<AksjonspunktResultat> aksjonspunktResultater = konverterTilAksjonspunktResultat(aksjonspunktListe);
        return new BehandleStegResultat(FellesTransisjoner.UTFØRT, aksjonspunktResultater);
    }

    public static BehandleStegResultat utførtUtenAksjonspunkter() {
        return new BehandleStegResultat(FellesTransisjoner.UTFØRT, Collections.emptyList());
    }

    public static BehandleStegResultat settPåVent() {
        return new BehandleStegResultat(FellesTransisjoner.SETT_PÅ_VENT, Collections.emptyList());
    }

    public static BehandleStegResultat tilbakeførtMedAksjonspunkter(List<AksjonspunktDefinisjon> aksjonspunktListe) {
        List<AksjonspunktResultat> aksjonspunktResultater = konverterTilAksjonspunktResultat(aksjonspunktListe);
        return new BehandleStegResultat(FellesTransisjoner.TILBAKEFØRT_TIL_AKSJONSPUNKT, aksjonspunktResultater);
    }

    public static BehandleStegResultat fremoverførtMedAksjonspunkter(TransisjonIdentifikator transisjon,
                                                                     List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        List<AksjonspunktResultat> aksjonspunktResultater = konverterTilAksjonspunktResultat(aksjonspunktDefinisjoner);
        return new BehandleStegResultat(transisjon, aksjonspunktResultater);
    }

    public static BehandleStegResultat fremoverførtMedAksjonspunktResultater(TransisjonIdentifikator transisjon,
                                                                             List<AksjonspunktResultat> aksjonspunktResultater) {
        return new BehandleStegResultat(transisjon, aksjonspunktResultater);
    }

    public static BehandleStegResultat fremoverført(TransisjonIdentifikator transisjon) {
        return new BehandleStegResultat(transisjon, Collections.emptyList());
    }

    private static List<AksjonspunktResultat> konverterTilAksjonspunktResultat(List<AksjonspunktDefinisjon> aksjonspunktListe) {
        return aksjonspunktListe.stream()
            .map(apDef -> AksjonspunktResultat.opprettForAksjonspunkt(apDef))
            .collect(toList());
    }

    public static BehandleStegResultat startet() {
        return new BehandleStegResultat(FellesTransisjoner.STARTET, Collections.emptyList());
    }

    // Må selv lage historikkinnslag i steget
    public static BehandleStegResultat henlagtBehandling() {
        return new BehandleStegResultat(FellesTransisjoner.HENLAGT, Collections.emptyList());
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktListe() {
        return aksjonspunktListe.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(toList());
    }

    public List<AksjonspunktResultat> getAksjonspunktResultater() {
        return aksjonspunktListe;
    }

    public TransisjonIdentifikator getTransisjon() {
        return transisjon;
    }

    /**
     * sett nytt aksjonspunkt spesifikt. returner kopi av denne instansen.
     */
    public BehandleStegResultat medAksjonspunktResultat(AksjonspunktResultat aksResultat) {
        List<AksjonspunktResultat> liste = new ArrayList<>(this.aksjonspunktListe);
        liste.remove(aksResultat);
        liste.add(aksResultat);

        return new BehandleStegResultat(this.transisjon, liste);
    }

    public BehandlingStegType getStegType() {
        return stegType;
    }
}
