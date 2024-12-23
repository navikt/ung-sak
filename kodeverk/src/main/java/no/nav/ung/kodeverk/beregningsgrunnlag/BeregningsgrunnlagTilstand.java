package no.nav.ung.kodeverk.beregningsgrunnlag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;


@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BeregningsgrunnlagTilstand implements Kodeverdi {

    OPPRETTET("OPPRETTET", "Opprettet"),
    FASTSATT_BEREGNINGSAKTIVITETER("FASTSATT_BEREGNINGSAKTIVITETER", "Fastsatt beregningsaktiviteter"),
    OPPDATERT_MED_ANDELER("OPPDATERT_MED_ANDELER", "Oppdatert med andeler"),
    KOFAKBER_UT("KOFAKBER_UT", "Kontroller fakta beregningsgrunnlag - Ut"),
    FORESLÅTT("FORESLÅTT", "Foreslått"),
    FORESLÅTT_UT("FORESLÅTT_UT", "Foreslått ut"),
    FORTS_FORESLÅTT("FORESLÅTT_DEL_2", "Fortsettelse Foreslått"),
    FORTS_FORESLÅTT_UT("FORESLÅTT_DEL_2_UT", "Fortsettelse Foreslått"),
    VURDERT_VILKÅR("VURDERT_VILKÅR", "Vurder vilkår"),
    VURDERT_TILKOMMET_INNTEKT("VURDERT_TILKOMMET_INNTEKT", "Vurder tilkommet inntekt"),
    VURDERT_TILKOMMET_INNTEKT_UT("VURDERT_TILKOMMET_INNTEKT_UT", "Vurder tilkommet inntekt - UT"),
    VURDERT_REFUSJON("VURDERT_REFUSJON", "Vurder refusjonskrav beregning"),
    VURDERT_REFUSJON_UT("VURDERT_REFUSJON_UT", "Vurder refusjonskrav beregning - Ut"),
    OPPDATERT_MED_REFUSJON_OG_GRADERING("OPPDATERT_MED_REFUSJON_OG_GRADERING", "Tilstand for splittet periode med refusjon og gradering"),
    FASTSATT_INN("FASTSATT_INN", "Fastsatt - Inn"),
    FASTSATT("FASTSATT", "Fastsatt"),
    FORS_BERGRUNN_INN("FORS_BERGRUNN_INN", "Foreslå beregningsgrunnlag - Inngang"),
    UDEFINERT("-", "Ikke definert"),
    ;
    public static final String KODEVERK = "BEREGNINGSGRUNNLAG_TILSTAND";

    private static final Map<String, BeregningsgrunnlagTilstand> KODER = new LinkedHashMap<>();

    private static final List<BeregningsgrunnlagTilstand> tilstandRekkefølge = Collections.unmodifiableList(
        List.of(
            OPPRETTET,
            FASTSATT_BEREGNINGSAKTIVITETER,
            OPPDATERT_MED_ANDELER,
            KOFAKBER_UT,
            FORESLÅTT,
            FORESLÅTT_UT,
            VURDERT_VILKÅR,
            VURDERT_REFUSJON,
            VURDERT_REFUSJON_UT,
            OPPDATERT_MED_REFUSJON_OG_GRADERING,
            FASTSATT_INN,
            FASTSATT
        ));

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

    BeregningsgrunnlagTilstand(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static BeregningsgrunnlagTilstand fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(BeregningsgrunnlagTilstand.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BeregningsgrunnlagTilstand: " + kode);
        }
        return ad;
    }

    public static Map<String, BeregningsgrunnlagTilstand> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public boolean erFør(BeregningsgrunnlagTilstand that) {
        int thisIndex = tilstandRekkefølge.indexOf(this);
        int thatIndex = tilstandRekkefølge.indexOf(that);
        return thisIndex < thatIndex;
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
}
