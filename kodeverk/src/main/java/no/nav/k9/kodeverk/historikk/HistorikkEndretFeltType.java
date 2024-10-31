package no.nav.k9.kodeverk.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
    ENDRING_NÆRING("ENDRING_NAERING", "Endring i næringsvirksomheten"),
    BRUTTO_NAERINGSINNTEKT("BRUTTO_NAERINGSINNTEKT", "Brutto næringsinntekt"),
    KOMPLETTHET("KOMPLETTHET", "Avklar inntektsgrunnlag for skjæringstidspunkt"),
    NY_STARTDATO_REFUSJON("NY_STARTDATO_REFUSJON", "Ny startdato for refusjon"),
    VURDER_NYTT_INNTEKTSFORHOLD("VURDER_NYTT_INNTEKTSFORHOLD", "Skal nytt inntektsforhold redusere utbetaling"),
    BRUTTO_INNTEKT_NYTT_INNTEKTSFORHOLD("BRUTTO_INNTEKT_NYTT_INNTEKTSFORHOLD", "Brutto inntekt for nytt inntektsforhold som skal redusere utbetaling"),

    DELVIS_REFUSJON_FØR_STARTDATO("DELVIS_REFUSJON_FØR_STARTDATO", "Delvis refusjon før startdato"),
    PERIODE_TOM("PERIODE_TOM", "Periode t.o.m."),

    /** Medlemskap og utenlandstilsnitt. */
    ER_SOKER_BOSATT_I_NORGE("ER_SOKER_BOSATT_I_NORGE", "Er søker bosatt i Norge?"),
    GYLDIG_MEDLEM_FOLKETRYGDEN("GYLDIG_MEDLEM_FOLKETRYGDEN", "Gyldig medlem i folketrygden"),
    OPPHOLDSRETT_EOS("OPPHOLDSRETT_EOS", "Bruker har oppholdsrett i EØS"),
    OPPHOLDSRETT_IKKE_EOS("OPPHOLDSRETT_IKKE_EOS", "Bruker har ikke oppholdsrett i EØS"),
    UTLAND("UTLAND", "Utland"),

    /** Uttak */
    UTTAK_OVERSTYRT_PERIODE("UTTAK_OVERSTYRT_PERIODE", "Aktuell uttaksperiode"),
    UTTAK_OVERSTYRT_SØKERS_UTTAKSGRAD("UTTAK_OVERSTYRT_SØKERS_UTTAKSGRAD", "Søkers uttaksgrad"),
    UTTAK_OVERSTYRT_UTBETALINGSGRAD("UTTAK_OVERSTYRT_UTBETALINGSGRAD", "Utbetalingsgrad"),
    OVST_UTTAK_FJERNET("OVST_UTTAK_FJERNET", "Overstyrt uttak er fjernet"),


    /** Tilkjent ytelse */
    TILKJENT_YTELSE("TILKJENT_YTELSE", "Tilkjent ytelse"),

    /** Tilbakekreving. */
    ER_SÆRLIGE_GRUNNER_TIL_REDUKSJON("ER_SÆRLIGE_GRUNNER_TIL_REDUKSJON", "Er det særlige grunner til reduksjon"),
    ER_VILKÅRENE_TILBAKEKREVING_OPPFYLT("ER_VILKÅRENE_TILBAKEKREVING_OPPFYLT", "Er vilkårene for tilbakekreving oppfylt"),
    FASTSETT_VIDERE_BEHANDLING("FASTSETT_VIDERE_BEHANDLING", "Fastsett videre behandling"),
    VIRKNINGSDATO_UTTAK_NYE_REGLER("VIRKNINGSDATO_UTTAK_NYE_REGLER", "Virkningsdato"),
    TILBAKETREKK("TILBAKETREKK", "Tilbaketrekk"),

    /** Vilkår */
    OMSORG_FOR("OMSORG_FOR", "Har omsorgen for"),

    /** Faresignaler. */
    FARESIGNALER("FARESIGNALER", "Faresignaler"),

    /** Refusjonskrav. */
    NYTT_REFUSJONSKRAV("NYTT_REFUSJONSKRAV", "Nytt refusjonskrav"),
    NY_REFUSJONSFRIST("NY_REFUSJONSFRIST", "Ny refusjonsfrist"),
    OPPHØR_REFUSJON("OPPHOER_REFUSJON", "Dato for opphør av refusjon"),

    /** Rammevedtak */
    UTVIDETRETT("UTVIDETRETT", "Utvidet rett"),
    ALENE_OM_OMSORG("ALENE_OM_OMSORG", "Alene om omsorgen"),
    MIDLERTIDIG_ALENE("MIDLERTIDIG_ALENE", "Utvidet rett"),
    ALDERSVILKÅR_BARN("ALDERSVILKAR_BARN", "Aldersvilkår barn"),

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

    @JsonCreator
    public static HistorikkEndretFeltType fraKode(String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkEndretFeltType: " + kode);
        }
        return ad;
    }

    @JsonCreator
    public static HistorikkEndretFeltType fraObjektProp(@JsonProperty("kode") String kode) {
        return fraKode(kode);
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
