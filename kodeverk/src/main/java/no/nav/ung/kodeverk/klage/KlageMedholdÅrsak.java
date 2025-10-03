package no.nav.ung.kodeverk.klage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum KlageMedholdÅrsak implements Kodeverdi {

    NYE_OPPLYSNINGER("NYE_OPPLYSNINGER", "Nytt faktum"),
    ULIK_REGELVERKSTOLKNING("ULIK_REGELVERKSTOLKNING", "Feil lovanvendelse"),
    ULIK_VURDERING("ULIK_VURDERING", "Ulik skjønnsvurdering"),
    PROSESSUELL_FEIL("PROSESSUELL_FEIL", "Saksbehandlingsfeil"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, KlageMedholdÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KLAGE_MEDHOLD_AARSAK";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    @JsonValue
    private String kode;

    private KlageMedholdÅrsak(String kode) {
        this.kode = kode;
    }

    private KlageMedholdÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static KlageMedholdÅrsak fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KlageMedholdÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, KlageMedholdÅrsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

}
