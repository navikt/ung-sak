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
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;

/**
 * Brevkode er et kodeverk som forvaltes av Kodeverkforvaltning.
 */
@JsonFormat(shape = Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Brevkode implements Kodeverdi {

    public static final String INNTEKTSMELDING_KODE = "INNTEKTSMELDING";

    // Match mot Deprecated {@link no.nav.k9.kodeverk.dokument.DokumentTypeId}
    public static final Brevkode INNTEKTSMELDING = new Brevkode(INNTEKTSMELDING_KODE, "4936");
    public static final Brevkode LEGEERKLÆRING =new Brevkode("LEGEERKLÆRING", "I000023");
    public static final Brevkode INNTEKTKOMP_FRILANS = new Brevkode("INNTEKTKOMP_FRILANS", "NAV 00-03.02");
    // Default
    public static final Brevkode UDEFINERT = new Brevkode("-", null);

    public static final String KODEVERK = "DOKUMENT_TYPE_ID";
    private static final Map<String, Brevkode> KODER = new LinkedHashMap<>();

    private String offisiellKode;

    @JsonValue
    private String kode;

    /** intern ctor for registrerte koder. */
    private Brevkode(String kode, String offisiellKode) {
        this(kode);
        this.offisiellKode = offisiellKode;

        if (KODER.putIfAbsent(kode, this) != null) {
            throw new IllegalArgumentException("Duplikat : " + kode);
        }
        if (KODER.putIfAbsent(offisiellKode, this) != null && !Objects.equals(offisiellKode, kode)) {
            throw new IllegalArgumentException("Duplikat : " + offisiellKode);
        }
    }

    /** value object ctor. */
    protected Brevkode(String kode) {
        this.kode = Objects.requireNonNull(kode, "kode");
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static Brevkode fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(Brevkode.class, node, "kode");
        Objects.requireNonNull(kode, "kode");
        var ad = KODER.get(kode);

        if (ad == null) {
            // midlertidig fallback til vi endrer til offisille kodeverdier
            ad = finnForKodeverkEiersKode(kode);
            if (ad == null) {
                // returnerer ny kode hvis ikke finnes blant offisielt registrete kodeverdier
                return new Brevkode(kode);
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

    public static Map<String, Brevkode> registrerteKoder() {
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

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

}
