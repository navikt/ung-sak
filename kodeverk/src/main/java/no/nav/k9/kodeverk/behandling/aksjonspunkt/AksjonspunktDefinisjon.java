package no.nav.k9.kodeverk.behandling.aksjonspunkt;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMP;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PSB;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.ENTRINN;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.FORBLI;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.TILBAKE;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.TOTRINN;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_FRIST;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_SKJERMLENKE;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_VILKÅR;

import java.time.Period;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;

/**
 * Definerer mulige Aksjonspunkter inkludert hvilket Vurderingspunkt de må løses i.
 * Inkluderer også konstanter for å enklere kunne referere til dem i eksisterende logikk.
 */
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum AksjonspunktDefinisjon implements Kodeverdi {

    // Gruppe : 5xxx
    AVKLAR_TILLEGGSOPPLYSNINGER(
            AksjonspunktKodeDefinisjon.AVKLAR_TILLEGGSOPPLYSNINGER_KODE, AksjonspunktType.MANUELL, "Avklar tilleggsopplysninger",
            BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(OMP, PSB)),
    FORESLÅ_VEDTAK(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_KODE,
            AksjonspunktType.MANUELL, "Foreslå vedtak", BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.VEDTAK, ENTRINN, EnumSet.of(OMP, PSB)),
    FATTER_VEDTAK(AksjonspunktKodeDefinisjon.FATTER_VEDTAK_KODE,
            AksjonspunktType.MANUELL, "Fatter vedtak", BehandlingStegType.FATTE_VEDTAK, VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.VEDTAK, ENTRINN,
            EnumSet.of(OMP, PSB)),
    SØKERS_OPPLYSNINGSPLIKT_MANU(
            AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU_KODE, AksjonspunktType.MANUELL,
            "Vurder søkers opplysningsplikt ved ufullstendig/ikke-komplett søknad", BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT,
            VurderingspunktType.UT, VilkårType.SØKERSOPPLYSNINGSPLIKT, SkjermlenkeType.OPPLYSNINGSPLIKT, ENTRINN, EnumSet.of(OMP, PSB)),
    VEDTAK_UTEN_TOTRINNSKONTROLL(
            AksjonspunktKodeDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL_KODE, AksjonspunktType.MANUELL, "Foreslå vedtak uten totrinnskontroll",
            BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(OMP, PSB)),
    AVKLAR_LOVLIG_OPPHOLD(AksjonspunktKodeDefinisjon.AVKLAR_LOVLIG_OPPHOLD_KODE,
            AksjonspunktType.MANUELL, "Avklar lovlig opphold.", BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN,
            VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, ENTRINN, EnumSet.of(OMP, PSB)),
    AVKLAR_OM_ER_BOSATT(AksjonspunktKodeDefinisjon.AVKLAR_OM_ER_BOSATT_KODE,
            AksjonspunktType.MANUELL, "Avklar om bruker er bosatt.", BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN,
            VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, ENTRINN, EnumSet.of(OMP, PSB)),
    AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE(
            AksjonspunktKodeDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE_KODE, AksjonspunktType.MANUELL, "Avklar om bruker har gyldig periode.",
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP,
            ENTRINN, EnumSet.of(OMP, PSB)),
    AVKLAR_FAKTA_FOR_PERSONSTATUS(
            AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS_KODE, AksjonspunktType.MANUELL, "Avklar fakta for status på person.",
            BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.INN, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER,
            ENTRINN, EnumSet.of(OMP, PSB)),
    AVKLAR_OPPHOLDSRETT(AksjonspunktKodeDefinisjon.AVKLAR_OPPHOLDSRETT_KODE,
            AksjonspunktType.MANUELL, "Avklar oppholdsrett.", BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN,
            VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, ENTRINN, EnumSet.of(OMP, PSB)),
    VARSEL_REVURDERING_MANUELL(
            AksjonspunktKodeDefinisjon.VARSEL_REVURDERING_MANUELL_KODE, AksjonspunktType.MANUELL, "Varsel om revurdering opprettet manuelt",
            BehandlingStegType.VARSEL_REVURDERING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(OMP, PSB)),
    FORESLÅ_VEDTAK_MANUELT(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_MANUELT_KODE,
            AksjonspunktType.MANUELL, "Foreslå vedtak manuelt", BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.VEDTAK,
            ENTRINN, EnumSet.of(OMP, PSB)),
    AVKLAR_VERGE(AksjonspunktKodeDefinisjon.AVKLAR_VERGE_KODE, AksjonspunktType.MANUELL,
            "Avklar verge", BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_VERGE, ENTRINN, EnumSet.of(OMP, PSB)),
    VURDERE_ANNEN_YTELSE_FØR_VEDTAK(
            AksjonspunktKodeDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere annen ytelse før vedtak",
            BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(OMP, PSB)),
    VURDERE_DOKUMENT_FØR_VEDTAK(
            AksjonspunktKodeDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere dokument før vedtak",
            BehandlingStegType.FORESLÅ_VEDTAK,
            VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(OMP, PSB)),
    FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE, AksjonspunktType.MANUELL,
            "Fastsette beregningsgrunnlag for arbeidstaker/frilanser skjønnsmessig", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG,
            VurderingspunktType.UT, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(OMP, PSB)),
    VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE(
            AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE, AksjonspunktType.MANUELL,
            "Vurder varig endret/nyoppstartet næring selvstendig næringsdrivende", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT,
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(OMP, PSB)),
    FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE_KODE, AksjonspunktType.MANUELL,
            "Fastsett beregningsgrunnlag for selvstendig næringsdrivende", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT,
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(OMP, PSB)),
    FORDEL_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.FORDEL_BEREGNINGSGRUNNLAG_KODE,
            AksjonspunktType.MANUELL, "Fordel beregningsgrunnlag", BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.FAKTA_OM_FORDELING, TOTRINN, EnumSet.of(OMP, PSB)),
    FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE, AksjonspunktType.MANUELL,
            "Fastsett beregningsgrunnlag for tidsbegrenset arbeidsforhold", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT,
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(OMP, PSB)),
    FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE, AksjonspunktType.MANUELL,
            "Fastsett beregningsgrunnlag for SN som er ny i arbeidslivet", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(OMP, PSB)),
    VURDER_GRADERING_UTEN_BEREGNINGSGRUNNLAG(
            AksjonspunktKodeDefinisjon.VURDER_GRADERING_UTEN_BEREGNINGSGRUNNLAG_KODE, AksjonspunktType.MANUELL,
            "Vurder gradering på andel uten beregningsgrunnlag",
            BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(OMP, PSB)),
    VURDER_PERIODER_MED_OPPTJENING(
            AksjonspunktKodeDefinisjon.VURDER_PERIODER_MED_OPPTJENING_KODE, AksjonspunktType.MANUELL, "Vurder perioder med opptjening",
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.INN, VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.FAKTA_FOR_OPPTJENING,
            ENTRINN, EnumSet.of(OMP, PSB)),
    AVKLAR_AKTIVITETER(AksjonspunktKodeDefinisjon.AVKLAR_AKTIVITETER_KODE,
            AksjonspunktType.MANUELL, "Avklar aktivitet for beregning", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT,
            UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(OMP, PSB)),
    AVKLAR_FORTSATT_MEDLEMSKAP(
            AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE, AksjonspunktType.MANUELL, "Avklar medlemskap.",
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN, VilkårType.MEDLEMSKAPSVILKÅRET,
            SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, TOTRINN, EnumSet.of(OMP, PSB)),
    KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST(
            AksjonspunktKodeDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST_KODE, AksjonspunktType.MANUELL,
            "Vurder varsel ved vedtak til ugunst",
            BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(OMP, PSB)),
    KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING(
            AksjonspunktKodeDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING_KODE, AksjonspunktType.MANUELL,
            "Kontroll av manuelt opprettet revurderingsbehandling", BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN,
            EnumSet.of(OMP, PSB)),
    VURDER_FAKTA_FOR_ATFL_SN(AksjonspunktKodeDefinisjon.VURDER_FAKTA_FOR_ATFL_SN_KODE,
            AksjonspunktType.MANUELL, "Vurder fakta for arbeidstaker, frilans og selvstendig næringsdrivende", BehandlingStegType.KONTROLLER_FAKTA_BEREGNING,
            VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(OMP, PSB)),
    AUTOMATISK_MARKERING_AV_UTENLANDSSAK(
            AksjonspunktKodeDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK_KODE, AksjonspunktType.MANUELL,
            "Innhent dokumentasjon fra utenlandsk trygdemyndighet",
            BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.INN, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(OMP, PSB)),
    TILKNYTTET_STORTINGET(AksjonspunktKodeDefinisjon.TILKNYTTET_STORTINGET_KODE,
            AksjonspunktType.MANUELL, "Søker er stortingsrepresentant/administrativt ansatt i Stortinget", BehandlingStegType.VURDER_UTTAK,
            VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(OMP, PSB)),
    VURDER_ARBEIDSFORHOLD(AksjonspunktKodeDefinisjon.VURDER_ARBEIDSFORHOLD_KODE,
            AksjonspunktType.MANUELL, "Avklar arbeidsforhold", BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.FAKTA_OM_ARBEIDSFORHOLD, ENTRINN, EnumSet.of(OMP, PSB)),
    VURDER_FEILUTBETALING(AksjonspunktKodeDefinisjon.VURDER_FEILUTBETALING_KODE,
            AksjonspunktType.MANUELL, "Vurder feilutbetaling", BehandlingStegType.SIMULER_OPPDRAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(OMP, PSB)),
    VURDER_INNTREKK(AksjonspunktKodeDefinisjon.VURDER_INNTREKK_KODE,
            AksjonspunktType.MANUELL, "Vurder inntrekk", BehandlingStegType.SIMULER_OPPDRAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(OMP, PSB)),
    VURDER_OPPTJENINGSVILKÅRET(
            AksjonspunktKodeDefinisjon.VURDER_OPPTJENINGSVILKÅRET_KODE, AksjonspunktType.MANUELL, "Manuell vurdering av opptjeningsvilkår",
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.UT, VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.PUNKT_FOR_OPPTJENING,
            TOTRINN, EnumSet.of(OMP, PSB)),
    VURDER_TILBAKETREKK(AksjonspunktKodeDefinisjon.VURDER_TILBAKETREKK_KODE,
            AksjonspunktType.MANUELL, "Vurder tilbaketrekk", BehandlingStegType.VURDER_TILBAKETREKK, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.TILKJENT_YTELSE, TOTRINN, EnumSet.of(OMP, PSB)),
    VURDER_FARESIGNALER(AksjonspunktKodeDefinisjon.VURDER_FARESIGNALER_KODE,
            AksjonspunktType.MANUELL, "Vurder Faresignaler", BehandlingStegType.VURDER_FARESIGNALER, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.VURDER_FARESIGNALER, TOTRINN, EnumSet.of(OMP, PSB)),

    // Gruppe : 60xx

    SØKERS_OPPLYSNINGSPLIKT_OVST(AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST_KODE, AksjonspunktType.SAKSBEHANDLEROVERSTYRING,
            "Saksbehandler initierer kontroll av søkers opplysningsplikt", BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT, VurderingspunktType.UT,
            VilkårType.SØKERSOPPLYSNINGSPLIKT, SkjermlenkeType.OPPLYSNINGSPLIKT, ENTRINN, EnumSet.of(OMP, PSB)),
    OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av medlemskapsvilkåret",
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.UT, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.PUNKT_FOR_MEDISINSK,
            TOTRINN, EnumSet.of(OMP, PSB)),
    OVERSTYRING_AV_MEDISINSKESVILKÅRET_UNDER_18(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_MEDISINSKVILKÅR_UNDER_18_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av medisinskvilkår for pleietrengende under 18 år",
            BehandlingStegType.VURDER_MEDISINSKVILKÅR, VurderingspunktType.UT, VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, SkjermlenkeType.PUNKT_FOR_MEDISINSK,
            TOTRINN, EnumSet.of(OMP, PSB)),
    OVERSTYRING_AV_OMSORGEN_FOR(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_OMSORGENFOR_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av OMPorgen for",
            BehandlingStegType.VURDER_OMSORG_FOR, VurderingspunktType.UT, VilkårType.OMSORGEN_FOR, SkjermlenkeType.PUNKT_FOR_OMSORGEN_FOR,
            TOTRINN, EnumSet.of(OMP, PSB)),
    OVERSTYRING_AV_BEREGNING(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNING_KODE,
            AksjonspunktType.OVERSTYRING, "Overstyring av beregning", BehandlingStegType.BEREGN_YTELSE, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.BEREGNING, TOTRINN, EnumSet.of(OMP, PSB)),
    OVERSTYRING_AV_OPPTJENINGSVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_OPPTJENINGSVILKÅRET_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av opptjeningsvilkåret",
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.UT, VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.PUNKT_FOR_OPPTJENING,
            TOTRINN, EnumSet.of(OMP, PSB)),
    OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_LØPENDE(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_LØPENDE_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av løpende medlemskapsvilkåret", BehandlingStegType.VULOMED, VurderingspunktType.UT,
            VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE, SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP_LØPENDE, TOTRINN, EnumSet.of(OMP, PSB)),
    OVERSTYRING_AV_BEREGNINGSAKTIVITETER(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av beregningsaktiviteter", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT,
            UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(OMP, PSB)),
    OVERSTYRING_AV_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av beregningsgrunnlag",
            BehandlingStegType.KONTROLLER_FAKTA_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(OMP, PSB)),
    MANUELL_MARKERING_AV_UTLAND_SAKSTYPE(AksjonspunktKodeDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE_KODE, AksjonspunktType.MANUELL, "Manuell markering av utenlandssak",
            BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTLAND, ENTRINN, EnumSet.of(OMP, PSB)),

    // Gruppe : 70xx

    AUTO_MANUELT_SATT_PÅ_VENT(AksjonspunktKodeDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT_KODE, AksjonspunktType.AUTOPUNKT,
            "Manuelt satt på vent", BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN,
            FORBLI, "P4W", EnumSet.of(OMP, PSB)),
    AUTO_VENTER_PÅ_KOMPLETT_SØKNAD(AksjonspunktKodeDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD_KODE, AksjonspunktType.AUTOPUNKT,
            "Venter på komplett søknad", BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, FORBLI, "P4W", EnumSet.of(OMP, PSB)),
    AUTO_SATT_PÅ_VENT_REVURDERING(AksjonspunktKodeDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING_KODE, AksjonspunktType.AUTOPUNKT,
            "Satt på vent etter varsel om revurdering", BehandlingStegType.VARSEL_REVURDERING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE,
            ENTRINN, FORBLI, "P4W", EnumSet.of(OMP, PSB)),
    AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER_KODE, AksjonspunktType.AUTOPUNKT, "Venter på opptjeningsopplysninger",
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.UT, VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.FAKTA_FOR_OPPTJENING,
            ENTRINN, TILBAKE, "P2W", EnumSet.of(OMP, PSB)),
    VENT_PÅ_SCANNING(AksjonspunktKodeDefinisjon.VENT_PÅ_SCANNING_KODE,
            AksjonspunktType.AUTOPUNKT, "Venter på scanning", BehandlingStegType.VURDER_INNSYN, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE,
            "P3D", EnumSet.of(OMP, PSB)),
    VENT_PGA_FOR_TIDLIG_SØKNAD(AksjonspunktKodeDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD_KODE, AksjonspunktType.AUTOPUNKT, "Satt på vent pga for tidlig søknad",
            BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(OMP, PSB)),
    AUTO_VENT_KOMPLETT_OPPDATERING(AksjonspunktKodeDefinisjon.AUTO_VENT_KOMPLETT_OPPDATERING_KODE, AksjonspunktType.AUTOPUNKT, "Vent på oppdatering som passerer kompletthetssjekk",
            BehandlingStegType.FATTE_VEDTAK, VurderingspunktType.INN, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, FORBLI, UTEN_FRIST, EnumSet.of(OMP, PSB)),
    AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST_KODE, AksjonspunktType.AUTOPUNKT, "Vent på rapporteringsfrist for inntekt",
            BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(OMP, PSB)),
    AUTO_VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.AUTO_VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG_KODE, AksjonspunktType.AUTOPUNKT,
            "Autopunkt gradering uten beregningsgrunnlag",
            BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(OMP, PSB)),
    AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT_KODE, AksjonspunktType.AUTOPUNKT,
            "Vent på siste meldekort for AAP eller DP-mottaker", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR,
            UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(OMP, PSB)),
    AUTO_VENT_INNTEKTSMELDING_MED_UGYLDIG_ARBEIDSFORHOLDID(AksjonspunktKodeDefinisjon.AUTO_VENT_INNTEKTSMELDING_MED_UGYLDIG_ARBEIDSFORHOLDID_KODE, AksjonspunktType.AUTOPUNKT,
            "Vent på ny inntektsmelding med gyldig arbeidsforholdId", BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD, VurderingspunktType.UT, UTEN_VILKÅR,
            UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(OMP, PSB)),
    AUTO_VENT_MILITÆR_OG_BG_UNDER_3G(AksjonspunktKodeDefinisjon.AUTO_VENT_MILITÆR_OG_BG_UNDER_3G_KODE, AksjonspunktType.AUTOPUNKT,
            "Autopunkt militær i opptjeningsperioden og beregninggrunnlag under 3G", BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG,
            VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(OMP, PSB)),
    AUTO_VENT_GRADERING_FLERE_ARBEIDSFORHOLD(AksjonspunktKodeDefinisjon.AUTO_VENT_GRADERING_FLERE_ARBEIDSFORHOLD_KODE, AksjonspunktType.AUTOPUNKT, "Autopunkt gradering flere arbeidsforhold",
            BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(OMP, PSB)),
    AUTO_VENT_ETTERLYST_INNTEKTSMELDING(AksjonspunktKodeDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING_KODE, AksjonspunktType.AUTOPUNKT, "Vent på etterlyst inntektsmelding",
            BehandlingStegType.INREG_AVSL, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P4D", EnumSet.of(OMP, PSB)),
    AUTO_VENT_BRUKER_70_ÅR(AksjonspunktKodeDefinisjon.AUTO_VENT_BRUKER_70_ÅR, AksjonspunktType.AUTOPUNKT, "Venter på manglende funksjonalitet, bruker 70år ved refusjonskrav",
            BehandlingStegType.VURDER_OPPTJENING_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P26W", EnumSet.of(OMP, PSB)),

    // Gruppe : 80xx (FRISINN)
    AUTO_VENT_FRISINN_BEREGNING(AksjonspunktKodeDefinisjon.AUTO_VENT_FRISINN_BEREGNING, AksjonspunktType.AUTOPUNKT, "Venter på manglende funksjonalitet.",
            BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P1D", EnumSet.of(FRISINN)),
    AUTO_VENT_FRISINN_MANGLENDE_FUNKSJONALITET(AksjonspunktKodeDefinisjon.AUTO_VENT_FRISINN_MANGLENDE_FUNKSJONALITET, AksjonspunktType.AUTOPUNKT, "Venter på manglende funksjonalitet.",
            BehandlingStegType.VARIANT_FILTER, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P26W", EnumSet.of(FRISINN)),

    AUTO_VENT_FRISINN_ATFL_SAMME_ORG(AksjonspunktKodeDefinisjon.AUTO_VENT_FRISINN_ATFL_SAMME_ORG_KODE, AksjonspunktType.AUTOPUNKT, "Arbeidstaker og frilanser i samme organisasjon, kan ikke beregnes.",
            BehandlingStegType.KONTROLLER_FAKTA_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P26W", EnumSet.of(FRISINN)),

    OVERSTYRING_FRISINN_OPPGITT_OPPTJENING(AksjonspunktKodeDefinisjon.OVERSTYRING_FRISINN_OPPGITT_OPPTJENING_KODE, AksjonspunktType.MANUELL, "Saksbehandler overstyrer oppgitt opptjening",
            BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.INN, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, Set.of(FRISINN)),

    // Gruppe : 90xx

    KONTROLLER_LEGEERKLÆRING(AksjonspunktKodeDefinisjon.KONTROLLER_LEGEERKLÆRING_KODE,
            AksjonspunktType.MANUELL, "Kontroller legeerklæring", BehandlingStegType.VURDER_MEDISINSKVILKÅR, VurderingspunktType.INN, VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR,
            SkjermlenkeType.FAKTA_OM_MEDISINSK, TOTRINN, EnumSet.of(PSB)),
    VURDER_OMSORGEN_FOR(AksjonspunktKodeDefinisjon.AVKLAR_OMSORGEN_FOR_KODE,
            AksjonspunktType.MANUELL, "Omsorgen for", BehandlingStegType.VURDER_OMSORG_FOR, VurderingspunktType.INN, VilkårType.OMSORGEN_FOR,
            SkjermlenkeType.FAKTA_OM_MEDISINSK, TOTRINN, EnumSet.of(PSB)),
    VURDER_ÅRSKVANTUM_KVOTE(AksjonspunktKodeDefinisjon.VURDER_ÅRSKVANTUM_KVOTE,
            AksjonspunktType.MANUELL, "Årskvantum", BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, VilkårType.ÅRSKVANTUM,
            SkjermlenkeType.FAKTA_OM_ÅRSKVANTUM, ENTRINN, TILBAKE, null, EnumSet.of(OMSORGSPENGER)),

    // Gruppe : 999x
    AUTO_VENT_FILTER_MANGLENDE_FUNKSJONALITET(AksjonspunktKodeDefinisjon.AUTO_VENT_FILTER_MANGLENDE_FUNKSJONALITET, AksjonspunktType.AUTOPUNKT, "Venter på manglende funksjonalitet.",
            BehandlingStegType.VARIANT_FILTER, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P26W", EnumSet.of(OMP, FRISINN, PSB)),

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
    private Set<FagsakYtelseType> ytelseTyper;

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

    /**
     * Brukes for utgåtte aksjonspunkt. Disse skal ikke kunne gjenoppstå.
     */
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
                                   Set<FagsakYtelseType> ytelseTyper) {
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
                                   Set<FagsakYtelseType> ytelseTyper) {
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

    /**
     * Returnerer kode verdi for aksjonspunkt utelukket av denne.
     */
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

    @JsonProperty(value = "kode")
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

    @JsonProperty(value = "kodeverk", access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    /**
     * Aksjonspunkt tidligere brukt, nå utgått (kan ikke gjenoppstå).
     */
    public boolean erUtgått() {
        return erUtgått;
    }

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

    public static void main(String[] args) {
        var sb = new StringBuilder(100 * 1000);

        sb.append("kode,type,ytelse,navn,defaultTotrinn,behandlingSteg\n");

        for (var v : values()) {
            var k = v.getKode();

            var ytelseTyper = v.ytelseTyper == null || v.ytelseTyper.isEmpty() ? Set.of(FagsakYtelseType.UDEFINERT) : v.ytelseTyper;
            for (var yt : ytelseTyper) {
                var sb2 = new StringBuilder(300);
                sb2.append(k).append(",");
                sb2.append(v.aksjonspunktType.getKode()).append(",");
                sb2.append(yt.getKode()).append(",");
                String navn = v.navn==null?"":"\""+v.navn+"\"";
                sb2.append(navn).append(",");
                sb2.append(v.defaultTotrinnBehandling).append(",");
                sb2.append(v.behandlingStegType==null?"":v.behandlingStegType.getKode() + (v.vurderingspunktType==null?"":":"+v.vurderingspunktType));
                
                sb.append(sb2).append("\n");
            }

        }

        System.out.println(sb.toString());
    }
}
