package no.nav.k9.kodeverk.dokument;

import java.util.Collections;
import java.util.LinkedHashMap;
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
public enum DokumentKategori implements Kodeverdi {

    UDEFINERT("-", "Ikke definert", null),
    KLAGE_ELLER_ANKE("KLGA", "Klage eller anke", "KA"),
    IKKE_TOLKBART_SKJEMA("ITSKJ", "Ikke tolkbart skjema", "IS"),
    SØKNAD("SOKN", "Søknad", "SOK"),
    ELEKTRONISK_SKJEMA("ESKJ", "Elektronisk skjema", "ES"),
    BRV("BRV", "Brev", "B"),
    EDIALOG("EDIALOG", "Elektronisk dialog", "ELEKTRONISK_DIALOG"),
    FNOT("FNOT", "Forvaltningsnotat", "FORVALTNINGSNOTAT"),
    IBRV("IBRV", "Informasjonsbrev", "IB"),
    KONVEARK("KONVEARK", "Konvertert fra elektronisk arkiv", "KD"),
    KONVSYS("KONVSYS", "Konverterte data fra system", "KS"),
    PUBEOS("PUBEOS", "Publikumsblankett EØS", "PUBL_BLANKETT_EOS"),
    SEDOK("SEDOK", "Strukturert elektronisk dokument - EU/EØS", "SED"),
    TSKJ("TSKJ", "Tolkbart skjema", "TS"),
    VBRV("VBRV", "Vedtaksbrev", "VB"),
    INNTEKTKOMP_FRILANS("INNTEKTKOMP_FRILANS", "Frilansere og Selvstendig næringdrivendes Inntektskompensasjon", "NAV 00-03.02"),
    SØKNAD_DAGP_PERM("SOKNAD_DAGP_PERM", "Søknad om dagpenger ved permittering", "NAV 04-01.04"),
    AVSLAG("AVSLAG", "Frisinn avslutning", "AVSLAG"),
    INNVILGELSE("INNVILGELSE", "Frisinn innvilgelse", "INNVILGELSE"),
    ;

    public static final String KODEVERK = "DOKUMENT_KATEGORI";

    private static final Map<String, DokumentKategori> KODER = new LinkedHashMap<>();

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private String offisiellKode;

    private String kode;

    private DokumentKategori(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    @JsonCreator
    public static DokumentKategori fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent DokumentKategori: " + kode);
        }
        return ad;
    }

    public static Map<String, DokumentKategori> kodeMap() {
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
        return offisiellKode;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    public static DokumentKategori finnForKodeverkEiersKode(String offisiellDokumentType) {
        return KODER.values().stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }

}
