package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum HistorikkEndretFeltVerdiType implements Kodeverdi {

    ABNR("ABNR", "Aktivt BOSTNR"),
    ADNR("ADNR", "Aktivt"),
    ADOPTERER_ALENE("ADOPTERER_ALENE", "adopterer alene"),
    ADOPTERER_IKKE_ALENE("ADOPTERER_IKKE_ALENE", "adopterer ikke alene"),
    ADVOKAT("ADVOKAT", "Advokat/advokatfullmektig"),
    ALENEOMSORG("ALENEOMSORG", "Søker har aleneomsorg for barnet"),
    ANNEN_F("ANNEN_F", "Annen fullmektig"),
    ANNEN_FORELDER_HAR_IKKE_RETT("ANNEN_FORELDER_HAR_IKKE_RETT", "Annen forelder har ikke rett"),
    ANNEN_FORELDER_HAR_RETT("ANNEN_FORELDER_HAR_RETT", "Annen forelder har rett"),
    ARBEIDSAVKLARINGSPENGER("ARBEIDSAVKLARINGSPENGER", "Arbeidsavklaringspenger"),
    ARBEIDSTAKER("ARBEIDSTAKER", "Arbeidstaker"),
    ARBEIDSTAKER_UTEN_FERIEPENGER("ARBEIDSTAKER_UTEN_FERIEPENGER", "Arbeidstaker uten feriepenger"),
    AVSLÅTT("AVSLÅTT", "Ikke oppfylt"),
    BARN("BARN", "Verge for barn under 18 år"),
    BEGGE("BEGGE", "Søker og verge/fullmektig"),
    BENYTT("BENYTT", "Benytt"),
    BENYTT_A_INNTEKT_I_BG("BENYTT_A_INNTEKT_I_BG", "Benytt i behandlingen. Inntekt fra A-inntekt benyttes i beregningsgrunnlaget"),
    BOSA("BOSA", "Bosatt"),
    BOSATT_I_NORGE("BOSATT_I_NORGE", "Søker er bosatt i Norge"),
    BOSATT_UTLAND("BOSATT_UTLAND", "Bosatt utland"),
    BRUK_MED_OVERSTYRTE_PERIODER("BRUK_MED_OVERSTYRTE_PERIODER", "Bruk arbeidsforholdet med overstyrt periode"),
    DAGMAMMA("DAGMAMMA", "Selvstendig næringsdrivende - Dagmamma"),
    DAGPENGER("DAGPENGER", "Dagpenger"),
    DOKUMENTERT("DOKUMENTERT", "dokumentert"),
    DØD("DØD", "Død"),
    EKTEFELLES_BARN("EKTEFELLES_BARN", "ektefelles barn"),
    EØS_BOSATT_NORGE("EØS_BOSATT_NORGE", "EØS bosatt Norge"),
    FASTSETT_RESULTAT_ENDRE_SOEKNADSPERIODEN("FASTSETT_RESULTAT_ENDRE_SOEKNADSPERIODEN", "Endre søknadsperioden"),
    FASTSETT_RESULTAT_GRADERING_AVKLARES("FASTSETT_RESULTAT_GRADERING_AVKLARES", "Perioden er ok"),
    FASTSETT_RESULTAT_PERIODEN_AVKLARES_IKKE("FASTSETT_RESULTAT_PERIODEN_AVKLARES_IKKE", "Perioden kan ikke avklares"),
    FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT("FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT",
        "Innleggelsen er dokumentert, angi avklart periode"),
    FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT_IKKE("FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT_IKKE", "Innleggelsen er ikke dokumentert"),
    FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT("FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT", "Sykdommen/skaden er dokumentert, angi avklart periode"),
    FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT_IKKE("FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT_IKKE", "Sykdommen/skaden er ikke dokumentert"),
    FASTSETT_RESULTAT_UTSETTELSE_AVKLARES("FASTSETT_RESULTAT_UTSETTELSE_AVKLARES", "Perioden er ok"),
    FBARN("FBARN", "Verge for foreldreløst barn under 18 år"),
    FEDREKVOTE("FEDREKVOTE", "Fedrekvote"),
    FELLESPERIODE("FELLESPERIODE", "Fellesperiode"),
    FISKER("FISKER", "Selvstendig næringsdrivende - Fisker"),
    FORELDREANSVAR_2_TITTEL("FORELDREANSVAR_2_TITTEL", "Foreldreansvarsvilkåret §14-17 andre ledd"),
    FORELDREANSVAR_4_TITTEL("FORELDREANSVAR_4_TITTEL", "Foreldreansvarsvilkåret §14-17 fjerde ledd"),
    FORELDREPENGER("FORELDREPENGER", "Foreldrepenger"),
    FORELDREPENGER_FØR_FØDSEL("FORELDREPENGER_FØR_FØDSEL", "Foreldrepenger før fødsel"),
    FORTSETT_BEHANDLING("FORTSETT_BEHANDLING", "Fortsett behandling"),
    FOSV("FOSV", "Forsvunnet/savnet"),
    FRILANSER("FRILANSER", "Frilanser"),
    FØDR("FØDR", "Fødselsregistrert"),
    GRADERING_IKKE_OPPFYLT("GRADERING_IKKE_OPPFYLT", "Ikke oppfylt"),
    GRADERING_OPPFYLT("GRADERING_OPPFYLT", "Oppfylt"),
    GRADERING_PÅ_ANDEL_UTEN_BG_IKKE_SATT_PÅ_VENT("GRADERING_PÅ_ANDEL_UTEN_BG_IKKE_SATT_PÅ_VENT", "Riktig"),
    HAR_GYLDIG_GRUNN("HAR_GYLDIG_GRUNN", "Gyldig grunn for sen fremsetting av søknaden"),
    HAR_IKKE_GYLDIG_GRUNN("HAR_IKKE_GYLDIG_GRUNN", "Ingen gyldig grunn for sen fremsetting av søknaden"),
    HENLEGG_BEHANDLING("HENLEGG_BEHANDLING", "Henlegg behandling"),
    HINDRE_TILBAKETREKK("HINDRE_TILBAKETREKK", "Ikke tilbakekrev fra søker"),
    IKKE_ALENEOMSORG("IKKE_ALENEOMSORG", "Søker har ikke aleneomsorg for barnet"),
    IKKE_BENYTT("IKKE_BENYTT", "Ikke benytt"),
    IKKE_BOSATT_I_NORGE("IKKE_BOSATT_I_NORGE", "Søker er ikke bosatt i Norge"),
    IKKE_BRUK("IKKE_BRUK", "Ikke bruk"),
    IKKE_DOKUMENTERT("IKKE_DOKUMENTERT", "ikke dokumentert"),
    IKKE_EKTEFELLES_BARN("IKKE_EKTEFELLES_BARN", "ikke ektefelles barn"),
    IKKE_FASTSATT("IKKE_FASTSATT", "ikke fastsatt"),
    IKKE_LOVLIG_OPPHOLD("IKKE_LOVLIG_OPPHOLD", "Søker har ikke lovlig opphold"),
    IKKE_NYOPPSTARTET("IKKE_NYOPPSTARTET", "ikke nyoppstartet"),
    IKKE_NY_I_ARBEIDSLIVET("IKKE_NY_I_ARBEIDSLIVET", "til ikke ny i arbeidslivet"),
    IKKE_OMSORG_FOR_BARNET("IKKE_OMSORG_FOR_BARNET", "Søker har ikke omsorg for barnet"),
    IKKE_OPPFYLT("IKKE_OPPFYLT", "ikke oppfylt"),
    IKKE_OPPHOLDSRETT("IKKE_OPPHOLDSRETT", "Søker har ikke oppholdsrett"),
    IKKE_RELEVANT_PERIODE("IKKE_RELEVANT_PERIODE", "Ikke relevant periode"),
    IKKE_TIDSBEGRENSET_ARBEIDSFORHOLD("IKKE_TIDSBEGRENSET_ARBEIDSFORHOLD", "ikke tidsbegrenset"),
    INGEN_INNVIRKNING("INGEN_INNVIRKNING", "Faresignalene hadde ingen innvirkning på behandlingen"),
    INGEN_VARIG_ENDRING_NAERING("INGEN_VARIG_ENDRING_NAERING", "Ingen varig endret eller nyoppstartet næring"),
    INNTEKT_IKKE_MED_I_BG("INNTEKT_IKKE_MED_I_BG", "Benytt i behandligen. Inntekten er ikke med i beregningsgrunnlaget"),
    INNVILGET("INNVILGET", "Oppfylt"),
    INNVIRKNING("INNVIRKNING", "Faresignalene hadde innvirkning på behandlingen"),
    JORDBRUKER("JORDBRUKER", "Selvstendig næringsdrivende - Jordbruker"),
    LAGT_TIL_AV_SAKSBEHANDLER("LAGT_TIL_AV_SAKSBEHANDLER", "Arbeidsforholdet er lagt til av saksbehandler"),
    LOVLIG_OPPHOLD("LOVLIG_OPPHOLD", "Søker har lovlig opphold"),
    MANGLENDE_OPPLYSNINGER("MANGLENDE_OPPLYSNINGER", "Benytt i behandlingen, men har manglende opplysninger"),
    MØDREKVOTE("MØDREKVOTE", "Mødrekvote"),
    NASJONAL("NASJONAL", "Nasjonal"),
    NYOPPSTARTET("NYOPPSTARTET", "nyoppstartet"),
    NYTT_ARBEIDSFORHOLD("NYTT_ARBEIDSFORHOLD", "Arbeidsforholdet er ansett som nytt"),
    NY_I_ARBEIDSLIVET("NY_I_ARBEIDSLIVET", "ny i arbeidslivet"),
    OMSORGSVILKARET_TITTEL("OMSORGSVILKARET_TITTEL", "Omsorgsvilkår §14-17 tredje ledd"),
    OMSORG_FOR_BARNET("OMSORG_FOR_BARNET", "Søker har omsorg for barnet"),
    OPPFYLT("OPPFYLT", "oppfylt"),
    OPPHOLDSRETT("OPPHOLDSRETT", "Søker har oppholdsrett"),
    PERIODE_MEDLEM("PERIODE_MEDLEM", "Periode med medlemskap"),
    PERIODE_UNNTAK("PERIODE_UNNTAK", "Perioder uten medlemskap"),
    SELVSTENDIG_NÆRINGSDRIVENDE("SELVSTENDIG_NÆRINGSDRIVENDE", "Selvstendig næringsdrivende"),
    SJØMANN("SJØMANN", "Arbeidstaker - Sjømann"),
    SLÅTT_SAMMEN_MED_ANNET("SLÅTT_SAMMEN_MED_ANNET", "Arbeidsforholdet er slått sammen med annet"),
    SOEKER("SOEKER", "Søker"),
    SØKER_ER_IKKE_I_PERMISJON("SØKER_ER_IKKE_I_PERMISJON", "Søker er ikke i permisjon"),
    SØKER_ER_I_PERMISJON("SØKER_ER_I_PERMISJON", "Søker er i permisjon"),
    TIDSBEGRENSET_ARBEIDSFORHOLD("TIDSBEGRENSET_ARBEIDSFORHOLD", "tidsbegrenset arbeidsforhold"),
    TILBAKEKR_IGNORER("TILBAKEKR_IGNORER", "Avvent samordning, ingen tilbakekreving"),
    TILBAKEKR_INFOTRYGD("TILBAKEKR_INFOTRYGD", "Behandle tilbakekreving i infotrygd"),
    UDEFINIERT("-", "Ikke definert"),
    UFUL("UFUL", "Ufullstendig fødselsnr"),
    UREG("UREG", "Uregistrert person"),
    UTAN("UTAN", "Utgått person annullert tilgang Fnr"),
    UTFØR_TILBAKETREKK("UTFØR_TILBAKETREKK", "Tilbakekrev fra søker"),
    UTPE("UTPE", "Utgått person"),
    UTVA("UTVA", "Utvandret"),
    VARIG_ENDRET_NAERING("VARIG_ENDRET_NAERING", "Varig endret eller nystartet næring"),
    VERGE("VERGE", "Verge/fullmektig"),
    VILKAR_IKKE_OPPFYLT("VILKAR_IKKE_OPPFYLT", "Vilkåret er ikke oppfylt"),
    VILKAR_OPPFYLT("VILKAR_OPPFYLT", "Vilkåret er oppfylt"),
    VOKSEN("VOKSEN", "Verge for voksen"),
    ;

    private static final Map<String, HistorikkEndretFeltVerdiType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_ENDRET_FELT_VERDI_TYPE";

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

    private HistorikkEndretFeltVerdiType(String kode) {
        this.kode = kode;
    }

    private HistorikkEndretFeltVerdiType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static HistorikkEndretFeltVerdiType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent HistorikkEndretFeltVerdiType: " + kode);
        }
        return ad;
    }

    public static Map<String, HistorikkEndretFeltVerdiType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
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
        return getKode();
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<HistorikkEndretFeltVerdiType, String> {
        @Override
        public String convertToDatabaseColumn(HistorikkEndretFeltVerdiType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public HistorikkEndretFeltVerdiType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
