package no.nav.k9.kodeverk.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum HistorikkOpplysningType implements Kodeverdi {

    UDEFINIERT("-", "Ikke definert"),
    ANTALL_BARN("ANTALL_BARN", "Antall barn"),
    TPS_ANTALL_BARN("TPS_ANTALL_BARN", "Antall barn"),
    FODSELSDATO("FODSELSDATO", "Fødselsdato"),
    UTTAK_PERIODE_FOM("UTTAK_PERIODE_FOM", "Fradato uttaksperiode"),
    UTTAK_PERIODE_TOM("UTTAK_PERIODE_TOM", "Tildato uttaksperiode"),
    ;

    private static final Map<String, HistorikkOpplysningType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_OPPLYSNING_TYPE";

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

    private HistorikkOpplysningType(String kode) {
        this.kode = kode;
    }

    private HistorikkOpplysningType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static HistorikkOpplysningType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkOpplysningType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkOpplysningType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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
}
