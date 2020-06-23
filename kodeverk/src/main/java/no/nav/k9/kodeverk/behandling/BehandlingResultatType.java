package no.nav.k9.kodeverk.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BehandlingResultatType implements Kodeverdi {

    IKKE_FASTSATT("IKKE_FASTSATT", "Ikke fastsatt"),
    INNVILGET("INNVILGET", "Innvilget"),
    AVSLÅTT("AVSLÅTT", "Avslått"),
    OPPHØR("OPPHØR", "Opphør"),
    HENLAGT_SØKNAD_TRUKKET("HENLAGT_SØKNAD_TRUKKET", "Henlagt, søknaden er trukket", true),
    HENLAGT_FEILOPPRETTET("HENLAGT_FEILOPPRETTET", "Henlagt, søknaden er feilopprettet", true),
    HENLAGT_BRUKER_DØD("HENLAGT_BRUKER_DØD", "Henlagt, brukeren er død", true),
    MERGET_OG_HENLAGT("MERGET_OG_HENLAGT", "Mottatt ny søknad", true),
    HENLAGT_SØKNAD_MANGLER("HENLAGT_SØKNAD_MANGLER", "Henlagt søknad mangler", true),
    INNVILGET_ENDRING("INNVILGET_ENDRING", "Endring innvilget"),
    INGEN_ENDRING("INGEN_ENDRING", "Ingen endring"),
    MANGLER_BEREGNINGSREGLER("MANGLER_BEREGNINGSREGLER", "Mangler beregningsregler", true),
    ;

    private static final Set<BehandlingResultatType> HENLEGGELSESKODER_FOR_SØKNAD = Set.of(HENLAGT_SØKNAD_TRUKKET, HENLAGT_FEILOPPRETTET, HENLAGT_BRUKER_DØD, HENLAGT_SØKNAD_MANGLER,
        MANGLER_BEREGNINGSREGLER);
    private static final Set<BehandlingResultatType> ALLE_HENLEGGELSESKODER = Set.of(HENLAGT_SØKNAD_TRUKKET, HENLAGT_FEILOPPRETTET, HENLAGT_BRUKER_DØD, MERGET_OG_HENLAGT, HENLAGT_SØKNAD_MANGLER,
        MANGLER_BEREGNINGSREGLER);
    private static final Set<BehandlingResultatType> INNVILGET_KODER = Set.of(INNVILGET, INNVILGET_ENDRING);

    private static final Map<String, BehandlingResultatType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "BEHANDLING_RESULTAT_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
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

    @JsonCreator
    public static BehandlingResultatType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingResultatType: " + kode);
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

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
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

    public boolean erHenlagt() {
        return ALLE_HENLEGGELSESKODER.contains(this);
    }

    public boolean isBehandlingHenlagt() {
        return BehandlingResultatType.getAlleHenleggelseskoder().contains(this);
    }

    public boolean isBehandlingsresultatAvslåttOrOpphørt() {
        return BehandlingResultatType.AVSLÅTT.equals(this)
            || BehandlingResultatType.OPPHØR.equals(this);
    }

    public boolean isBehandlingsresultatAvslått() {
        return BehandlingResultatType.AVSLÅTT.equals(this);
    }

    public boolean isBehandlingsresultatOpphørt() {
        return BehandlingResultatType.OPPHØR.equals(this);
    }

    public boolean isBehandlingsresultatIkkeEndret() {
        return BehandlingResultatType.INGEN_ENDRING.equals(this);
    }
    
    public boolean isBehandlingsresultatHenlagt() {
        return BehandlingResultatType.getHenleggelseskoderForSøknad().contains(this);
    }

}
