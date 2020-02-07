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
public enum HistorikkAvklartSoeknadsperiodeType implements Kodeverdi {

    GRADERING("GRADERING", "Uttak: gradering"),
    UTSETTELSE_ARBEID("UTSETTELSE_ARBEID", "Utsettelse: arbeid"),
    UTSETTELSE_FERIE("UTSETTELSE_FERIE", "Utsettelse: ferie"),
    UTSETTELSE_SKYDOM("UTSETTELSE_SKYDOM", "Utsettelse: sykdom/skade"),
    UTSETTELSE_INSTITUSJON_SØKER("UTSETTELSE_INSTITUSJON_SØKER", "Utsettelse: innleggelse av forelder"),
    UTSETTELSE_INSTITUSJON_BARN("UTSETTELSE_INSTITUSJON_BARN", "Utsettelse: innleggelse av barn"),
    NY_SOEKNADSPERIODE("NY_SOEKNADSPERIODE", "Ny periode er lagt til"),
    SLETTET_SOEKNASPERIODE("SLETTET_SOEKNASPERIODE", "Perioden er slettet"),
    OVERFOERING_ALENEOMSORG("OVERFOERING_ALENEOMSORG", "Overføring: søker har aleneomsorg"),
    OVERFOERING_SKYDOM("OVERFOERING_SKYDOM", "Overføring: sykdom/skade"),
    OVERFOERING_INNLEGGELSE("OVERFOERING_INNLEGGELSE", "Overføring: innleggelse"),
    OVERFOERING_IKKE_RETT("OVERFOERING_IKKE_RETT", "Overføring: annen forelder har ikke rett"),
    UTTAK("UTTAK", "Uttak"),
    OPPHOLD("OPPHOLD", "Opphold: annen foreldres uttak"),

    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, HistorikkAvklartSoeknadsperiodeType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_AVKLART_SOEKNADSPERIODE_TYPE";

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

    private HistorikkAvklartSoeknadsperiodeType(String kode) {
        this.kode = kode;
    }

    private HistorikkAvklartSoeknadsperiodeType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static HistorikkAvklartSoeknadsperiodeType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkAvklartSoeknadsperiodeType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkAvklartSoeknadsperiodeType> kodeMap() {
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
