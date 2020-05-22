package no.nav.k9.sak.kompletthet;

import java.time.LocalDateTime;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;

public class KompletthetResultat {

    private boolean erOppfylt;
    private LocalDateTime ventefrist;
    private Venteårsak venteårsak;
    private String venteårsakVariant;

    private KompletthetResultat(boolean erOppfylt, LocalDateTime ventefrist, Venteårsak venteårsak, String venteårsakVariant) {
        this.erOppfylt = erOppfylt;
        this.ventefrist = ventefrist;
        this.venteårsak = venteårsak;
        this.venteårsakVariant = venteårsakVariant;
    }

    public static KompletthetResultat oppfylt() {
        return new KompletthetResultat(true, null, null, null);
    }

    public static KompletthetResultat ikkeOppfylt(LocalDateTime ventefrist, Venteårsak venteårsak) {
        return ikkeOppfylt(ventefrist, venteårsak, null);
    }
    
    public static KompletthetResultat ikkeOppfylt(LocalDateTime ventefrist, Venteårsak venteårsak, String venteårsakVariant) {
        return new KompletthetResultat(false, ventefrist, venteårsak, venteårsakVariant);
    }

    public static KompletthetResultat fristUtløpt() {
        return new KompletthetResultat(false, null, null, null);
    }

    public LocalDateTime getVentefrist() {
        return ventefrist;
    }

    public Venteårsak getVenteårsak() {
        return venteårsak;
    }
    
    public String getVenteårsakVariant() {
        return venteårsakVariant;
    }

    public boolean erOppfylt() {
        return erOppfylt;
    }

    public boolean erFristUtløpt() {
        return !erOppfylt && ventefrist == null;
    }
}
