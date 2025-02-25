package no.nav.ung.kodeverk.behandling.aksjonspunkt;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import no.nav.ung.kodeverk.vilkår.VilkårType;

public class AksjonspunktKodeDefinisjon {

    // Aksjonspunkt Nr

    public static final String AUTO_MANUELT_SATT_PÅ_VENT_KODE = "7001";
    public static final String AUTO_VENTER_PÅ_KOMPLETT_SØKNAD_KODE = "7003";
    public static final String AUTO_SATT_PÅ_VENT_REVURDERING_KODE = "7005";
    public static final String AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER_KODE = "7006";
    public static final String AUTO_VENT_KOMPLETT_OPPDATERING_KODE = "7009";
    public static final String AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST_KODE = "7014";
    public static final String AUTO_VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG_KODE = "7019";
    public static final String AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT_KODE = "7020";
    public static final String AUTO_VENT_INNTEKTSMELDING_MED_UGYLDIG_ARBEIDSFORHOLDID_KODE = "7022";
    public static final String AUTO_VENT_MILITÆR_OG_BG_UNDER_3G_KODE = "7023";
    public static final String AUTO_VENT_GRADERING_FLERE_ARBEIDSFORHOLD_KODE = "7025";
    public static final String AUTO_VENT_ETTERLYST_INNTEKTSMELDING_KODE = "7030";
    public static final String AUTO_VENT_PÅ_LOVENDRING_8_41_KODE = "7041";

    public static final String AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE_KODE = "5021";
    public static final String AVKLAR_LOVLIG_OPPHOLD_KODE = "5019";
    public static final String AVKLAR_OM_ER_BOSATT_KODE = "5020";
    public static final String AVKLAR_OPPHOLDSRETT_KODE = "5023";
    public static final String AVKLAR_TILLEGGSOPPLYSNINGER_KODE = "5009";
    public static final String AVKLAR_VERGE_KODE = "5030";
    public static final String AVKLAR_FORTSATT_MEDLEMSKAP_KODE = "5053";

    public static final String FATTER_VEDTAK_KODE = "5016";

    public static final String FORESLÅ_VEDTAK_KODE = "5015";
    public static final String FORESLÅ_VEDTAK_MANUELT_KODE = "5028";

    public static final String SØKERS_OPPLYSNINGSPLIKT_OVST_KODE = "6002";
    public static final String OVERSTYRING_AV_OMSORGENFOR_KODE = "6003";
    public static final String OVERSTYRING_AV_MEDISINSKVILKÅR_UNDER_18_KODE = "6004";
    public static final String OVERSTYRING_AV_MEDISINSKVILKÅR_OVER_18_KODE = "6008";
    public static final String OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_KODE = "6005";
    public static final String OVERSTYRING_AV_SØKNADSFRISTVILKÅRET_KODE = "6006";
    public static final String OVERSTYRING_AV_BEREGNING_KODE = "6007";
    public static final String OVERSTYRING_AV_OPPTJENINGSVILKÅRET_KODE = "6011";
    public static final String OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE = "6014";
    public static final String OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE = "6015";
    public static final String OVERSTYRING_AV_K9_VILKÅRET_KODE = "6016";
    public static final String OVERSTYRING_AV_UTTAK_KODE = "6017";
    public static final String MANUELL_MARKERING_AV_UTLAND_SAKSTYPE_KODE = "6068";

    public static final String SØKERS_OPPLYSNINGSPLIKT_MANU_KODE = "5017";

    public static final String VARSEL_REVURDERING_ETTERKONTROLL_KODE = "5025";
    public static final String VARSEL_REVURDERING_MANUELL_KODE = "5026";
    public static final String KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST_KODE = "5055";
    public static final String KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING_KODE = "5056";
    public static final String MANUELL_TILKJENT_YTELSE_KODE = "5057";

    public static final String VEDTAK_UTEN_TOTRINNSKONTROLL_KODE = "5018";

    public static final String VENT_PGA_FOR_TIDLIG_SØKNAD_KODE = "7008";

    public static final String VURDERE_ANNEN_YTELSE_FØR_VEDTAK_KODE = "5033";
    public static final String VURDERE_DOKUMENT_FØR_VEDTAK_KODE = "5034";
    public static final String VURDERE_OVERLAPPENDE_YTELSER_FØR_VEDTAK_KODE = "5040";

    public static final String FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE = "5038";
    public static final String FORDEL_BEREGNINGSGRUNNLAG_KODE = "5046";
    public static final String VURDER_NYTT_INNTEKTSFORHOLD_KODE = "5067";
    public static final String FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE = "5047";
    public static final String FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE = "5049";
    public static final String VURDER_GRADERING_UTEN_BEREGNINGSGRUNNLAG_KODE = "5050";
    public static final String AVKLAR_AKTIVITETER_KODE = "5052";
    public static final String VURDER_REPRESENTERER_STORTINGET_KODE = "5087";

    public static final String VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE = "5039";
    public static final String VURDER_VARIG_ENDRET_ARBEIDSSITUASJON_KODE = "5054";
    public static final String VURDER_FAKTA_FOR_ATFL_SN_KODE = "5058";
    public static final String VURDER_REFUSJON_BERGRUNN_KODE = "5059";

    public static final String FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE_KODE = "5042";

    public static final String TILKNYTTET_STORTINGET_KODE = "5072";

    public static final String KONTROLLER_OPPLYSNINGER_OM_DØD_KODE = "5076";
    public static final String KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE = "5077";
    public static final String KONTROLLER_TILSTØTENDE_YTELSER_INNVILGET_KODE = "5078";
    public static final String KONTROLLER_TILSTØTENDE_YTELSER_OPPHØRT_KODE = "5079";

