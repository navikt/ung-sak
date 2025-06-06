package no.nav.ung.kodeverk.behandling.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum ReaktiveringStatus implements Kodeverdi {

    AKTIV("AKTIV", "Aktiv"),
    INAKTIV("INAKTIV", "Inaktiv"),
    SLETTET("SLETTET", "Inaktiv og slettet"),

    ;

    private static final Map<String, ReaktiveringStatus> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    public static final String KODEVERK = "REAKTIVERING_STATUS";

    private String navn;

    private String kode;

    private ReaktiveringStatus(String kode) {
        this.kode = kode;
    }

    private ReaktiveringStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static ReaktiveringStatus fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ReaktiveringStatus: " + kode);
        }
        return ad;
    }

    public static Map<String, ReaktiveringStatus> kodeMap() {
        return Collections.unmodifiableMap(KODER);
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
        return getKode();
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }
}
