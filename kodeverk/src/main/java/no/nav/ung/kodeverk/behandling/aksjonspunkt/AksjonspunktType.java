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

    NAV_KONTOR_AUTOPUNKT("NAV_KONTOR_AUTO", "Nav-kontor Autopunkt"),
    NAV_KONTOR_MANUELL("NAV_KONTOR_MANU", "Nav-kontor Manuell"),
    NAV_KONTOR_OVERSTYRING("NAV_KONTOR_OVST", "Nav-kontor Overstyring"),
    NAV_KONTOR_SAKSBEHANDLEROVERSTYRING("NAV_KONTOR_SAOV", "Nav-kontor Saksbehandleroverstyring"),

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
            || this == NAV_KONTOR_AUTOPUNKT;
    }

    public boolean erOverstyringpunkt() {
        return this == OVERSTYRING
            || this == SAKSBEHANDLEROVERSTYRING
            || this == NAV_KONTOR_OVERSTYRING
            || this == NAV_KONTOR_SAKSBEHANDLEROVERSTYRING;
    }

    public boolean erNavKontorAksjonspunkt() {
        return this == NAV_KONTOR_MANUELL
            || this == NAV_KONTOR_OVERSTYRING
            || this == NAV_KONTOR_SAKSBEHANDLEROVERSTYRING
            || this == NAV_KONTOR_AUTOPUNKT;

    }

    public boolean erNavSentraltAksjonspunkt() {
        return !erNavKontorAksjonspunkt();

    }
}
