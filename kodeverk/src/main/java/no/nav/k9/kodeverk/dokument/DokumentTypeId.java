package no.nav.k9.kodeverk.dokument;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;

/**
 * DokumentTypeId er et kodeverk som forvaltes av Kodeverkforvaltning. Det er et subsett av kodeverket DokumentType, mer spesifikt alle
 * inngående dokumenttyper.
 */
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum DokumentTypeId implements Kodeverdi {

    INNTEKTSMELDING("INNTEKTSMELDING", "I000067", DokumentGruppe.INNTEKTSMELDING, Brevkode.INNTEKTSMELDING),
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

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private String offisiellKode;

    private String kode;
    
    @JsonIgnore
    private DokumentGruppe dokumentGruppe;

    @JsonIgnore
    private Brevkode brevkode;

    private DokumentTypeId(String kode, String offisiellKode, DokumentGruppe dokumentGruppe, Brevkode brevkode) {
        this.kode = kode;
        this.offisiellKode = offisiellKode;
        this.dokumentGruppe = dokumentGruppe;
        this.brevkode = brevkode;

    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static DokumentTypeId  fraKode(Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(DokumentTypeId.class, node, "kode");
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

    public Brevkode getBrevkode() {
        return brevkode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

}
