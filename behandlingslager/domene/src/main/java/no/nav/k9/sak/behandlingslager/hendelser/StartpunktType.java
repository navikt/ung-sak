package no.nav.k9.sak.behandlingslager.hendelser;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.VilkårType;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum StartpunktType implements Kodeverdi {

    KONTROLLER_ARBEIDSFORHOLD("KONTROLLER_ARBEIDSFORHOLD", "Startpunkt kontroller arbeidsforhold", 1, BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD),
    KONTROLLER_FAKTA("KONTROLLER_FAKTA", "Kontroller fakta", 2, BehandlingStegType.KONTROLLER_FAKTA),
    INNGANGSVILKÅR_OPPLYSNINGSPLIKT("INNGANGSVILKÅR_OPPL", "Inngangsvilkår opplysningsplikt", 3, BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT),
    INNGANGSVILKÅR_MEDLEMSKAP("INNGANGSVILKÅR_MEDL", "Inngangsvilkår medlemskapsvilkår", 5, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR),
    OPPTJENING("OPPTJENING", "Opptjening", 6, BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE),
    BEREGNING("BEREGNING", "Beregning", 7, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING),
    UTTAKSVILKÅR("UTTAKSVILKÅR", "Uttaksvilkår", 8, BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP),

    UDEFINERT("-", "Ikke definert", 99, BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT),
    ;

    public static final String KODEVERK = "STARTPUNKT_TYPE";
    private static final Map<String, StartpunktType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    static Map<StartpunktType, Set<VilkårType>> VILKÅR_HÅNDTERT_INNEN_STARTPUNKT = new HashMap<>();
    static {
        // Kontroller arbeidsforhold - ingen vilkår håndter før dette startpunktet
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.KONTROLLER_ARBEIDSFORHOLD,
            new HashSet<>());

        // Kontroller Fakta - ingen vilkår håndter før dette startpunktet
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.KONTROLLER_FAKTA,
            new HashSet<>());

        // Opplysningsplikt - ingen vilkår håndter før dette startpunktet
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT,
            new HashSet<>());

        // Medlemskap
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));

        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.OPPTJENING,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(StartpunktType.OPPTJENING).addAll(new HashSet<>(
            Collections.singletonList(VilkårType.MEDLEMSKAPSVILKÅRET)));

        // Beregning
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.BEREGNING,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(StartpunktType.BEREGNING).addAll(new HashSet<>(
            Arrays.asList(VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårType.OPPTJENINGSVILKÅRET)));

        // Uttak
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.UTTAKSVILKÅR,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(StartpunktType.UTTAKSVILKÅR).addAll(new HashSet<>(
            Collections.singletonList(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)));
    }

    @JsonIgnore
    private BehandlingStegType behandlingSteg;

    @JsonIgnore
    private int rangering;

    @JsonIgnore
    private String navn;

    private String kode;

    private StartpunktType(String kode) {
        this.kode = kode;
    }

    private StartpunktType(String kode, String navn, int rangering, BehandlingStegType stegType) {
        this.kode = kode;
        this.navn = navn;
        this.rangering = rangering;
        this.behandlingSteg = stegType;
    }

    @JsonCreator
    public static StartpunktType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent StartpunktType: " + kode);
        }
        return ad;
    }

    public static Map<String, StartpunktType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return this.kode;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    public BehandlingStegType getBehandlingSteg() {
        return behandlingSteg;
    }

    public String getTransisjonIdentifikator() {
        return "revurdering-fremover-til-" + getBehandlingSteg().getKode();
    }

    public static Set<VilkårType> finnVilkårHåndtertInnenStartpunkt(StartpunktType startpunkt) {
        return VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(startpunkt);
    }

    public int getRangering() {
        return rangering;
    }
}
