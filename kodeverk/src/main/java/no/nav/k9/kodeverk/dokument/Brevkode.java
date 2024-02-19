package no.nav.k9.kodeverk.dokument;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

    public static final Comparator<? super Brevkode> COMP_REKKEFØLGE = Comparator.comparing(Brevkode::getRangering, Comparator.nullsLast(Comparator.naturalOrder()));
    private static final Map<String, Brevkode> KODER = new LinkedHashMap<>();

    public static final int VEDLEGG_RANGERING = 99;
    public static final int INNTEKTSMELDING_RANGERING = 10;
    public static final int SØKNAD_RANGERING = 1;

    public static final String SØKNAD_OMS_UTVIDETRETT_MA_KODE = "SØKNAD_OMS_UTVIDETRETT_MA";
    public static final String ETTERSENDELSE_OMS_UTVIDETRETT_MA_KODE = "ETTERSENDELSE_OMS_UTVIDETRETT_MA";
    public static final String SØKNAD_OMS_UTVIDETRETT_KS_KODE = "SØKNAD_OMS_UTVIDETRETT_KS";
    public static final String ETTERSENDELSE_OMS_UTVIDETRETT_KS_KODE = "ETTERSENDELSE_OMS_UTVIDETRETT_KS";
    public static final String SØKNAD_OMS_UTVIDETRETT_AO_KODE = "SØKNAD_OMS_UTVIDETRETT_AO";
    public static final String ETTERSENDELSE_OMS_UTVIDETRETT_AO_KODE = "ETTERSENDELSE_OMS_UTVIDETRETT_AO";
    public static final String SØKNAD_UTBETALING_OMS_KODE = "SØKNAD_UTBETALING_OMS";
    public static final String ETTERSENDELSE_UTBETALING_OMS_KODE = "ETTERSENDELSE_UTBETALING_OMS";
    public static final String SØKNAD_UTBETALING_OMS_AT_KODE = "SØKNAD_UTBETALING_OMS_AT";
    public static final String ETTERSENDELSE_UTBETALING_OMS_AT_KODE = "ETTERSENDELSE_UTBETALING_OMS_AT";
    public static final String PAPIRSØKNAD_UTBETALING_OMS_AT_KODE = "PAPIRSØKNAD_UTBETALING_OMS_AT";
    public static final String FRAVÆRSKORRIGERING_IM_OMS_KODE = "FRAVÆRSKORRIGERING_IM_OMS";
    public static final String INNTEKTSMELDING_KODE = "INNTEKTSMELDING";
    // Match mot Deprecated {@link no.nav.k9.kodeverk.dokument.DokumentTypeId}
    public static final Brevkode INNTEKTSMELDING = new Brevkode(INNTEKTSMELDING_KODE, "4936", INNTEKTSMELDING_RANGERING);
    public static final Brevkode LEGEERKLÆRING = new Brevkode("LEGEERKLÆRING", "I000023", VEDLEGG_RANGERING);
    public static final Brevkode INNTEKTKOMP_FRILANS = new Brevkode("INNTEKTKOMP_FRILANS", "NAV 00-03.02", SØKNAD_RANGERING);
    /**
     * Omsorgspenger brevkoder.
     */
    public static final Brevkode SØKNAD_UTBETALING_OMS = new Brevkode(SØKNAD_UTBETALING_OMS_KODE, "NAV 09-35.01", SØKNAD_RANGERING);
    public static final Brevkode ETTERSENDELSE_UTBETALING_OMS = new Brevkode(ETTERSENDELSE_UTBETALING_OMS_KODE, "NAVe 09-35.01", VEDLEGG_RANGERING);
    public static final Brevkode SØKNAD_UTBETALING_OMS_AT = new Brevkode(SØKNAD_UTBETALING_OMS_AT_KODE, "NAV 09-35.02", SØKNAD_RANGERING);
    public static final Brevkode ETTERSENDELSE_UTBETALING_OMS_AT = new Brevkode(ETTERSENDELSE_UTBETALING_OMS_AT_KODE, "NAVe 09-35.02", VEDLEGG_RANGERING);
    public static final Brevkode PAPIRSØKNAD_UTBETALING_OMS_AT = new Brevkode(PAPIRSØKNAD_UTBETALING_OMS_AT_KODE, "NAV 09-09.01", SØKNAD_RANGERING);
    public static final Brevkode FRAVÆRSKORRIGERING_IM_OMS = new Brevkode(FRAVÆRSKORRIGERING_IM_OMS_KODE, FRAVÆRSKORRIGERING_IM_OMS_KODE, SØKNAD_RANGERING);
    public static final Brevkode SØKNAD_OMS_UTVIDETRETT_KS = new Brevkode(SØKNAD_OMS_UTVIDETRETT_KS_KODE, "NAV 09-06.05", SØKNAD_RANGERING);
    public static final Brevkode ETTERSENDELSE_OMS_UTVIDETRETT_KS = new Brevkode(ETTERSENDELSE_OMS_UTVIDETRETT_KS_KODE, "NAVe 09-06.05", VEDLEGG_RANGERING);
    public static final Brevkode SØKNAD_OMS_UTVIDETRETT_MA = new Brevkode(SØKNAD_OMS_UTVIDETRETT_MA_KODE, "NAV 09-06.07", SØKNAD_RANGERING);
    public static final Brevkode ETTERSENDELSE_OMS_UTVIDETRETT_MA = new Brevkode(ETTERSENDELSE_OMS_UTVIDETRETT_MA_KODE, "NAVe 09-06.07", VEDLEGG_RANGERING);
    public static final Brevkode SØKNAD_OMS_UTVIDETRETT_AO = new Brevkode(SØKNAD_OMS_UTVIDETRETT_AO_KODE, "NAV 09-06.10", SØKNAD_RANGERING);
    public static final Brevkode ETTERSENDELSE_OMS_UTVIDETRETT_AO = new Brevkode(ETTERSENDELSE_OMS_UTVIDETRETT_AO_KODE, "NAVe 09-06.10", VEDLEGG_RANGERING);
    /**
     * Pleiepenger brevkoder.
     */
    public static final String PLEIEPENGER_BARN_SOKNAD_KODE = "PLEIEPENGER_SOKNAD";
    public static final String ETTERSENDELSE_PLEIEPENGER_BARN_KODE = "ETTERSENDELSE_PLEIEPENGER_SYKT_BARN";
    public static final String SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE_KODE = "PLEIEPENGER_LIVETS_SLUTTFASE_SOKNAD";
    public static final String ETTERSENDELSE_PLEIEPENGER_LIVETS_SLUTTFASE_KODE = "ETTERSENDELSE_PLEIEPENGER_LIVETS_SLUTTFASE_KODE";
    public static final Brevkode PLEIEPENGER_BARN_SOKNAD = new Brevkode(PLEIEPENGER_BARN_SOKNAD_KODE, "NAV 09-11.05", SØKNAD_RANGERING);
    public static final Brevkode ETTERSENDELSE_PLEIEPENGER_SYKT_BARN = new Brevkode(ETTERSENDELSE_PLEIEPENGER_BARN_KODE, "NAVe 09-11.05", VEDLEGG_RANGERING);
    public static final Brevkode SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE = new Brevkode(SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE_KODE, "NAV 09-12.05", SØKNAD_RANGERING);
    public static final Brevkode ETTERSENDELSE_PLEIEPENGER_LIVETS_SLUTTFASE = new Brevkode(ETTERSENDELSE_PLEIEPENGER_LIVETS_SLUTTFASE_KODE, "NAVe 09-12.05", VEDLEGG_RANGERING);
    /**
     * Opplæringspenger brevkoder.
     */
    public static final String OPPLÆRINGSPENGER_SOKNAD_KODE = "OPPLÆRINGSPENGER_SOKNAD";
    public static final Brevkode OPPLÆRINGSPENGER_SOKNAD = new Brevkode(OPPLÆRINGSPENGER_SOKNAD_KODE, "NAV 09-11.08", SØKNAD_RANGERING);
    public static final String DOKUMENTASJON_AV_OPPLÆRING_KODE = "DOKUMENTASJON_AV_OPPLÆRING";
    public static final String LEGEERKLÆRING_MED_DOKUMENTASJON_AV_OPPLÆRING_KODE = "LEGEERKLÆRING_MED_DOKUMENTASJON_AV_OPPLÆRING";
    public static final Brevkode DOKUMENTASJON_AV_OPPLÆRING = new Brevkode(DOKUMENTASJON_AV_OPPLÆRING_KODE, DOKUMENTASJON_AV_OPPLÆRING_KODE, VEDLEGG_RANGERING);
    public static final Brevkode LEGEERKLÆRING_MED_DOKUMENTASJON_AV_OPPLÆRING = new Brevkode(LEGEERKLÆRING_MED_DOKUMENTASJON_AV_OPPLÆRING_KODE, LEGEERKLÆRING_MED_DOKUMENTASJON_AV_OPPLÆRING_KODE, VEDLEGG_RANGERING);
    // Default
    public static final Brevkode UDEFINERT = new Brevkode("-", null, VEDLEGG_RANGERING);
    public static final String KODEVERK = "DOKUMENT_TYPE_ID";
    private String offisiellKode;

    public static final Set<Brevkode> SØKNAD_TYPER = Set.of(
        PLEIEPENGER_BARN_SOKNAD,
        OPPLÆRINGSPENGER_SOKNAD,
        SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE,
        SØKNAD_UTBETALING_OMS,
        SØKNAD_UTBETALING_OMS_AT,
        PAPIRSØKNAD_UTBETALING_OMS_AT,
        SØKNAD_OMS_UTVIDETRETT_KS,
        SØKNAD_OMS_UTVIDETRETT_MA,
        SØKNAD_OMS_UTVIDETRETT_AO,
        INNTEKTKOMP_FRILANS);

    public static final Set<Brevkode> ETTERSENDELSE_TYPER = Set.of(
        ETTERSENDELSE_PLEIEPENGER_SYKT_BARN,
        ETTERSENDELSE_PLEIEPENGER_LIVETS_SLUTTFASE,
        ETTERSENDELSE_UTBETALING_OMS,
        ETTERSENDELSE_UTBETALING_OMS_AT,
        ETTERSENDELSE_OMS_UTVIDETRETT_KS,
        ETTERSENDELSE_OMS_UTVIDETRETT_MA,
        ETTERSENDELSE_OMS_UTVIDETRETT_AO
        );

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

    @JsonCreator(mode = Mode.DELEGATING)
    public static Brevkode fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(Brevkode.class, node, "kode");
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
