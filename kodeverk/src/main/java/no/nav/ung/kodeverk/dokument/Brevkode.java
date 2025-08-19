package no.nav.ung.kodeverk.dokument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.LegacyKodeverdiJsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.*;

/**
 * Brevkode er et kodeverk som forvaltes av Kodeverkforvaltning.
 */
@LegacyKodeverdiJsonValue // Serialiserast som kode string i default object mapper
public class Brevkode implements Kodeverdi {

    public static final Comparator<? super Brevkode> COMP_REKKEFØLGE = Comparator.comparing(Brevkode::getRangering, Comparator.nullsLast(Comparator.naturalOrder()));
    private static final Map<String, Brevkode> KODER = new LinkedHashMap<>();

    public static final int VEDLEGG_RANGERING = 99;
    public static final int INNTEKTRAPPORTERING_RANGERING = 10;
    public static final int SØKNAD_RANGERING = 1;

    /**
     * Ung brevkoder.
     */
    public static final String UNGDOMSYTELSE_SOKNAD_KODE = "UNGDOMSYTELSE_SOKNAD";
    public static final Brevkode UNGDOMSYTELSE_SOKNAD = new Brevkode(UNGDOMSYTELSE_SOKNAD_KODE, "NAV 76-13.92", SØKNAD_RANGERING);

    public static final String UNGDOMSYTELSE_INNTEKTRAPPORTERING_KODE = "UNGDOMSYTELSE_INNTEKTRAPPORTERING";
    public static final Brevkode UNGDOMSYTELSE_INNTEKTRAPPORTERING = new Brevkode(UNGDOMSYTELSE_INNTEKTRAPPORTERING_KODE, "NAV 76-13.93", INNTEKTRAPPORTERING_RANGERING);

    public static final String UNGDOMSYTELSE_VARSEL_UTTALELSE_KODE = "UNGDOMSYTELSE_VARSEL_UTTALELSE";
    public static final Brevkode UNGDOMSYTELSE_VARSEL_UTTALELSE = new Brevkode(UNGDOMSYTELSE_VARSEL_UTTALELSE_KODE, "NAV 76-13.94", VEDLEGG_RANGERING);


    // Default
    public static final Brevkode UDEFINERT = new Brevkode("-", null, VEDLEGG_RANGERING);
    public static final String KODEVERK = "DOKUMENT_TYPE_ID";
    private String offisiellKode;

    public static final Set<Brevkode> SØKNAD_TYPER = Set.of(UNGDOMSYTELSE_SOKNAD);

    @JsonValue
    private String kode;

    private int rangering;

    /**
     * intern ctor for registrerte koder.
     */
    private Brevkode(String kode, String offisiellKode, int rangering) {
        this(kode);
        this.offisiellKode = offisiellKode;
        this.rangering = rangering;

        if (KODER.putIfAbsent(kode, this) != null) {
            throw new IllegalArgumentException("Duplikat : " + kode);
        }
        if (KODER.putIfAbsent(offisiellKode, this) != null && !Objects.equals(offisiellKode, kode)) {
            throw new IllegalArgumentException("Duplikat : " + offisiellKode);
        }
    }

    /**
     * value object ctor - brukes f.eks av Jax-Rs dersom spesifiseres direkte.
     */
    protected Brevkode(String kode) {
        this.kode = Objects.requireNonNull(kode, "kode");
    }

    @JsonCreator
    public static Brevkode fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        Objects.requireNonNull(kode, "kode");
        var ad = KODER.get(kode);

        if (ad == null) {
            // midlertidig fallback til vi endrer til offisielle kodeverdier
            ad = finnForKodeverkEiersKode(kode);
            if (ad == null) {
                // returnerer ny kode hvis ikke finnes blant offisielt registrete kodeverdier
                return new Brevkode(kode);
            }
        }
        return ad;
    }

    private static Brevkode finnForKodeverkEiersKode(String offisiellDokumentType) {
        if (offisiellDokumentType == null || offisiellDokumentType.isBlank())
            return Brevkode.UDEFINERT;

        Optional<Brevkode> dokId = KODER.values().stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst();
        if (dokId.isPresent()) {
            return dokId.get();
        } else {
            return new Brevkode(offisiellDokumentType);
        }
    }

    public static Map<String, Brevkode> registrerteKoder() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !getClass().equals(obj.getClass()))
            return false;
        var other = (Brevkode) obj;
        return Objects.equals(kode, other.kode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kode);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<kode=" + kode + ">";
    }

    @Override
    public String getNavn() {
        return getKode();
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    public int getRangering() {
        return rangering;
    }
}
