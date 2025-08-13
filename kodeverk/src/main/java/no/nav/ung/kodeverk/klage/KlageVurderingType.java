package no.nav.ung.kodeverk.klage;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum KlageVurderingType implements Kodeverdi {

    OPPHEVE_YTELSESVEDTAK("OPPHEVE_YTELSESVEDTAK", "Ytelsesvedtaket oppheves"),
    STADFESTE_YTELSESVEDTAK("STADFESTE_YTELSESVEDTAK", "Ytelsesvedtaket stadfestes"),
    MEDHOLD_I_KLAGE("MEDHOLD_I_KLAGE", "Medhold"),
    AVVIS_KLAGE("AVVIS_KLAGE", "Klagen avvises"),
    HJEMSENDE_UTEN_Å_OPPHEVE("HJEMSENDE_UTEN_Å_OPPHEVE", "Hjemsende uten å oppheve"),
    TRUKKET("TRUKKET_KLAGE", "Trukket"),
    FEILREGISTRERT("FEILREGISTRERT", "Feilregistrert av Kabal"),
    UDEFINERT("-", "Udefinert");

    private static final Map<String, KlageVurderingType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KLAGEVURDERING";

    @Deprecated
    public static final String DISCRIMINATOR = "KLAGEVURDERING";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    private String kode;

    private KlageVurderingType(String kode) {
        this.kode = kode;
    }

    private KlageVurderingType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static KlageVurderingType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KlageVurdering: " + kode);
        }
        return ad;
    }

    public static Map<String, KlageVurderingType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    /** Kun til invortes bruk i tester. Ingen garanti for at dette dekker alle konstanter. */
    public static Map<String, KlageVurderingType> getHardkodedeKonstanter() {
        return Set.of(values()).stream().collect(Collectors.toMap(v -> v.getKode(), v -> v));
    }
}
