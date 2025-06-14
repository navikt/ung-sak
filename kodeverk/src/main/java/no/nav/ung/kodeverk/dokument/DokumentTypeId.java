package no.nav.ung.kodeverk.dokument;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.*;

/**
 * DokumentTypeId er et kodeverk som forvaltes av Kodeverkforvaltning. Det er et subsett av kodeverket DokumentType, mer spesifikt alle
 * inngående dokumenttyper.
 */
public enum DokumentTypeId implements Kodeverdi {

    LEGEERKLÆRING("LEGEERKLÆRING", "I000023", DokumentGruppe.VEDLEGG, Brevkode.LEGEERKLÆRING),
    UDEFINERT("-", null, null, null),
    ;

    public static final String KODEVERK = "DOKUMENT_TYPE_ID";
    private static final Map<String, DokumentTypeId> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            if (KODER.putIfAbsent(v.offisiellKode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.offisiellKode);
            }
        }
    }

    private String navn;

    private String offisiellKode;

    private String kode;

    private DokumentGruppe dokumentGruppe;

    private Brevkode brevkode;

    private DokumentTypeId(String kode, String offisiellKode, DokumentGruppe dokumentGruppe, Brevkode brevkode) {
        this.kode = kode;
        this.offisiellKode = offisiellKode;
        this.dokumentGruppe = dokumentGruppe;
        this.brevkode = brevkode;

    }

    public static DokumentTypeId  fraKode(final String kode)  {
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

    public static DokumentTypeId finnForKodeverkEiersKode(String offisiellDokumentType) {
        if (offisiellDokumentType == null || offisiellDokumentType.isBlank())
            return DokumentTypeId.UDEFINERT;

        Optional<DokumentTypeId> dokId = KODER.values().stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst();
        if (dokId.isPresent()) {
            return dokId.get();
        } else {
            // FIXME K9 - erstatt DokumentTypeId helt med kodeverk vi kan ha tillit til
            throw new IllegalArgumentException("Ukjent offisiellDokumentType: " + offisiellDokumentType);
        }
    }

    public static Map<String, DokumentTypeId> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return getKode();
    }

    public DokumentGruppe getDokumentGruppe() {
        return dokumentGruppe;
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

    public Brevkode getBrevkode() {
        return brevkode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

}