    public static final String VURDER_PERIODER_MED_OPPTJENING_KODE = "5051";
    public static final String VURDER_ARBEIDSFORHOLD_KODE = "5080";
    public static final String VURDER_FEILUTBETALING_KODE = "5084";
    public static final String SJEKK_TILBAKEKREVING_KODE = "5085";
    public static final String VURDER_OPPTJENINGSVILKÅRET_KODE = "5089";

    public static final String AUTOMATISK_MARKERING_AV_UTENLANDSSAK_KODE = "5068";
    public static final String VURDER_TILBAKETREKK_KODE = "5090";

    /** p.t. ikke i bruk i K9 */
    @Deprecated
    public static final String VURDER_FARESIGNALER_KODE = "5095";

    public static final String AUTO_VENT_BRUKER_70_ÅR = "7035";

    public static final String AVKLAR_OMSORGEN_FOR_KODE_V2 = "9020";


    // PSB
    public static final String KONTROLLER_LEGEERKLÆRING_KODE = "9001";
    public static final String VURDER_NATTEVÅK = "9200";
    public static final String VURDER_BEREDSKAP = "9201";
    public static final String VURDER_RETT_ETTER_PLEIETRENGENDES_DØD = "9202";
    public static final String MANGLER_AKTIVITETER = "9203";
    public static final String VENT_ANNEN_PSB_SAK_KODE = "9290";
    public static final String VURDER_DATO_NY_REGEL_UTTAK = "9291";
    public static final String VURDER_OVERLAPPENDE_SØSKENSAK_KODE = "9292";
    public static final String OVERSTYR_BEREGNING_INPUT = "9005";
    public static final String AUTO_VENT_PÅ_KOMPLETT_SØKNAD_FOR_PERIODE = "9006";
    public static final String TRENGER_SØKNAD_FOR_INFOTRYGD_PERIODE = "9007";
    public static final String TRENGER_SØKNAD_FOR_INFOTRYGD_PERIODE_ANNEN_PART = "9008";


    // OLP
    public static final String VURDER_INSTITUSJON = "9300";
    public static final String VURDER_NØDVENDIGHET = "9301";
    public static final String VURDER_GJENNOMGÅTT_OPPLÆRING = "9302";
    public static final String VURDER_REISETID = "9303";

    // OMS
    public static final String AVKLAR_OMSORGEN_FOR_KODE = "9002";
    public static final String VURDER_ÅRSKVANTUM_KVOTE = "9003";
    public static final String VURDER_ÅRSKVANTUM_DOK = "9004";
    public static final String VURDER_OMS_UTVIDET_RETT = "9013";
    public static final String ÅRSKVANTUM_FOSTERBARN = "9014";
    public static final String VURDER_ALDERSVILKÅR_BARN = "9015";

    // FRISINN
    public static final String AUTO_VENT_FRISINN_BEREGNING = "8000";
    public static final String AUTO_VENT_FRISINN_MANGLENDE_FUNKSJONALITET = "8003";
    public static final String OVERSTYRING_FRISINN_OPPGITT_OPPTJENING_KODE = "8004";
    public static final String AUTO_VENT_FRISINN_ATFL_SAMME_ORG_KODE = "8005";

    // Generelt manglende funksjonalitet.
    public static final String AUTO_VENT_FILTER_MANGLENDE_FUNKSJONALITET = "9999";
    public static final String AUTO_VENTE_PA_OMSORGENFOR_OMS = "9099";

    // Kompletthet for beregning
    public static final String AVKLAR_KOMPLETT_NOK_FOR_BEREGNING_KODE = "9069";
    public static final String ENDELING_AVKLAR_KOMPLETT_NOK_FOR_BEREGNING_KODE = "9071";
    public static final String ETTERLYS_IM_FOR_BEREGNING_KODE = "9068";
    public static final String ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING_KODE = "9070";

    static final Map<String, String> KODER;

    static {
        // lag liste av alle koder definert, brukes til konsistensjsekk mot AksjonspunktDefinisjon i tilfelle ubrukte/utgåtte koder.
        var cls = AksjonspunktKodeDefinisjon.class;
        var map = new TreeMap<String, String>();
        Arrays.stream(cls.getDeclaredFields())
            .filter(f -> Modifier.isPublic(f.getModifiers()) && f.getType() == String.class && Modifier.isStatic(f.getModifiers()))
            .filter(f -> getValue(f) != null)
            .forEach(f -> {
                var kode = getValue(f);
                // sjekker duplikat kode definisjon
                if (map.putIfAbsent(kode, f.getName()) != null) {
                    throw new IllegalStateException("Duplikat kode for : " + kode);
                }
            });
        KODER = Collections.unmodifiableMap(map);
    }

    private static String getValue(Field f) {
        try {
            return (String) f.get(AksjonspunktKodeDefinisjon.class);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Andre koder, brukes til definisjon av {@link AksjonspunktDefinisjon}. */
    public static final SkjermlenkeType UTEN_SKJERMLENKE = null;
    public static final VilkårType UTEN_VILKÅR = null;
    public static final String UTEN_FRIST = null;
    public static final boolean TOTRINN = true;
    public static final boolean ENTRINN = false;

    public static final boolean KAN_OVERSTYRE_TOTRINN_ETTER_LUKKING = true;
    public static final boolean KAN_IKKE_OVERSTYRE_TOTRINN_ETTER_LUKKING = false;
    public static final boolean TILBAKE = true;
    public static final boolean SKAL_IKKE_AVBRYTES = false;
    public static final boolean AVBRYTES = true;
    public static final boolean FORBLI = false;

    public static void main(String[] args) {
        KODER.entrySet().stream().forEach(e -> System.out.println(e));
    }

}
