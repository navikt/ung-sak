package no.nav.k9.kodeverk.dokument;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.api.Kodeverdi;

/**
 * Brevkode er et kodeverk som forvaltes av Kodeverkforvaltning.
 */
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum Brevkode implements Kodeverdi {

    // Match mot Deprecated  {@link no.nav.k9.kodeverk.dokument.DokumentTypeId}
    INNTEKTSMELDING("INNTEKTSMELDING", "4936"),
    LEGEERKLÆRING("LEGEERKLÆRING", "I000023"),  // Brevkode ikke avklart

    // Match mot Deprecated  {@link no.nav.k9.kodeverk.dokument.Dokumentkategori}
    KLAGE_ELLER_ANKE("KLGA", "KA"), // Brevkode ikke avklart
    IKKE_TOLKBART_SKJEMA("ITSKJ", "IS"), // Brevkode ikke avklart
    SØKNAD("SOKN", "SOK"), // Brevkode ikke avklart
    ELEKTRONISK_SKJEMA("ESKJ", "ES"), // Brevkode ikke avklart
    BRV("BRV", "B"), // Brevkode ikke avklart
    EDIALOG("EDIALOG", "ELEKTRONISK_DIALOG"), // Brevkode ikke avklart
    FNOT("FNOT", "FORVALTNINGSNOTAT"), // Brevkode ikke avklart
    IBRV("IBRV", "IB"), // Brevkode ikke avklart
    KONVEARK("KONVEARK", "KD"), // Brevkode ikke avklart
    KONVSYS("KONVSYS", "KS"), // Brevkode ikke avklart
    PUBEOS("PUBEOS", "PUBL_BLANKETT_EOS"), // Brevkode ikke avklart
    SEDOK("SEDOK", "SED"), // Brevkode ikke avklart
    TSKJ("TSKJ", "TS"), // Brevkode ikke avklart
    VBRV("VBRV", "VB"), // Brevkode ikke avklart
    INNTEKTKOMP_FRILANS("INNTEKTKOMP_FRILANS", "NAV 00-03.02"),
    SØKNAD_DAGP_PERM("SOKNAD_DAGP_PERM", "NAV 04-01.04"),
    AVSLAG("AVSLAG", "AVSLAG"),
    INNVILGELSE("INNVILGELSE", "INNVILGELSE"),

    // Default
    UDEFINERT("-", null),
    ;

    public static final String KODEVERK = "DOKUMENT_TYPE_ID";
    private static final Map<String, Brevkode> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            if (KODER.putIfAbsent(v.offisiellKode, v) != null && !Objects.equals(v.offisiellKode, v.kode)) {
                throw new IllegalArgumentException("Duplikat : " + v.offisiellKode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private String offisiellKode;

    private String kode;

    private Brevkode(String kode, String offisiellKode) {
        this.kode = kode;
        this.offisiellKode = offisiellKode;

    }

    @JsonCreator
    public static Brevkode fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);

        if (ad == null) {
            // midlertidig fallback til vi endrer til offisille kodeverdier
            ad = finnForKodeverkEiersKode(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent DokumentTypeId: " + kode);
            }
        }
        return ad;
    }

    public static Brevkode finnForKodeverkEiersKode(String offisiellDokumentType) {
        if (offisiellDokumentType == null || offisiellDokumentType.isBlank())
            return Brevkode.UDEFINERT;

        Optional<Brevkode> dokId = KODER.values().stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst();
        if (dokId.isPresent()) {
            return dokId.get();
        } else {
            throw new IllegalArgumentException("Ukjent offisiellDokumentType: " + offisiellDokumentType);
        }
    }

    public static Map<String, Brevkode> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return getKode();
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

}
