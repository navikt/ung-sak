package no.nav.ung.kodeverk.vedtak;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum IverksettingStatus implements Kodeverdi {

    IKKE_IVERKSATT("IKKE_IVERKSATT", "Ikke iverksatt"),
    UNDER_IVERKSETTING("UNDER_IVERKSETTING", "Under iverksetting"),
    IVERKSATT("IVERKSATT", "Iverksatt"),

    UDEFINERT("-", "Ikke definert"),

    ;

    public static final String KODEVERK = "IVERKSETTING_STATUS"; //$NON-NLS-1$
    private static final Map<String, IverksettingStatus> KODER = new LinkedHashMap<>();

    private String navn;

    private String kode;

    private IverksettingStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static IverksettingStatus fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent IverksettingStatus: for input " + kode);
        }
        return ad;
    }

    public static Map<String, IverksettingStatus> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
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
