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
public enum HistorikkResultatType implements Kodeverdi {

    UDEFINIERT("-", "Ikke definert"),
    AVVIS_KLAGE("AVVIS_KLAGE", "Klagen er avvist"),
    MEDHOLD_I_KLAGE("MEDHOLD_I_KLAGE", "Vedtaket er omgjort, til gunst"),
    OPPHEVE_VEDTAK("OPPHEVE_VEDTAK", "Vedtaket er opphevet"),
    OPPRETTHOLDT_VEDTAK("OPPRETTHOLDT_VEDTAK", "Vedtaket er opprettholdt"),
    STADFESTET_VEDTAK("STADFESTET_VEDTAK", "Vedtaket er stadfestet"),
    BEREGNET_AARSINNTEKT("BEREGNET_AARSINNTEKT", "Grunnlag for beregnet årsinntekt"),
    UTFALL_UENDRET("UTFALL_UENDRET", "Overstyrt vurdering: Utfall er uendret"),
    DELVIS_MEDHOLD_I_KLAGE("DELVIS_MEDHOLD_I_KLAGE", "Vedtaket er delvis omgjort"),
    KLAGE_HJEMSENDE_UTEN_OPPHEVE("KLAGE_HJEMSENDE_UTEN_OPPHEVE", "Behandling er hjemsendt"),
    UGUNST_MEDHOLD_I_KLAGE("UGUNST_MEDHOLD_I_KLAGE", "Vedtaket er omgjort til ugunst"),
    OVERSTYRING_FAKTA_UTTAK("OVERSTYRING_FAKTA_UTTAK", "Overstyrt vurdering:"),
    ANKE_AVVIS("ANKE_AVVIS", "Anken er avvist"),
    ANKE_OMGJOER("ANKE_OMGJOER", "Omgjør i anke"),
    ANKE_OPPHEVE_OG_HJEMSENDE("ANKE_OPPHEVE_OG_HJEMSENDE", "Ytelsesvedtaket oppheve og hjemsende"),
    ANKE_STADFESTET_VEDTAK("ANKE_STADFESTET_VEDTAK", "Vedtaket er stadfestet"),
    ANKE_DELVIS_OMGJOERING_TIL_GUNST("ANKE_DELVIS_OMGJOERING_TIL_GUNST", "Delvis omgjøring, til gunst i anke"),
    ANKE_TIL_UGUNST("ANKE_TIL_UGUNST", "Ugunst omgjør i anke"),
    ANKE_TIL_GUNST("ANKE_TIL_GUNST", "Til gunst omgjør i anke"),
    ;

    private static final Map<String, HistorikkResultatType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_RESULTAT_TYPE";

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

    private HistorikkResultatType(String kode) {
        this.kode = kode;
    }

    private HistorikkResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static HistorikkResultatType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkResultatType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkResultatType> kodeMap() {
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

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }
}
