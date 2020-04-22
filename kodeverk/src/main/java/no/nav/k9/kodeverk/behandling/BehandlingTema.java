package no.nav.k9.kodeverk.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
public enum BehandlingTema implements Kodeverdi {
    PLEIEPENGER_SYKT_BARN("PLEIE", "Pleiepenger sykt barn", "ab0320", FagsakYtelseType.PLEIEPENGER_SYKT_BARN),  // ny ordning fom 011017
    OMSORGSPENGER("OMS_OMSORG", "Omsorgspenger", "ab0149", FagsakYtelseType.OMSORGSPENGER),
    UDEFINERT("-", "Ikke definert", null, FagsakYtelseType.UDEFINERT),

    ;

    public static final String KODEVERK = "BEHANDLING_TEMA";
    private static final Map<String, BehandlingTema> KODER = new LinkedHashMap<>();
    private static final Map<FagsakYtelseType, BehandlingTema> YTELSE_TYPE_TIL_TEMA = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat kode: " + v.kode);
            }
            if (YTELSE_TYPE_TIL_TEMA.putIfAbsent(v.fagsakYtelseType, v) != null) {
                throw new IllegalArgumentException("Duplikat ytelseType -> tema: " + v);
            }
        }
    }

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private String offisiellKode;

    private String kode;

    private FagsakYtelseType fagsakYtelseType;

    private BehandlingTema(String kode) {
        this.kode = kode;
    }

    private BehandlingTema(String kode, String navn, String offisiellKode, FagsakYtelseType fagsakYtelseType) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
        this.fagsakYtelseType = fagsakYtelseType;
    }


    @JsonCreator
    public static BehandlingTema fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingTema: " + kode);
        }
        return ad;
    }

    public static Map<String, BehandlingTema> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static BehandlingTema finnForKodeverkEiersKode(String offisiellDokumentType) {
        return List.of(values()).stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }

    public static BehandlingTema finnForFagsakYtelseType(FagsakYtelseType ytelseType) {
        return YTELSE_TYPE_TIL_TEMA.getOrDefault(ytelseType, BehandlingTema.UDEFINERT);
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
        return offisiellKode;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }
}
