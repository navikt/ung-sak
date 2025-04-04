package no.nav.ung.kodeverk.dokument;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;

/**
 * NB: Pass på! Ikke legg koder vilkårlig her
 */
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum DokumentStatus implements Kodeverdi {

    /** Dokument mottatt, ikke godtatt for behandling ennå (må sette til GYLDIG først). */
    MOTTATT("MOTTATT", "Mottar", 1),

    /** Dokumentet er {@link #MOTTATT}, vurderer om {@link #GYLDIG}/komplett. */
    BEHANDLER("BEHANDLER", "Behandler/vurderer dokument", 2),

    /** Dokument mottatt og validert gyldig. */
    GYLDIG("GYLDIG", "Gyldig", 3),

    /** Dokument henlagt av saksbehandler. */
    HENLAGT("HENLAGT", "Henlagt av saksbehandler", 4),

    /** Dokument vurdert som ugyldig. */
    UGYLDIG("UGYLDIG", "Ugyldig", 4),
    ;

    private static final Map<String, DokumentStatus> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "BEHANDLING_STATUS";

    @JsonIgnore
    private String navn;

    private String kode;

    @JsonIgnore
    private int rank;

    private DokumentStatus(String kode) {
        this.kode = kode;
    }

    private DokumentStatus(String kode, String navn, int rank) {
        this.kode = kode;
        this.navn = navn;
        this.rank = rank;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static DokumentStatus  fraKode(Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(DokumentStatus.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent DokumentStatus: " + kode);
        }
        return ad;
    }

    public static Map<String, DokumentStatus> kodeMap() {
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

    public boolean erGyldigTransisjon(DokumentStatus tilStatus) {
        return this.rank <= tilStatus.rank;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }
}
