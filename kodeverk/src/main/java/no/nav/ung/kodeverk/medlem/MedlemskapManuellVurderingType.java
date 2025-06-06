package no.nav.ung.kodeverk.medlem;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum MedlemskapManuellVurderingType implements Kodeverdi {

    UDEFINERT("-", "Ikke definert", false),
    MEDLEM("MEDLEM", "Periode med medlemskap", true),
    UNNTAK("UNNTAK", "Periode med unntak fra medlemskap", true),
    IKKE_RELEVANT("IKKE_RELEVANT", "Ikke relevant periode", true),
    SAKSBEHANDLER_SETTER_OPPHØR_AV_MEDL_PGA_ENDRINGER_I_TPS("OPPHOR_PGA_ENDRING_I_TPS", "Opphør av medlemskap på grunn av endringer i tps", false),

    ;

    private static final Map<String, MedlemskapManuellVurderingType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "MEDLEMSKAP_MANUELL_VURD";

    private String navn;

    private boolean visForGui;

    private String kode;

    private MedlemskapManuellVurderingType(String kode) {
        this.kode = kode;
    }

    private MedlemskapManuellVurderingType(String kode, String navn, boolean visGui) {
        this.kode = kode;
        this.navn = navn;
        this.visForGui = visGui;
    }

    public static MedlemskapManuellVurderingType  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent MedlemskapManuellVurderingType: " + kode);
        }
        return ad;
    }

    public static Map<String, MedlemskapManuellVurderingType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public boolean visesPåKlient() {
        return visForGui;
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
