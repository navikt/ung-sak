package no.nav.k9.kodeverk.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
public enum HistorikkEndretFeltType implements Kodeverdi {

    /** Opptjening. */
    AKTIVITET("AKTIVITET", "Aktivitet"),
    AKTIVITET_PERIODE("AKTIVITET_PERIODE", "Perioden med aktivitet"),
    OPPTJENINGSVILKARET("OPPTJENINGSVILKARET", "Opptjeningsvilkåret"),

    /** Arbeidsforhold og inntekt. */
    ARBEIDSFORHOLD("ARBEIDSFORHOLD", "Arbeidsforhold"),
    DAGPENGER_INNTEKT("DAGPENGER_INNTEKT", "Dagpenger"),
    FASTSETT_ETTERLØNN_SLUTTPAKKE("FASTSETT_ETTERLØNN_SLUTTPAKKE", "Fastsett søkers månedsinntekt fra etterlønn eller sluttpakke"),

    /** Søknad og behandling. */
    AVKLARSAKSOPPLYSNINGER("AVKLARSAKSOPPLYSNINGER", "Avklar saksopplysninger"),
    BEHANDLENDE_ENHET("BEHANDLENDE_ENHET", "Behandlende enhet"),
    BEHANDLING("BEHANDLING", "Behandling"),
    OVERSTYRT_VURDERING("OVERSTYRT_VURDERING", "Overstyrt vurdering"),
    SOKERSOPPLYSNINGSPLIKT("SOKERSOPPLYSNINGSPLIKT", "Søkers opplysningsplikt"),
    SOKNADSFRISTVILKARET("SOKNADSFRISTVILKARET", "Søknadsfristvilkåret"),

    /** Beregning og inntektshistorikk. */
    ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD("ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD", "Endring tidsbegrenset arbeidsforhold"),
    FRILANSVIRKSOMHET("FRILANSVIRKSOMHET", "Frilansvirksomhet"),
    FRILANS_INNTEKT("FRILANS_INNTEKT", "Frilans inntekt"),
    INNTEKTSKATEGORI("INNTEKTSKATEGORI", "Inntektskategori"),
    INNTEKT_FRA_ARBEIDSFORHOLD("INNTEKT_FRA_ARBEIDSFORHOLD", "Inntekt fra arbeidsforhold"),
    LØNNSENDRING_I_PERIODEN("LØNNSENDRING_I_PERIODEN", "Lønnsendring i beregningsperioden"),
    MILITÆR_ELLER_SIVIL("MILITÆR_ELLER_SIVIL", "Militær- eller siviltjeneste"),
    MOTTAR_YTELSE_ARBEID("MOTTAR_YTELSE_ARBEID", "Mottar søker ytelse for arbeid i {value}"),
    MOTTAR_YTELSE_FRILANS("MOTTAR_YTELSE_FRILANS", "Mottar søker ytelse for frilansaktiviteten"),
    SELVSTENDIG_NÆRINGSDRIVENDE("SELVSTENDIG_NAERINGSDRIVENDE", "Selvstendig næringsdrivende"),
    VURDER_ETTERLØNN_SLUTTPAKKE("VURDER_ETTERLØNN_SLUTTPAKKE", "Har søker inntekt fra etterlønn eller sluttpakke"),

    /** Medlemskap og utenlandstilsnitt. */
    ER_SOKER_BOSATT_I_NORGE("ER_SOKER_BOSATT_I_NORGE", "Er søker bosatt i Norge?"),
    GYLDIG_MEDLEM_FOLKETRYGDEN("GYLDIG_MEDLEM_FOLKETRYGDEN", "Gyldig medlem i folketrygden"),
    OPPHOLDSRETT_EOS("OPPHOLDSRETT_EOS", "Bruker har oppholdsrett i EØS"),
    OPPHOLDSRETT_IKKE_EOS("OPPHOLDSRETT_IKKE_EOS", "Bruker har ikke oppholdsrett i EØS"),
    UTLAND("UTLAND", "Utland"),

    /** Tilkjent ytelse */
    TILKJENT_YTELSE("TILKJENT_YTELSE", "Tilkjent ytelse"),

    /** Tilbakekreving. */
    ER_SÆRLIGE_GRUNNER_TIL_REDUKSJON("ER_SÆRLIGE_GRUNNER_TIL_REDUKSJON", "Er det særlige grunner til reduksjon"),
    ER_VILKÅRENE_TILBAKEKREVING_OPPFYLT("ER_VILKÅRENE_TILBAKEKREVING_OPPFYLT", "Er vilkårene for tilbakekreving oppfylt"),
    FASTSETT_VIDERE_BEHANDLING("FASTSETT_VIDERE_BEHANDLING", "Fastsett videre behandling"),
    TILBAKETREKK("TILBAKETREKK", "Tilbaketrekk"),

    /** Faresignaler. */
    FARESIGNALER("FARESIGNALER", "Faresignaler"),

    /** Refusjonskrav. */
    NYTT_REFUSJONSKRAV("NYTT_REFUSJONSKRAV", "Nytt refusjonskrav"),
    NY_REFUSJONSFRIST("NY_REFUSJONSFRIST", "Ny refusjonsfrist"),

    VALG("VALG", "Valg"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, HistorikkEndretFeltType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_ENDRET_FELT_TYPE";

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

    private HistorikkEndretFeltType(String kode) {
        this.kode = kode;
    }

    private HistorikkEndretFeltType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static HistorikkEndretFeltType  fraKode(Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(HistorikkEndretFeltType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkEndretFeltType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkEndretFeltType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
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

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }
}
