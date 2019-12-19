package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.ENTRINN;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.FORBLI;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.TILBAKE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.TOTRINN;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_FRIST;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_SKJERMLENKE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_VILKÅR;
import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType.ES;
import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType.FP;
import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType.SVP;

import java.time.Period;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

/**
 * Definerer mulige Aksjonspunkter inkludert hvilket Vurderingspunkt de må løses i.
 * Inkluderer også konstanter for å enklere kunne referere til dem i eksisterende logikk.
 */
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum AksjonspunktDefinisjon implements Kodeverdi {

    // Gruppe : 500
    AVKLAR_TILLEGGSOPPLYSNINGER(
            AksjonspunktKodeDefinisjon.AVKLAR_TILLEGGSOPPLYSNINGER_KODE, AksjonspunktType.MANUELL, "Avklar tilleggsopplysninger",
            BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    FORESLÅ_VEDTAK(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_KODE,
            AksjonspunktType.MANUELL, "Vurder om ytelse allerede er innvilget", BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.VEDTAK, ENTRINN, EnumSet.of(ES, FP, SVP)),
    FATTER_VEDTAK(AksjonspunktKodeDefinisjon.FATTER_VEDTAK_KODE,
            AksjonspunktType.MANUELL, "Fatter vedtak", BehandlingStegType.FATTE_VEDTAK, VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.VEDTAK, ENTRINN,
            EnumSet.of(ES, FP, SVP)),
    SØKERS_OPPLYSNINGSPLIKT_MANU(
            AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU_KODE, AksjonspunktType.MANUELL,
            "Vurder søkers opplysningsplikt ved ufullstendig/ikke-komplett søknad", BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT,
            VurderingspunktType.UT, VilkårType.SØKERSOPPLYSNINGSPLIKT, SkjermlenkeType.OPPLYSNINGSPLIKT, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VEDTAK_UTEN_TOTRINNSKONTROLL(
            AksjonspunktKodeDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL_KODE, AksjonspunktType.MANUELL, "Foreslå vedtak uten totrinnskontroll",
            BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_LOVLIG_OPPHOLD(AksjonspunktKodeDefinisjon.AVKLAR_LOVLIG_OPPHOLD_KODE,
            AksjonspunktType.MANUELL, "Avklar lovlig opphold.", BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN,
            VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_OM_ER_BOSATT(AksjonspunktKodeDefinisjon.AVKLAR_OM_ER_BOSATT_KODE,
            AksjonspunktType.MANUELL, "Avklar om bruker er bosatt.", BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN,
            VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE(
            AksjonspunktKodeDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE_KODE, AksjonspunktType.MANUELL, "Avklar om bruker har gyldig periode.",
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP,
            ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_FAKTA_FOR_PERSONSTATUS(
            AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS_KODE, AksjonspunktType.MANUELL, "Avklar fakta for status på person.",
            BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.INN, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER,
            ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_OPPHOLDSRETT(AksjonspunktKodeDefinisjon.AVKLAR_OPPHOLDSRETT_KODE,
            AksjonspunktType.MANUELL, "Avklar oppholdsrett.", BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN,
            VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VARSEL_REVURDERING_MANUELL(
            AksjonspunktKodeDefinisjon.VARSEL_REVURDERING_MANUELL_KODE, AksjonspunktType.MANUELL, "Varsel om revurdering opprettet manuelt",
            BehandlingStegType.VARSEL_REVURDERING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP)),
    FORESLÅ_VEDTAK_MANUELT(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_MANUELT_KODE,
            AksjonspunktType.MANUELL, "Foreslå vedtak manuelt", BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.VEDTAK,
            ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_VERGE(AksjonspunktKodeDefinisjon.AVKLAR_VERGE_KODE, AksjonspunktType.MANUELL,
            "Avklar verge", BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_VERGE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE(
            AksjonspunktKodeDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE_KODE, AksjonspunktType.MANUELL, "Vurdere om søkers ytelse gjelder samme barn",
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, TOTRINN, EnumSet.of(ES, FP)),
    VURDERE_ANNEN_YTELSE_FØR_VEDTAK(
            AksjonspunktKodeDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere annen ytelse før vedtak",
            BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VURDERE_DOKUMENT_FØR_VEDTAK(
            AksjonspunktKodeDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere dokument før vedtak",
            BehandlingStegType.FORESLÅ_VEDTAK,
            VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE, AksjonspunktType.MANUELL,
            "Fastsette beregningsgrunnlag for arbeidstaker/frilanser skjønnsmessig", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG,
            VurderingspunktType.UT, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE(
            AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE, AksjonspunktType.MANUELL,
            "Vurder varig endret/nyoppstartet næring selvstendig næringsdrivende", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT,
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE_KODE, AksjonspunktType.MANUELL,
            "Fastsett beregningsgrunnlag for selvstendig næringsdrivende", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT,
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    FORDEL_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.FORDEL_BEREGNINGSGRUNNLAG_KODE,
            AksjonspunktType.MANUELL, "Fordel beregningsgrunnlag", BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.FAKTA_OM_FORDELING, TOTRINN, EnumSet.of(FP, SVP)),
    FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE, AksjonspunktType.MANUELL,
            "Fastsett beregningsgrunnlag for tidsbegrenset arbeidsforhold", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT,
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE, AksjonspunktType.MANUELL,
            "Fastsett beregningsgrunnlag for SN som er ny i arbeidslivet", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_GRADERING_UTEN_BEREGNINGSGRUNNLAG(
            AksjonspunktKodeDefinisjon.VURDER_GRADERING_UTEN_BEREGNINGSGRUNNLAG_KODE, AksjonspunktType.MANUELL,
            "Vurder gradering på andel uten beregningsgrunnlag",
            BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_PERIODER_MED_OPPTJENING(
            AksjonspunktKodeDefinisjon.VURDER_PERIODER_MED_OPPTJENING_KODE, AksjonspunktType.MANUELL, "Vurder perioder med opptjening",
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.INN, VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.FAKTA_FOR_OPPTJENING,
            ENTRINN, EnumSet.of(FP, SVP)),
    AVKLAR_AKTIVITETER(AksjonspunktKodeDefinisjon.AVKLAR_AKTIVITETER_KODE,
            AksjonspunktType.MANUELL, "Avklar aktivitet for beregning", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    AVKLAR_FORTSATT_MEDLEMSKAP(
            AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE, AksjonspunktType.MANUELL, "Avklar fortsatt medlemskap.",
            BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP, VurderingspunktType.UT, VilkårType.MEDLEMSKAPSVILKÅRET,
            SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, TOTRINN, EnumSet.of(FP, SVP)),
    KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST(
            AksjonspunktKodeDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST_KODE, AksjonspunktType.MANUELL,
            "Vurder varsel ved vedtak til ugunst",
            BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING(
            AksjonspunktKodeDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING_KODE, AksjonspunktType.MANUELL,
            "Kontroll av manuelt opprettet revurderingsbehandling", BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN,
            EnumSet.of(ES, FP, SVP)),
    VURDER_FAKTA_FOR_ATFL_SN(AksjonspunktKodeDefinisjon.VURDER_FAKTA_FOR_ATFL_SN_KODE,
            AksjonspunktType.MANUELL, "Vurder fakta for arbeidstaker, frilans og selvstendig næringsdrivende", BehandlingStegType.KONTROLLER_FAKTA_BEREGNING,
            VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    AUTOMATISK_MARKERING_AV_UTENLANDSSAK(
            AksjonspunktKodeDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK_KODE, AksjonspunktType.MANUELL,
            "Innhent dokumentasjon fra utenlandsk trygdemyndighet",
            BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.INN, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    TILKNYTTET_STORTINGET(AksjonspunktKodeDefinisjon.TILKNYTTET_STORTINGET_KODE,
            AksjonspunktType.MANUELL, "Søker er stortingsrepresentant/administrativt ansatt i Stortinget", BehandlingStegType.VURDER_UTTAK,
            VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    KONTROLLER_OPPLYSNINGER_OM_MEDLEMSKAP(
            AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_MEDLEMSKAP_KODE, AksjonspunktType.MANUELL, "Kontroller opplysninger om medlemskap",
            BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    KONTROLLER_OPPLYSNINGER_OM_DØD(
            AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD_KODE, AksjonspunktType.MANUELL, "Kontroller opplysninger om død",
            BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST(
            AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE, AksjonspunktType.MANUELL, "Kontroller opplysninger om søknadsfrist",
            BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    KONTROLLER_TILSTØTENDE_YTELSER_INNVILGET(
            AksjonspunktKodeDefinisjon.KONTROLLER_TILSTØTENDE_YTELSER_INNVILGET_KODE, AksjonspunktType.MANUELL, "Kontroller tilstøtende ytelser innvilget",
            BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    KONTROLLER_TILSTØTENDE_YTELSER_OPPHØRT(
            AksjonspunktKodeDefinisjon.KONTROLLER_TILSTØTENDE_YTELSER_OPPHØRT_KODE, AksjonspunktType.MANUELL, "Kontroller tilstøtende ytelser opphørt",
            BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_ARBEIDSFORHOLD(AksjonspunktKodeDefinisjon.VURDER_ARBEIDSFORHOLD_KODE,
            AksjonspunktType.MANUELL, "Avklar arbeidsforhold", BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.FAKTA_OM_ARBEIDSFORHOLD, ENTRINN, EnumSet.of(FP, SVP)),
    VURDER_FEILUTBETALING(AksjonspunktKodeDefinisjon.VURDER_FEILUTBETALING_KODE,
            AksjonspunktType.MANUELL, "Vurder feilutbetaling", BehandlingStegType.SIMULER_OPPDRAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VURDER_INNTREKK(AksjonspunktKodeDefinisjon.VURDER_INNTREKK_KODE,
            AksjonspunktType.MANUELL, "Vurder inntrekk", BehandlingStegType.SIMULER_OPPDRAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VURDER_OPPTJENINGSVILKÅRET(
            AksjonspunktKodeDefinisjon.VURDER_OPPTJENINGSVILKÅRET_KODE, AksjonspunktType.MANUELL, "Manuell vurdering av opptjeningsvilkår",
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.UT, VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.PUNKT_FOR_OPPTJENING,
            TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_TILBAKETREKK(AksjonspunktKodeDefinisjon.VURDER_TILBAKETREKK_KODE,
            AksjonspunktType.MANUELL, "Vurder tilbaketrekk", BehandlingStegType.VURDER_TILBAKETREKK, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.TILKJENT_YTELSE, TOTRINN, EnumSet.of(FP)),
    VURDER_FARESIGNALER(AksjonspunktKodeDefinisjon.VURDER_FARESIGNALER_KODE,
            AksjonspunktType.MANUELL, "Vurder Faresignaler", BehandlingStegType.VURDER_FARESIGNALER, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.VURDER_FARESIGNALER, TOTRINN, EnumSet.of(ES, FP, SVP)),

    // Gruppe : 600

    SØKERS_OPPLYSNINGSPLIKT_OVST(AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST_KODE, AksjonspunktType.SAKSBEHANDLEROVERSTYRING,
            "Saksbehandler initierer kontroll av søkers opplysningsplikt", BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT, VurderingspunktType.UT,
        VilkårType.SØKERSOPPLYSNINGSPLIKT, SkjermlenkeType.OPPLYSNINGSPLIKT, ENTRINN, EnumSet.of(ES, FP, SVP)),
    OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av medlemskapsvilkåret",
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.UT, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP,
            TOTRINN, EnumSet.of(ES, FP, SVP)),
    OVERSTYRING_AV_BEREGNING(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNING_KODE,
            AksjonspunktType.OVERSTYRING, "Overstyring av beregning", BehandlingStegType.BEREGN_YTELSE, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(ES, FP, SVP)),
    OVERSTYRING_AV_OPPTJENINGSVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_OPPTJENINGSVILKÅRET_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av opptjeningsvilkåret",
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.UT, VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.PUNKT_FOR_OPPTJENING,
            TOTRINN, EnumSet.of(FP, SVP)),
    OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_LØPENDE(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_LØPENDE_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av løpende medlemskapsvilkåret", BehandlingStegType.VULOMED, VurderingspunktType.UT,
            VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE, SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP_LØPENDE, TOTRINN, EnumSet.of(FP)),
    OVERSTYRING_AV_BEREGNINGSAKTIVITETER(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av beregningsaktiviteter", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT,
            UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    OVERSTYRING_AV_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av beregningsgrunnlag",
            BehandlingStegType.KONTROLLER_FAKTA_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    MANUELL_MARKERING_AV_UTLAND_SAKSTYPE(AksjonspunktKodeDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE_KODE, AksjonspunktType.MANUELL, "Manuell markering av utenlandssak",
            BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTLAND, ENTRINN, EnumSet.of(ES, FP, SVP)),

    // Gruppe : 700

    AUTO_MANUELT_SATT_PÅ_VENT(AksjonspunktKodeDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT_KODE, AksjonspunktType.AUTOPUNKT,
            "Manuelt satt på vent", BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN,
            FORBLI, "P4W", EnumSet.of(ES, FP, SVP)),
    AUTO_VENTER_PÅ_KOMPLETT_SØKNAD(AksjonspunktKodeDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD_KODE, AksjonspunktType.AUTOPUNKT,
            "Venter på komplett søknad", BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, FORBLI, "P4W", EnumSet.of(ES, FP, SVP)),
    AUTO_SATT_PÅ_VENT_REVURDERING(AksjonspunktKodeDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING_KODE, AksjonspunktType.AUTOPUNKT,
            "Satt på vent etter varsel om revurdering", BehandlingStegType.VARSEL_REVURDERING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE,
            ENTRINN, FORBLI, "P4W", EnumSet.of(ES, FP)),
    AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER_KODE, AksjonspunktType.AUTOPUNKT, "Venter på opptjeningsopplysninger",
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.UT, VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.FAKTA_FOR_OPPTJENING,
        ENTRINN, TILBAKE, "P2W", EnumSet.of(FP, SVP)),
    VENT_PÅ_SCANNING(AksjonspunktKodeDefinisjon.VENT_PÅ_SCANNING_KODE,
            AksjonspunktType.AUTOPUNKT, "Venter på scanning", BehandlingStegType.VURDER_INNSYN, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE,
            "P3D", EnumSet.of(ES, FP)),
    VENT_PGA_FOR_TIDLIG_SØKNAD(AksjonspunktKodeDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD_KODE, AksjonspunktType.AUTOPUNKT, "Satt på vent pga for tidlig søknad",
            BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(ES, FP, SVP)),
    AUTO_VENT_KOMPLETT_OPPDATERING(AksjonspunktKodeDefinisjon.AUTO_VENT_KOMPLETT_OPPDATERING_KODE, AksjonspunktType.AUTOPUNKT, "Vent på oppdatering som passerer kompletthetssjekk",
            BehandlingStegType.FATTE_VEDTAK, VurderingspunktType.INN, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, FORBLI, UTEN_FRIST, EnumSet.of(ES, FP, SVP)),
    VENT_PÅ_SØKNAD(AksjonspunktKodeDefinisjon.VENT_PÅ_SØKNAD_KODE,
            AksjonspunktType.AUTOPUNKT, "Venter på søknad", BehandlingStegType.REGISTRER_SØKNAD, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE,
            "P3W", EnumSet.of(ES, FP, SVP)),
    AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST_KODE, AksjonspunktType.AUTOPUNKT, "Vent på rapporteringsfrist for inntekt",
            BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(FP, SVP)),
    AUTO_VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.AUTO_VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG_KODE, AksjonspunktType.AUTOPUNKT,
            "Autopunkt gradering uten beregningsgrunnlag",
            BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(FP, SVP)),
    AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT_KODE, AksjonspunktType.AUTOPUNKT,
            "Vent på siste meldekort for AAP eller DP-mottaker", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR,
        UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(FP, SVP)),
    AUTO_VENT_INNTEKTSMELDING_MED_UGYLDIG_ARBEIDSFORHOLDID(AksjonspunktKodeDefinisjon.AUTO_VENT_INNTEKTSMELDING_MED_UGYLDIG_ARBEIDSFORHOLDID_KODE, AksjonspunktType.AUTOPUNKT,
            "Vent på ny inntektsmelding med gyldig arbeidsforholdId", BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD, VurderingspunktType.UT, UTEN_VILKÅR,
        UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(FP, SVP)),
    AUTO_VENT_MILITÆR_OG_BG_UNDER_3G(AksjonspunktKodeDefinisjon.AUTO_VENT_MILITÆR_OG_BG_UNDER_3G_KODE, AksjonspunktType.AUTOPUNKT,
            "Autopunkt militær i opptjeningsperioden og beregninggrunnlag under 3G", BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG,
            VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(FP, SVP)),
    AUTO_VENT_GRADERING_FLERE_ARBEIDSFORHOLD(AksjonspunktKodeDefinisjon.AUTO_VENT_GRADERING_FLERE_ARBEIDSFORHOLD_KODE, AksjonspunktType.AUTOPUNKT, "Autopunkt gradering flere arbeidsforhold",
            BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(FP, SVP)),
    AUTO_VENT_ETTERLYST_INNTEKTSMELDING(AksjonspunktKodeDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING_KODE, AksjonspunktType.AUTOPUNKT, "Vent på etterlyst inntektsmelding",
            BehandlingStegType.INREG_AVSL, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P3W", EnumSet.of(ES, FP, SVP)),

    UNDEFINED,

    ;

    static final String KODEVERK = "AKSJONSPUNKT_DEF";

    private static final Map<String, AksjonspunktDefinisjon> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private AksjonspunktType aksjonspunktType = AksjonspunktType.UDEFINERT;

    /**
     * Definerer hvorvidt Aksjonspunktet default krever totrinnsbehandling. Dvs. Beslutter må godkjenne hva
     * Saksbehandler har utført.
     */
    @JsonIgnore
    private boolean defaultTotrinnBehandling = false;

    /**
     * Hvorvidt aksjonspunktet har en frist før det må være løst. Brukes i forbindelse med når Behandling er lagt til
     * Vent.
     */
    @JsonIgnore
    private String fristPeriode;

    @JsonIgnore
    private VilkårType vilkårType;

    @JsonIgnore
    private SkjermlenkeType skjermlenkeType;

    @JsonIgnore
    private boolean tilbakehoppVedGjenopptakelse;

    @JsonIgnore
    private BehandlingStegType behandlingStegType;

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private Set<YtelseType> ytelseTyper;

    @JsonIgnore
    private VurderingspunktType vurderingspunktType;

    @JsonIgnore
    private Set<String> utelukkendeAksjonspunkter = Collections.emptySet();

    @JsonIgnore
    private boolean erUtgått = false;

    private String kode;

    AksjonspunktDefinisjon() {
        // for hibernate
    }

    /** Brukes for utgåtte aksjonspunkt. Disse skal ikke kunne gjenoppstå. */
    private AksjonspunktDefinisjon(String kode, AksjonspunktType type, String navn) {
        this.kode = kode;
        this.aksjonspunktType = type;
        this.navn = navn;
        erUtgått = true;
    }

    // Bruk for ordinære aksjonspunkt og overstyring
    private AksjonspunktDefinisjon(String kode,
                                   AksjonspunktType aksjonspunktType,
                                   String navn,
                                   BehandlingStegType behandlingStegType,
                                   VurderingspunktType vurderingspunktType,
                                   VilkårType vilkårType,
                                   SkjermlenkeType skjermlenkeType,
                                   boolean defaultTotrinnBehandling,
                                   Set<FagsakYtelseType.YtelseType> ytelseTyper) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStegType = behandlingStegType;
        this.vurderingspunktType = vurderingspunktType;
        this.ytelseTyper = ytelseTyper;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = false;
        this.fristPeriode = null;
    }

    // Bruk for autopunkt i 7nnn serien
    private AksjonspunktDefinisjon(String kode,
                                   AksjonspunktType aksjonspunktType,
                                   String navn,
                                   BehandlingStegType behandlingStegType,
                                   VurderingspunktType vurderingspunktType,
                                   VilkårType vilkårType,
                                   SkjermlenkeType skjermlenkeType,
                                   boolean defaultTotrinnBehandling,
                                   boolean tilbakehoppVedGjenopptakelse,
                                   String fristPeriode,
                                   Set<FagsakYtelseType.YtelseType> ytelseTyper) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStegType = behandlingStegType;
        this.vurderingspunktType = vurderingspunktType;
        this.ytelseTyper = ytelseTyper;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = tilbakehoppVedGjenopptakelse;
        this.fristPeriode = fristPeriode;
    }


    /**
     * @deprecated Bruk heller
     *             {@link no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder#medSkjermlenke(SkjermlenkeType)}
     *             direkte og unngå å slå opp fra aksjonspunktdefinisjon
     */
    @Deprecated
    public SkjermlenkeType getSkjermlenkeType() {
        return skjermlenkeType;
    }

    public AksjonspunktType getAksjonspunktType() {
        return Objects.equals(AksjonspunktType.UDEFINERT, aksjonspunktType) ? null : aksjonspunktType;
    }

    public boolean erAutopunkt() {
        return AksjonspunktType.AUTOPUNKT.equals(getAksjonspunktType());
    }

    public boolean getDefaultTotrinnBehandling() {
        return defaultTotrinnBehandling;
    }

    public String getFristPeriode() {
        return fristPeriode;
    }

    public Period getFristPeriod() {
        return (fristPeriode == null ? null : Period.parse(fristPeriode));
    }

    public VilkårType getVilkårType() {
        return (Objects.equals(VilkårType.UDEFINERT, vilkårType) ? null : vilkårType);
    }

    public boolean tilbakehoppVedGjenopptakelse() {
        return tilbakehoppVedGjenopptakelse;
    }

    /** Returnerer kode verdi for aksjonspunkt utelukket av denne. */
    public Set<String> getUtelukkendeApdef() {
        return Set.of();
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    public BehandlingStegType getBehandlingSteg() {
        return behandlingStegType;
    }

    public VurderingspunktType getVurderingspunktType() {
        return vurderingspunktType;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    /** Aksjonspunkt tidligere brukt, nå utgått (kan ikke gjenoppstå). */
    public boolean erUtgått() {
        return erUtgått;
    }

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

    @JsonCreator
    public static AksjonspunktDefinisjon fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AksjonspunktDefinisjon: " + kode);
        }
        return ad;
    }

    public static Map<String, AksjonspunktDefinisjon> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static List<AksjonspunktDefinisjon> finnAksjonspunktDefinisjoner(BehandlingStegType behandlingStegType, VurderingspunktType vurderingspunktType) {
        return KODER.values().stream()
            .filter(ad -> Objects.equals(ad.getBehandlingSteg(), behandlingStegType) && Objects.equals(ad.getVurderingspunktType(), vurderingspunktType))
            .collect(Collectors.toList());
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<AksjonspunktDefinisjon, String> {
        @Override
        public String convertToDatabaseColumn(AksjonspunktDefinisjon attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AksjonspunktDefinisjon convertToEntityAttribute(String dbData) {
            return dbData == null ? null : AksjonspunktDefinisjon.fraKode(dbData);
        }
    }

    public static void main(String[] args) {
        for(var k: values()) {
            if(k.getAksjonspunktType().equals(AksjonspunktType.AUTOPUNKT)) {
                System.out.println(k.getKode() + ", ");
            }
        }
    }
}
