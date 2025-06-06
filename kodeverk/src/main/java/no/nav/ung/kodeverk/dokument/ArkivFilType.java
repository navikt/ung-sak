package no.nav.ung.kodeverk.dokument;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.*;

public enum ArkivFilType implements Kodeverdi {

    PDF("PDF"),
    PDFA("PDFA"),
    XML("XML"),
    AFP("AFP"),
    AXML("AXML"),
    DLF("DLF"),
    DOC("DOC"),
    DOCX("DOCX"),
    JPEG("JPEG"),
    RTF("RTF"),
    TIFF("TIFF"),
    XLS("XLS"),
    XLSX("XLSX"),

    UDEFINERT("-"),

    ;

    private static final String KODEVERK = "ARKIV_FILTYPE";

    private static final Map<String, ArkivFilType> KODER = new LinkedHashMap<>();

    private String kode;

    private ArkivFilType(String kode) {
        this.kode = kode;
    }

    public static ArkivFilType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ArkivFilType: " + kode);
        }
        return ad;
    }

    public static Map<String, ArkivFilType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return getKode();
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

    public static ArkivFilType finnForKodeverkEiersKode(String offisiellDokumentType) {
        return List.of(values()).stream().filter(k -> Objects.equals(k.getOffisiellKode(), offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }
}
