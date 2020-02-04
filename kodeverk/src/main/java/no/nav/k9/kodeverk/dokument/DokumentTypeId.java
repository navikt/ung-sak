package no.nav.k9.kodeverk.dokument;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import no.nav.k9.kodeverk.api.Kodeverdi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DokumentTypeId er et kodeverk som forvaltes av Kodeverkforvaltning. Det er et subsett av kodeverket DokumentType, mer spesifikt alle
 * inngående dokumenttyper.
 * @deprecated FIXME K9 fjern dette kodeverket, erstatt med noe mer forutsigbart
 */
@Deprecated
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum DokumentTypeId implements Kodeverdi {

    SØKNAD_ENGANGSSTØNAD_FØDSEL("SØKNAD_ENGANGSSTØNAD_FØDSEL", "I000003"),
    SØKNAD_ENGANGSSTØNAD_ADOPSJON("SØKNAD_ENGANGSSTØNAD_ADOPSJON", "I000004"),
    SØKNAD_FORELDREPENGER_FØDSEL("SØKNAD_FORELDREPENGER_FØDSEL", "I000005"),
    SØKNAD_FORELDREPENGER_ADOPSJON("SØKNAD_FORELDREPENGER_ADOPSJON", "I000002"),
    FORELDREPENGER_ENDRING_SØKNAD("FORELDREPENGER_ENDRING_SØKNAD", "I000050"),
    INNTEKTSMELDING("INNTEKTSMELDING", "I000067"),
    DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL("DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL", "I000041"),
    LEGEERKLÆRING("LEGEERKLÆRING", "I000023"),
    ANNET("ANNET", "I000060"),
    UDEFINERT("-", null),
    ;

    private static final Map<String, DokumentTypeId> KODER = new LinkedHashMap<>();
    private static final Set<String> VEDLEGG_TYPER = Set.of(LEGEERKLÆRING,
        DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL)
        .stream().flatMap(dti -> List.of(dti.getKode(), dti.getOffisiellKode()).stream()).collect(Collectors.toSet());

    private static final Set<String> SØKNAD_TYPER = Set.of(SØKNAD_ENGANGSSTØNAD_FØDSEL, SØKNAD_FORELDREPENGER_FØDSEL,
        SØKNAD_ENGANGSSTØNAD_ADOPSJON, SØKNAD_FORELDREPENGER_ADOPSJON)
        .stream().flatMap(dti -> List.of(dti.getKode(), dti.getOffisiellKode()).stream()).collect(Collectors.toSet());

    private static final Set<String> ENDRING_SØKNAD_TYPER = Set.of(FORELDREPENGER_ENDRING_SØKNAD)
        .stream().flatMap(dti -> List.of(dti.getKode(), dti.getOffisiellKode()).stream()).collect(Collectors.toSet());

    private static final Set<String> ANDRE_SPESIAL_TYPER = Set.of(INNTEKTSMELDING)
        .stream().flatMap(dti -> List.of(dti.getKode(), dti.getOffisiellKode()).stream()).collect(Collectors.toSet());

    public static final String KODEVERK = "DOKUMENT_TYPE_ID";

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

    private DokumentTypeId(String kode, String offisiellKode) {
        this.kode = kode;
        this.offisiellKode = offisiellKode;

    }

    public static Set<String> getVedleggTyper() {
        return VEDLEGG_TYPER;
    }

    public static Set<String> getSpesialTyperKoder() {
        Set<String> typer = new LinkedHashSet<>(SØKNAD_TYPER);
        typer.addAll(ENDRING_SØKNAD_TYPER);
        typer.addAll(ANDRE_SPESIAL_TYPER);
        return Collections.unmodifiableSet(typer);

    }

    public static Set<String> getSøknadTyper() {
        return SØKNAD_TYPER;
    }

    public static Set<String> getEndringSøknadTyper() {
        return ENDRING_SØKNAD_TYPER;
    }

    public boolean erSøknadType() {
        return SØKNAD_TYPER.contains(this.getKode());
    }

    public boolean erEndringsSøknadType() {
        return ENDRING_SØKNAD_TYPER.contains(this.getKode());
    }

    @JsonCreator
    public static DokumentTypeId fraKode(@JsonProperty("kode") String kode) {
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

    @Override
    public String getNavn() {
        return getKode();
    }

    @JsonProperty
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

    public static DokumentTypeId finnForKodeverkEiersKode(String offisiellDokumentType) {
        if (offisiellDokumentType == null)
            return DokumentTypeId.UDEFINERT;

        Optional<DokumentTypeId> dokId = KODER.values().stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst();
        if (dokId.isPresent()) {
            return dokId.get();
        } else {
            // FIXME K9 - erstatt DokumentTypeId helt med kodeverk vi kan ha tillit til
            throw new IllegalArgumentException("Ukjent offisiellDokumentType: " + offisiellDokumentType);
        }
    }

    public static boolean erSøknadType(DokumentTypeId dokumentTypeId) {
        return fraKode(dokumentTypeId.getKode()).erSøknadType();
    }

    public static boolean erEndringsSøknadType(DokumentTypeId dokumentTypeId) {
        return fraKode(dokumentTypeId.getKode()).erEndringsSøknadType();
    }

    public static Map<String, DokumentTypeId> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

}
