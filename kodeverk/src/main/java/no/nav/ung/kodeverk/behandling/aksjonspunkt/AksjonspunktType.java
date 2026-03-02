package no.nav.ung.kodeverk.behandling.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum AksjonspunktType implements Kodeverdi {

    AUTOPUNKT("AUTO", "Autopunkt"),
    MANUELL("MANU", "Manuell"),
    OVERSTYRING("OVST", "Overstyring"),
    SAKSBEHANDLEROVERSTYRING("SAOV", "Saksbehandleroverstyring"),

    DEL1_AUTOPUNKT("DEL1_AUTO", "Del 1 Autopunkt"),
    DEL1_MANUELL("DEL1_MANU", "Del 1 Manuell"),
    DEL1_OVERSTYRING("DEL1_OVST", "Del 1 Overstyring"),
    DEL1_SAKSBEHANDLEROVERSTYRING("DEL1_SAOV", "Del 1 Saksbehandleroverstyring"),

    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, AksjonspunktType> KODER = new LinkedHashMap<>();
    public static final String KODEVERK = "AKSJONSPUNKT_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private String offisiellKode;

    AksjonspunktType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = kode;
    }

    public static AksjonspunktType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AksjonspunktType: " + kode);
        }
        return ad;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    public static Map<String, AksjonspunktType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public boolean erAutopunkt() {
        return this == AUTOPUNKT
            || this == DEL1_AUTOPUNKT;
    }

    public boolean erOverstyringpunkt() {
        return this == OVERSTYRING
            || this == SAKSBEHANDLEROVERSTYRING
            || this == DEL1_OVERSTYRING
            || this == DEL1_SAKSBEHANDLEROVERSTYRING;
    }

}
