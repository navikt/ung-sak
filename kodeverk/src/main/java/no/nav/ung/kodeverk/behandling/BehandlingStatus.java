package no.nav.ung.kodeverk.behandling;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * NB: Pass på! Ikke legg koder vilkårlig her
 * Denne definerer etablerte behandlingstatuser ihht. modell angitt av FFA (Forretning og Fag).
 */
public enum BehandlingStatus implements Kodeverdi {

    AVSLUTTET("AVSLU", "Avsluttet"),
    FATTER_VEDTAK("FVED", "Fatter vedtak"),
    IVERKSETTER_VEDTAK("IVED", "Iverksetter vedtak"),
    OPPRETTET("OPPRE", "Opprettet"),
    UTREDES("UTRED", "Behandling utredes"),

    ;

    private static final Map<String, BehandlingStatus> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "BEHANDLING_STATUS";

    private static final Set<BehandlingStatus> FERDIGBEHANDLET_STATUS = Set.of(AVSLUTTET, IVERKSETTER_VEDTAK);

    private String navn;

    private String kode;

    private BehandlingStatus(String kode) {
        this.kode = kode;
    }

    private BehandlingStatus(String kode, String navn) {
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

    public static BehandlingStatus fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingStatus: for input " + kode);
        }
        return ad;
    }

    public static Map<String, BehandlingStatus> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static Set<BehandlingStatus> getFerdigbehandletStatuser() {
        return FERDIGBEHANDLET_STATUS;
    }

    public boolean erFerdigbehandletStatus() {
        return FERDIGBEHANDLET_STATUS.contains(this);
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
