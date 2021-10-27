package no.nav.k9.kodeverk.behandling;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BehandlingResultatType implements Kodeverdi {

    IKKE_FASTSATT("IKKE_FASTSATT", "Ikke fastsatt"),
    INNVILGET("INNVILGET", "Innvilget"),
    DELVIS_INNVILGET("DELVIS_INNVILGET", "Delvis innvilget"),
    AVSLÅTT("AVSLÅTT", "Avslått"),
    /** @deprecated OPPHØR brukes ikke lenger, om det er opphør (mot OS) beregnes i k9-oppdrag */
    OPPHØR("OPPHØR", "Opphør"),
    HENLAGT_SØKNAD_TRUKKET("HENLAGT_SØKNAD_TRUKKET", "Henlagt, søknaden er trukket", true),
    HENLAGT_FEILOPPRETTET("HENLAGT_FEILOPPRETTET", "Henlagt, søknaden er feilopprettet", true),
    HENLAGT_BRUKER_DØD("HENLAGT_BRUKER_DØD", "Henlagt, brukeren er død", true),
    MERGET_OG_HENLAGT("MERGET_OG_HENLAGT", "Mottatt ny søknad", true),
    /** @deprecated HENLAGT_SØKNAD_MANGLER erstattes av HENLAGT_MASKINELT */
    HENLAGT_SØKNAD_MANGLER("HENLAGT_SØKNAD_MANGLER", "Henlagt søknad mangler", true),
    HENLAGT_MASKINELT("HENLAGT_MASKINELT", "Henlagt maskinelt", true),
    INNVILGET_ENDRING("INNVILGET_ENDRING", "Endring innvilget"),
    INGEN_ENDRING("INGEN_ENDRING", "Ingen endring"),
    MANGLER_BEREGNINGSREGLER("MANGLER_BEREGNINGSREGLER", "Mangler beregningsregler", true),
    ;

    private static final Set<BehandlingResultatType> HENLEGGELSESKODER_FOR_SØKNAD;
    private static final Set<BehandlingResultatType> ALLE_HENLEGGELSESKODER;
    private static final Set<BehandlingResultatType> INNVILGET_KODER = Set.of(INNVILGET, DELVIS_INNVILGET, INNVILGET_ENDRING);

    private static final Map<String, BehandlingResultatType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "BEHANDLING_RESULTAT_TYPE";


    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }

        var henlagte = KODER.values().stream().filter(v -> v.erHenleggelse).collect(Collectors.toSet());
        ALLE_HENLEGGELSESKODER = EnumSet.copyOf(henlagte);

        var henlagtSøknad = EnumSet.copyOf(henlagte);
        henlagtSøknad.remove(MERGET_OG_HENLAGT);

        HENLEGGELSESKODER_FOR_SØKNAD = henlagtSøknad;
    }

    @JsonIgnore
    private String navn;

    private String kode;

    @JsonIgnore
    private boolean erHenleggelse;

    private BehandlingResultatType(String kode) {
        this.kode = kode;
    }

    private BehandlingResultatType(String kode, String navn) {
        this(kode, navn, false);
    }

    private BehandlingResultatType(String kode, String navn, boolean erHenleggelse) {
        this.kode = kode;
        this.navn = navn;
        this.erHenleggelse = erHenleggelse;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static BehandlingResultatType fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(BehandlingResultatType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingResultatType: for input " + node);
        }
        return ad;
    }

    public static Map<String, BehandlingResultatType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean erHenleggelse() {
        return erHenleggelse;
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

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    public static Set<BehandlingResultatType> getAlleHenleggelseskoder() {
        return ALLE_HENLEGGELSESKODER;
    }

    public static Set<BehandlingResultatType> getHenleggelseskoderForSøknad() {
        return HENLEGGELSESKODER_FOR_SØKNAD;
    }

    public static Set<BehandlingResultatType> getInnvilgetKoder() {
        return INNVILGET_KODER;
    }

    public boolean isBehandlingHenlagt() {
        return BehandlingResultatType.getAlleHenleggelseskoder().contains(this);
    }

    public boolean isBehandlingsresultatHenlagt() {
        return BehandlingResultatType.getHenleggelseskoderForSøknad().contains(this);
    }

    public boolean isBehandlingsresultatOpphørt() {
        return BehandlingResultatType.OPPHØR.equals(this);
    }

    public boolean isBehandlingsresultatIkkeEndret() {
        return BehandlingResultatType.INGEN_ENDRING.equals(this);
    }

}
