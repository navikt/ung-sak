package no.nav.ung.kodeverk.dokument;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NB: Pass på! Ikke legg koder vilkårlig her
 */
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

    private String navn;

    private String kode;

    private int rank;

    private DokumentStatus(String kode) {
        this.kode = kode;
    }

    private DokumentStatus(String kode, String navn, int rank) {
        this.kode = kode;
        this.navn = navn;
        this.rank = rank;
    }

    public static DokumentStatus  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
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
