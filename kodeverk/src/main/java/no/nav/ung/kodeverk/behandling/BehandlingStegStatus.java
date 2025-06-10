package no.nav.ung.kodeverk.behandling;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.*;

/**
 * Kodefor status i intern håndtering av flyt på et steg
 * <p>
 * Kommer kun til anvendelse dersom det oppstår aksjonspunkter eller noe må legges på vent i et steg. Hvis ikke
 * flyter et rett igjennom til UTFØRT.
 */
public enum BehandlingStegStatus implements Kodeverdi {

    /** midlertidig intern tilstand når steget startes (etter inngang). */
    STARTET("STARTET", "Steget er startet"),
    INNGANG("INNGANG", "Inngangkriterier er ikke oppfylt"),
    UTGANG("UTGANG", "Utgangskriterier er ikke oppfylt"),
    VENTER("VENTER", "På vent"),
    AVBRUTT("AVBRUTT", "Avbrutt"),
    UTFØRT("UTFØRT", "Utført"),
    FREMOVERFØRT("FREMOVERFØRT", "Fremoverført"),
    TILBAKEFØRT("TILBAKEFØRT", "Tilbakeført"),
    UDEFINERT("-", "Ikke definert"),

    ;
    private static final Set<BehandlingStegStatus> KAN_UTFØRE_STEG = new HashSet<>(Arrays.asList(STARTET, VENTER));
    private static final Set<BehandlingStegStatus> KAN_FORTSETTE_NESTE = new HashSet<>(Arrays.asList(UTFØRT, FREMOVERFØRT));

    // hvis oppdaterer SLUTT_STATUSER, husk Behandling.behandlingStegTilstand har en @Where annotation som matcher
    private static final Set<BehandlingStegStatus> SLUTT_STATUSER = new HashSet<>(Arrays.asList(AVBRUTT, UTFØRT, TILBAKEFØRT));

    private static final Map<String, BehandlingStegStatus> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "BEHANDLING_STEG_STATUS"; //$NON-NLS-1$

    private String navn;

    private String kode;

    private BehandlingStegStatus(String kode) {
        this.kode = kode;
    }

    private BehandlingStegStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    /**
     * toString is set to output the kode value of the enum instead of the default that is the enum name.
     * This makes the generated openapi spec correct when the enum is used as a query param. Without this the generated
     * spec incorrectly specifies that it is the enum name string that should be used as input.
     */
    @Override
    public String toString() {
        return this.getKode();
    }

    public static BehandlingStegStatus fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingStegStatus: for input " + kode);
        }
        return ad;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    public boolean kanUtføreSteg() {
        return KAN_UTFØRE_STEG.contains(this);
    }

    public boolean kanFortsetteTilNeste() {
        return KAN_FORTSETTE_NESTE.contains(this);
    }

    public static boolean erSluttStatus(BehandlingStegStatus status) {
        return SLUTT_STATUSER.contains(status);
    }

    public boolean erVedInngang() {
       return Objects.equals(INNGANG, this);
    }

    public static boolean erVedUtgang(BehandlingStegStatus stegStatus) {
        return Objects.equals(UTGANG, stegStatus);
    }

    public static Map<String, BehandlingStegStatus> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

}
