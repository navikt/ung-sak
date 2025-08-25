package no.nav.ung.kodeverk.behandling;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.*;
import java.util.stream.Collectors;

public enum BehandlingResultatType implements Kodeverdi {

    IKKE_FASTSATT("IKKE_FASTSATT", "Ikke fastsatt"),
    INNVILGET("INNVILGET", "Innvilget"),
    DELVIS_INNVILGET("DELVIS_INNVILGET", "Delvis innvilget"),
    AVSLÅTT("AVSLÅTT", "Avslått"),
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

    private String navn;

    private String kode;

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

    public static BehandlingResultatType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingResultatType: for input " + kode);
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

    public boolean erHenleggelse() {
        return erHenleggelse;
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
