package no.nav.ung.kodeverk.behandling.aksjonspunkt;

import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.AVBRYTES;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.ENTRINN;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.FORBLI;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.KAN_OVERSTYRE_TOTRINN_ETTER_LUKKING;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.SKAL_IKKE_AVBRYTES;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.TILBAKE;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.TOTRINN;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_FRIST;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_SKJERMLENKE;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_VILKÅR;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.Ventekategori.AVVENTER_ANNET;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.Ventekategori.AVVENTER_ANNET_IKKE_SAKSBEHANDLINGSTID;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.Ventekategori.AVVENTER_ARBEIDSGIVER;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.Ventekategori.AVVENTER_SAKSBEHANDLER;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.Ventekategori.AVVENTER_SØKER;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.Ventekategori.AVVENTER_TEKNISK_FEIL;

import java.time.Period;
import java.util.Collections;
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
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.vilkår.VilkårType;

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
        BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    FORESLÅ_VEDTAK(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_KODE,
        AksjonspunktType.MANUELL, "Foreslå vedtak", BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.VEDTAK, ENTRINN, AVVENTER_SAKSBEHANDLER),
    FATTER_VEDTAK(AksjonspunktKodeDefinisjon.FATTER_VEDTAK_KODE,
        AksjonspunktType.MANUELL, "Fatter vedtak", Set.of(BehandlingStatus.FATTER_VEDTAK, BehandlingStatus.UTREDES), BehandlingStegType.FATTE_VEDTAK, VurderingspunktType.INN, UTEN_VILKÅR,
        SkjermlenkeType.VEDTAK,
        ENTRINN, AVVENTER_SAKSBEHANDLER),
    SØKERS_OPPLYSNINGSPLIKT_MANU(
        AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU_KODE, AksjonspunktType.MANUELL,
        "Vurder søkers opplysningsplikt ved ufullstendig/ikke-komplett søknad", BehandlingStatus.UTREDES,
        BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT, VurderingspunktType.UT, VilkårType.SØKERSOPPLYSNINGSPLIKT, SkjermlenkeType.OPPLYSNINGSPLIKT, ENTRINN, AVVENTER_SAKSBEHANDLER),
    VEDTAK_UTEN_TOTRINNSKONTROLL(
        AksjonspunktKodeDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL_KODE, AksjonspunktType.MANUELL, "Foreslå vedtak uten totrinnskontroll",
        BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    AVKLAR_LOVLIG_OPPHOLD(AksjonspunktKodeDefinisjon.AVKLAR_LOVLIG_OPPHOLD_KODE,
        AksjonspunktType.MANUELL, "Avklar lovlig opphold.", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR,
        VurderingspunktType.INN, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, ENTRINN, AVVENTER_SAKSBEHANDLER),
    AVKLAR_OM_ER_BOSATT(AksjonspunktKodeDefinisjon.AVKLAR_OM_ER_BOSATT_KODE,
        AksjonspunktType.MANUELL, "Avklar om bruker er bosatt.", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR,
        VurderingspunktType.INN, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, ENTRINN, AVVENTER_SAKSBEHANDLER),
    AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE(
        AksjonspunktKodeDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE_KODE, AksjonspunktType.MANUELL, "Avklar om bruker har gyldig periode.",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN, VilkårType.MEDLEMSKAPSVILKÅRET,
        SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, ENTRINN, AVVENTER_SAKSBEHANDLER),
    AVKLAR_OPPHOLDSRETT(AksjonspunktKodeDefinisjon.AVKLAR_OPPHOLDSRETT_KODE,
        AksjonspunktType.MANUELL, "Avklar oppholdsrett.", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR,
        VurderingspunktType.INN, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, ENTRINN, AVVENTER_SAKSBEHANDLER),
    VARSEL_REVURDERING_MANUELL(
        AksjonspunktKodeDefinisjon.VARSEL_REVURDERING_MANUELL_KODE, AksjonspunktType.MANUELL, "Varsel om revurdering opprettet manuelt",
        BehandlingStatus.UTREDES, BehandlingStegType.VARSEL_REVURDERING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    FORESLÅ_VEDTAK_MANUELT(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_MANUELT_KODE,
        AksjonspunktType.MANUELL, "Foreslå vedtak manuelt", BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR,
        SkjermlenkeType.VEDTAK, ENTRINN, AVVENTER_SAKSBEHANDLER),
    AVKLAR_VERGE(AksjonspunktKodeDefinisjon.AVKLAR_VERGE_KODE, AksjonspunktType.MANUELL,
        "Avklar verge", BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_VERGE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    VURDERE_ANNEN_YTELSE_FØR_VEDTAK(
        AksjonspunktKodeDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere annen ytelse før vedtak",
        BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    VURDERE_DOKUMENT_FØR_VEDTAK(
        AksjonspunktKodeDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere dokument før vedtak",
        BehandlingStatus.UTREDES,
        BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    VURDERE_OVERLAPPENDE_YTELSER_FØR_VEDTAK(
        AksjonspunktKodeDefinisjon.VURDERE_OVERLAPPENDE_YTELSER_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere overlappende ytelse før vedtak",
        BehandlingStatus.UTREDES,
        BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS(
        AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE, AksjonspunktType.MANUELL,
        "Fastsette beregningsgrunnlag for arbeidstaker/frilanser skjønnsmessig", BehandlingStatus.UTREDES,
        BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE(
        AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE, AksjonspunktType.MANUELL,
        "Vurder varig endret/nyoppstartet næring selvstendig næringsdrivende", BehandlingStatus.UTREDES, BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG,
        VurderingspunktType.UT, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    VURDER_VARIG_ENDRET_ARBEIDSSITUASJON(
        AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ARBEIDSSITUASJON_KODE, AksjonspunktType.MANUELL,
        "Vurder varig endret/nyoppstartet næring selvstendig næringsdrivende", BehandlingStatus.UTREDES, BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG,
        VurderingspunktType.UT, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE(
        AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE_KODE, AksjonspunktType.MANUELL,
        "Fastsett beregningsgrunnlag for selvstendig næringsdrivende", BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG,
        VurderingspunktType.UT, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    FORDEL_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.FORDEL_BEREGNINGSGRUNNLAG_KODE,
        AksjonspunktType.MANUELL, "Fordel beregningsgrunnlag", BehandlingStatus.UTREDES, BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_FORDELING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    VURDER_NYTT_INNTEKTSFORHOLD(AksjonspunktKodeDefinisjon.VURDER_NYTT_INNTEKTSFORHOLD_KODE,
        AksjonspunktType.MANUELL, "Vurder nytt inntektsforhold", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_TILKOMMET_INNTEKT, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_FORDELING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    VURDER_REFUSJON_BERGRUNN(AksjonspunktKodeDefinisjon.VURDER_REFUSJON_BERGRUNN_KODE,
        AksjonspunktType.MANUELL, "Vurder refusjon beregningsgrunnlag", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_REF_BERGRUNN, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_FORDELING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD(
        AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE, AksjonspunktType.MANUELL,
        "Fastsett beregningsgrunnlag for tidsbegrenset arbeidsforhold", BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG,
        VurderingspunktType.UT, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET(
        AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE, AksjonspunktType.MANUELL,
        "Fastsett beregningsgrunnlag for SN som er ny i arbeidslivet", BehandlingStatus.UTREDES, BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    VURDER_GRADERING_UTEN_BEREGNINGSGRUNNLAG(
        AksjonspunktKodeDefinisjon.VURDER_GRADERING_UTEN_BEREGNINGSGRUNNLAG_KODE, AksjonspunktType.MANUELL,
        "Vurder gradering på andel uten beregningsgrunnlag",
        BehandlingStatus.UTREDES, BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    VURDER_PERIODER_MED_OPPTJENING(
        AksjonspunktKodeDefinisjon.VURDER_PERIODER_MED_OPPTJENING_KODE, AksjonspunktType.MANUELL, "Vurder perioder med opptjening",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.INN, VilkårType.OPPTJENINGSVILKÅRET,
        SkjermlenkeType.FAKTA_FOR_OPPTJENING, ENTRINN, AVVENTER_SAKSBEHANDLER),
    AVKLAR_AKTIVITETER(AksjonspunktKodeDefinisjon.AVKLAR_AKTIVITETER_KODE,
        AksjonspunktType.MANUELL, "Avklar aktivitet for beregning", BehandlingStatus.UTREDES, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING,
        VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    AVKLAR_FORTSATT_MEDLEMSKAP(
        AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE, AksjonspunktType.MANUELL, "Avklar medlemskap.",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN,
        VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, TOTRINN, AVVENTER_SAKSBEHANDLER),
    KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST(
        AksjonspunktKodeDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST_KODE, AksjonspunktType.MANUELL,
        "Vurder varsel ved vedtak til ugunst",
        BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING(
        AksjonspunktKodeDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING_KODE, AksjonspunktType.MANUELL,
        "Kontroll av manuelt opprettet revurderingsbehandling", Set.of(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES), BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT,
        UTEN_VILKÅR, UTEN_SKJERMLENKE,
        ENTRINN, AVVENTER_SAKSBEHANDLER),
    MANUELL_TILKJENT_YTELSE(
        AksjonspunktKodeDefinisjon.MANUELL_TILKJENT_YTELSE_KODE, AksjonspunktType.MANUELL,
        "Manuell tilkjenning av ytelse", Set.of(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES), BehandlingStegType.MANUELL_TILKJENNING_YTELSE, VurderingspunktType.INN,
        UTEN_VILKÅR, SkjermlenkeType.TILKJENT_YTELSE,
        TOTRINN, AVVENTER_SAKSBEHANDLER),
    VURDER_FAKTA_FOR_ATFL_SN(AksjonspunktKodeDefinisjon.VURDER_FAKTA_FOR_ATFL_SN_KODE,
        AksjonspunktType.MANUELL, "Vurder fakta for arbeidstaker, frilans og selvstendig næringsdrivende", BehandlingStatus.UTREDES,
        BehandlingStegType.KONTROLLER_FAKTA_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    AUTOMATISK_MARKERING_AV_UTENLANDSSAK(
        AksjonspunktKodeDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK_KODE, AksjonspunktType.MANUELL,
        "Innhent dokumentasjon fra utenlandsk trygdemyndighet",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.INN, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    TILKNYTTET_STORTINGET(AksjonspunktKodeDefinisjon.TILKNYTTET_STORTINGET_KODE,
        AksjonspunktType.MANUELL, "Søker er stortingsrepresentant/administrativt ansatt i Stortinget", BehandlingStatus.UTREDES,
        BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, AVVENTER_SAKSBEHANDLER),
    VURDER_ARBEIDSFORHOLD(AksjonspunktKodeDefinisjon.VURDER_ARBEIDSFORHOLD_KODE,
        AksjonspunktType.MANUELL, "Avklar arbeidsforhold", BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_ARBEIDSFORHOLD, ENTRINN, AVVENTER_SAKSBEHANDLER),
    VURDER_FEILUTBETALING(AksjonspunktKodeDefinisjon.VURDER_FEILUTBETALING_KODE,
        AksjonspunktType.MANUELL, "Vurder feilutbetaling", BehandlingStatus.UTREDES, BehandlingStegType.SIMULER_OPPDRAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    SJEKK_TILBAKEKREVING(AksjonspunktKodeDefinisjon.SJEKK_TILBAKEKREVING_KODE,
        AksjonspunktType.MANUELL, "Sjekk om ytelsesbehandlingen skal utføres før eller etter tilbakekrevingsbehandlingen", BehandlingStatus.UTREDES, BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),
    VURDER_OPPTJENINGSVILKÅRET(
        AksjonspunktKodeDefinisjon.VURDER_OPPTJENINGSVILKÅRET_KODE, AksjonspunktType.MANUELL, "Manuell vurdering av opptjeningsvilkår",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.UT, VilkårType.OPPTJENINGSVILKÅRET,
        SkjermlenkeType.PUNKT_FOR_OPPTJENING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    VURDER_TILBAKETREKK(AksjonspunktKodeDefinisjon.VURDER_TILBAKETREKK_KODE,
        AksjonspunktType.MANUELL, "Vurder tilbaketrekk", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_TILBAKETREKK, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.TILKJENT_YTELSE, TOTRINN, AVVENTER_SAKSBEHANDLER),
    KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST(AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE,
        AksjonspunktType.MANUELL, "Vurder søknadsfrist", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_SØKNADSFRIST, VurderingspunktType.UT,
        VilkårType.SØKNADSFRIST, SkjermlenkeType.SOEKNADSFRIST, TOTRINN, TILBAKE, null, AVVENTER_SAKSBEHANDLER),
    AVKLAR_KOMPLETT_NOK_FOR_BEREGNING(AksjonspunktKodeDefinisjon.AVKLAR_KOMPLETT_NOK_FOR_BEREGNING_KODE,
        AksjonspunktType.MANUELL, "Avklar om inntektsmeldinger kreves for å kunne beregne", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_KOMPLETTHET_BEREGNING, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, ENTRINN, FORBLI, SKAL_IKKE_AVBRYTES, AVVENTER_SAKSBEHANDLER),
    ENDELIG_AVKLAR_KOMPLETT_NOK_FOR_BEREGNING(AksjonspunktKodeDefinisjon.ENDELING_AVKLAR_KOMPLETT_NOK_FOR_BEREGNING_KODE,
        AksjonspunktType.MANUELL, "Endeling avklaring om inntektsmeldinger kreves for å kunne beregne eller om perioden skal avslås", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_KOMPLETTHET_BEREGNING, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, TILBAKE, SKAL_IKKE_AVBRYTES, AVVENTER_SAKSBEHANDLER),

    // Gruppe : 60xx
    @Deprecated(forRemoval = true)
    SØKERS_OPPLYSNINGSPLIKT_OVST(AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST_KODE, AksjonspunktType.SAKSBEHANDLEROVERSTYRING,
        "Saksbehandler initierer kontroll av søkers opplysningsplikt", BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT,
        VurderingspunktType.UT, VilkårType.SØKERSOPPLYSNINGSPLIKT, SkjermlenkeType.OPPLYSNINGSPLIKT, ENTRINN, AVVENTER_SAKSBEHANDLER),
    OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av medlemskapsvilkåret",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.UT, VilkårType.MEDLEMSKAPSVILKÅRET,
        SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, TOTRINN, AVVENTER_SAKSBEHANDLER),
    OVERSTYRING_AV_SØKNADSFRISTVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_SØKNADSFRISTVILKÅRET_KODE, AksjonspunktType.SAKSBEHANDLEROVERSTYRING, "Overstyring av Søknadsfrist",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_SØKNADSFRIST, VurderingspunktType.UT, VilkårType.SØKNADSFRIST,
        SkjermlenkeType.SOEKNADSFRIST, TOTRINN, AVVENTER_SAKSBEHANDLER),
    OVERSTYRING_AV_MEDISINSKESVILKÅRET_UNDER_18(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_MEDISINSKVILKÅR_UNDER_18_KODE, AksjonspunktType.OVERSTYRING,
        "Overstyring av medisinskvilkår for pleietrengende under 18 år",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_MEDISINSKE_VILKÅR, VurderingspunktType.UT, VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR,
        SkjermlenkeType.PUNKT_FOR_MEDISINSK, TOTRINN, AVVENTER_SAKSBEHANDLER),
    OVERSTYRING_AV_MEDISINSKESVILKÅRET_OVER_18(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_MEDISINSKVILKÅR_OVER_18_KODE, AksjonspunktType.OVERSTYRING,
        "Overstyring av medisinskvilkår for pleietrengende 18 år",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_MEDISINSKE_VILKÅR, VurderingspunktType.UT, VilkårType.MEDISINSKEVILKÅR_18_ÅR,
        SkjermlenkeType.PUNKT_FOR_MEDISINSK, TOTRINN, AVVENTER_SAKSBEHANDLER),
    OVERSTYRING_AV_OMSORGEN_FOR(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_OMSORGENFOR_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av Omsorgen for",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_OMSORG_FOR, VurderingspunktType.UT, VilkårType.OMSORGEN_FOR,
        SkjermlenkeType.PUNKT_FOR_OMSORGEN_FOR, TOTRINN, AVVENTER_SAKSBEHANDLER),
    OVERSTYRING_AV_BEREGNING(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNING_KODE,
        AksjonspunktType.OVERSTYRING, "Overstyring av beregning", BehandlingStatus.UTREDES, BehandlingStegType.BEREGN_YTELSE, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    OVERSTYRING_AV_OPPTJENINGSVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_OPPTJENINGSVILKÅRET_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av opptjeningsvilkåret",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.UT, VilkårType.OPPTJENINGSVILKÅRET,
        SkjermlenkeType.PUNKT_FOR_OPPTJENING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    OVERSTYRING_AV_BEREGNINGSAKTIVITETER(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE, AksjonspunktType.OVERSTYRING,
        "Overstyring av beregningsaktiviteter", BehandlingStatus.UTREDES, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING,
        VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    OVERSTYRING_AV_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av beregningsgrunnlag",
        BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_FAKTA_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    OVERSTYRING_AV_UTTAK(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_UTTAK_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av uttak",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_UTTAK_V2, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, AVVENTER_SAKSBEHANDLER),
    OVERSTYRING_AV_K9_VILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_K9_VILKÅRET_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av K9-vilkåret",
        Set.of(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES), BehandlingStegType.MANUELL_VILKÅRSVURDERING, VurderingspunktType.INN, UTEN_VILKÅR,
        SkjermlenkeType.PUNKT_FOR_MAN_VILKÅRSVURDERING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    MANUELL_MARKERING_AV_UTLAND_SAKSTYPE(AksjonspunktKodeDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE_KODE, AksjonspunktType.MANUELL, "Manuell markering av utenlandssak",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTLAND, ENTRINN, AVVENTER_SAKSBEHANDLER),

    // Gruppe : 70xx

    AUTO_MANUELT_SATT_PÅ_VENT(AksjonspunktKodeDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT_KODE, AksjonspunktType.AUTOPUNKT,
        "Manuelt satt på vent", BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE,
        ENTRINN, FORBLI, "P4W", AVVENTER_ANNET),
    AUTO_VENTER_PÅ_KOMPLETT_SØKNAD(AksjonspunktKodeDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD_KODE, AksjonspunktType.AUTOPUNKT,
        "Venter på komplett søknad", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, FORBLI, "P4W", AVVENTER_SØKER),
    AUTO_SATT_PÅ_VENT_REVURDERING(AksjonspunktKodeDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING_KODE, AksjonspunktType.AUTOPUNKT,
        "Satt på vent etter varsel om revurdering", BehandlingStatus.UTREDES, BehandlingStegType.VARSEL_REVURDERING, VurderingspunktType.UT, UTEN_VILKÅR,
        UTEN_SKJERMLENKE, ENTRINN, FORBLI, "P4W", AVVENTER_SØKER),
    AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER_KODE, AksjonspunktType.AUTOPUNKT, "Venter på opptjeningsopplysninger",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.UT, VilkårType.OPPTJENINGSVILKÅRET,
        SkjermlenkeType.FAKTA_FOR_OPPTJENING, ENTRINN, TILBAKE, "P2W", AVVENTER_SØKER),  //TODO?
    VENT_PGA_FOR_TIDLIG_SØKNAD(AksjonspunktKodeDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD_KODE, AksjonspunktType.AUTOPUNKT, "Satt på vent pga for tidlig søknad",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, AVVENTER_ANNET_IKKE_SAKSBEHANDLINGSTID),
    AUTO_VENT_KOMPLETT_OPPDATERING(AksjonspunktKodeDefinisjon.AUTO_VENT_KOMPLETT_OPPDATERING_KODE, AksjonspunktType.AUTOPUNKT, "Vent på oppdatering som passerer kompletthetssjekk",
        BehandlingStatus.UTREDES, BehandlingStegType.FATTE_VEDTAK, VurderingspunktType.INN, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, FORBLI, UTEN_FRIST, AVVENTER_SØKER),
    AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST_KODE, AksjonspunktType.AUTOPUNKT, "Vent på rapporteringsfrist for inntekt",
        BehandlingStatus.UTREDES, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, AVVENTER_SØKER),  //TODO?
    AUTO_VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.AUTO_VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG_KODE, AksjonspunktType.AUTOPUNKT,
        "Autopunkt gradering uten beregningsgrunnlag",
        BehandlingStatus.UTREDES, BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, AVVENTER_TEKNISK_FEIL),
    AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT_KODE, AksjonspunktType.AUTOPUNKT,
        "Vent på siste meldekort for AAP eller DP-mottaker", BehandlingStatus.UTREDES, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT,
        UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, AVVENTER_SØKER),
    AUTO_VENT_INNTEKTSMELDING_MED_UGYLDIG_ARBEIDSFORHOLDID(AksjonspunktKodeDefinisjon.AUTO_VENT_INNTEKTSMELDING_MED_UGYLDIG_ARBEIDSFORHOLDID_KODE, AksjonspunktType.AUTOPUNKT,
        "Vent på ny inntektsmelding med gyldig arbeidsforholdId", BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD, VurderingspunktType.UT,
        UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, AVVENTER_ARBEIDSGIVER),
    AUTO_VENT_MILITÆR_OG_BG_UNDER_3G(AksjonspunktKodeDefinisjon.AUTO_VENT_MILITÆR_OG_BG_UNDER_3G_KODE, AksjonspunktType.AUTOPUNKT,
        "Autopunkt militær i opptjeningsperioden og beregninggrunnlag under 3G", BehandlingStatus.UTREDES,
        BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, AVVENTER_TEKNISK_FEIL),
    AUTO_VENT_GRADERING_FLERE_ARBEIDSFORHOLD(AksjonspunktKodeDefinisjon.AUTO_VENT_GRADERING_FLERE_ARBEIDSFORHOLD_KODE, AksjonspunktType.AUTOPUNKT, "Autopunkt gradering flere arbeidsforhold",
        BehandlingStatus.UTREDES, BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, AVVENTER_TEKNISK_FEIL),
    AUTO_VENT_ETTERLYST_INNTEKTSMELDING(AksjonspunktKodeDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING_KODE, AksjonspunktType.AUTOPUNKT, "Vent på etterlyst inntektsmelding",
        BehandlingStatus.UTREDES, BehandlingStegType.INREG_AVSL, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P4D", AVVENTER_ARBEIDSGIVER),
    AUTO_VENT_BRUKER_70_ÅR(AksjonspunktKodeDefinisjon.AUTO_VENT_BRUKER_70_ÅR, AksjonspunktType.AUTOPUNKT, "Venter på manglende funksjonalitet, bruker 70år ved refusjonskrav",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_OPPTJENING_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P26W", AVVENTER_TEKNISK_FEIL),
    AUTO_VENT_ETTERLYS_IM_FOR_BEREGNING(AksjonspunktKodeDefinisjon.ETTERLYS_IM_FOR_BEREGNING_KODE, AksjonspunktType.AUTOPUNKT, "Vent på etterlyst inntektsmelding",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_KOMPLETTHET_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P2W", AVVENTER_ARBEIDSGIVER),
    AUTO_VENT_ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING(AksjonspunktKodeDefinisjon.ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING_KODE, AksjonspunktType.AUTOPUNKT, "Vent på etterlyst inntektsmelding og/eller tilsvar på varsel om avslag",
        BehandlingStatus.UTREDES, BehandlingStegType.VURDER_KOMPLETTHET_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P3W", AVVENTER_ARBEIDSGIVER),
    AUTO_VENT_PÅ_LOVENDRING_8_41(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_LOVENDRING_8_41_KODE, AksjonspunktType.AUTOPUNKT,
        "Vent på vedtak om lovendring vedrørende beregning av næring i kombinasjon med arbeid eller frilans", BehandlingStatus.UTREDES, BehandlingStegType.PRECONDITION_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR,
        UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, AVVENTER_TEKNISK_FEIL),

    // Gruppe : 80xx (FRISINN)
    AUTO_VENT_FRISINN_BEREGNING(AksjonspunktKodeDefinisjon.AUTO_VENT_FRISINN_BEREGNING, AksjonspunktType.AUTOPUNKT, "Venter på manglende funksjonalitet.",
        BehandlingStatus.UTREDES, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P1D", AVVENTER_TEKNISK_FEIL),
    AUTO_VENT_FRISINN_MANGLENDE_FUNKSJONALITET(AksjonspunktKodeDefinisjon.AUTO_VENT_FRISINN_MANGLENDE_FUNKSJONALITET, AksjonspunktType.AUTOPUNKT, "Venter på manglende funksjonalitet.",
        BehandlingStatus.UTREDES, BehandlingStegType.VARIANT_FILTER, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P26W", AVVENTER_TEKNISK_FEIL),

    AUTO_VENT_FRISINN_ATFL_SAMME_ORG(AksjonspunktKodeDefinisjon.AUTO_VENT_FRISINN_ATFL_SAMME_ORG_KODE, AksjonspunktType.AUTOPUNKT, "Arbeidstaker og frilanser i samme organisasjon, kan ikke beregnes.",
        BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_FAKTA_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P26W", AVVENTER_TEKNISK_FEIL),

    OVERSTYRING_FRISINN_OPPGITT_OPPTJENING(AksjonspunktKodeDefinisjon.OVERSTYRING_FRISINN_OPPGITT_OPPTJENING_KODE, AksjonspunktType.MANUELL, "Saksbehandler overstyrer oppgitt opptjening",
        Set.of(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES), BehandlingStegType.PRECONDITION_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, AVVENTER_SAKSBEHANDLER),

    // Gruppe : 90xx

    KONTROLLER_LEGEERKLÆRING(AksjonspunktKodeDefinisjon.KONTROLLER_LEGEERKLÆRING_KODE,
        AksjonspunktType.MANUELL, "Kontroller legeerklæring", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_MEDISINSKE_VILKÅR, VurderingspunktType.UT,
        VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, SkjermlenkeType.FAKTA_OM_MEDISINSK, ENTRINN, KAN_OVERSTYRE_TOTRINN_ETTER_LUKKING, FORBLI, SKAL_IKKE_AVBRYTES, AVVENTER_SAKSBEHANDLER),
    VURDER_OMSORGEN_FOR(AksjonspunktKodeDefinisjon.AVKLAR_OMSORGEN_FOR_KODE,
        AksjonspunktType.MANUELL, "Omsorgen for", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_OMSORG_FOR, VurderingspunktType.UT,
        VilkårType.OMSORGEN_FOR, SkjermlenkeType.FAKTA_OM_OMSORGENFOR, TOTRINN, AVVENTER_SAKSBEHANDLER),
    VURDER_OMSORGEN_FOR_V2(AksjonspunktKodeDefinisjon.AVKLAR_OMSORGEN_FOR_KODE_V2,
        AksjonspunktType.MANUELL, "Omsorgen for", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_OMSORG_FOR, VurderingspunktType.UT,
        VilkårType.OMSORGEN_FOR, SkjermlenkeType.FAKTA_OM_OMSORGENFOR, TOTRINN, TILBAKE, SKAL_IKKE_AVBRYTES, AVVENTER_SAKSBEHANDLER),
    VENTE_PA_OMSORGENFOR_OMS(AksjonspunktKodeDefinisjon.AUTO_VENTE_PA_OMSORGENFOR_OMS,
        AksjonspunktType.AUTOPUNKT, "Omsorgen for", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_OMSORG_FOR, VurderingspunktType.UT,
        VilkårType.OMSORGEN_FOR, SkjermlenkeType.FAKTA_OM_OMSORGENFOR, ENTRINN, TILBAKE, "P1D", AVVENTER_SAKSBEHANDLER),
    VURDER_ÅRSKVANTUM_KVOTE(AksjonspunktKodeDefinisjon.VURDER_ÅRSKVANTUM_KVOTE,
        AksjonspunktType.MANUELL, "Årskvantum", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_ÅRSKVANTUM, ENTRINN, FORBLI, null, AVVENTER_SAKSBEHANDLER),
    VURDER_ÅRSKVANTUM_DOK(AksjonspunktKodeDefinisjon.VURDER_ÅRSKVANTUM_DOK,
        AksjonspunktType.MANUELL, "Årskvantum dokumentasjon", BehandlingStatus.UTREDES, BehandlingStegType.BEKREFT_UTTAK, VurderingspunktType.INN,
        UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_ÅRSKVANTUM, TOTRINN, TILBAKE, null, AVVENTER_SAKSBEHANDLER),
    VURDER_OMS_UTVIDET_RETT(AksjonspunktKodeDefinisjon.VURDER_OMS_UTVIDET_RETT,
        AksjonspunktType.MANUELL, "Utvidet Rett", BehandlingStatus.UTREDES, BehandlingStegType.MANUELL_VILKÅRSVURDERING, VurderingspunktType.UT,
        VilkårType.UTVIDETRETT, SkjermlenkeType.FAKTA_OM_UTVIDETRETT, TOTRINN, AVVENTER_SAKSBEHANDLER),
    ÅRSKVANTUM_FOSTERBARN(AksjonspunktKodeDefinisjon.ÅRSKVANTUM_FOSTERBARN,
        AksjonspunktType.MANUELL, "Årskvantum", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_ÅRSKVANTUM, ENTRINN, FORBLI, null, AVVENTER_SAKSBEHANDLER),
    VURDER_ALDERSVILKÅR_BARN(AksjonspunktKodeDefinisjon.VURDER_ALDERSVILKÅR_BARN,
        AksjonspunktType.MANUELL, "Vurder aldersvilkår barn", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_ALDERSVILKÅR_BARN, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.PUNKT_FOR_ALDERSVILKÅR_BARN, TOTRINN, FORBLI, null, AVVENTER_SAKSBEHANDLER),
    OVERSTYR_BEREGNING_INPUT(AksjonspunktKodeDefinisjon.OVERSTYR_BEREGNING_INPUT,
        AksjonspunktType.MANUELL, "Overstyr input beregning", BehandlingStatus.UTREDES, BehandlingStegType.PRECONDITION_BEREGNING, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.OVERSTYR_INPUT_BEREGNING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    AUTO_VENT_PÅ_KOMPLETT_SØKNAD_VED_OVERGANG_FRA_INFOTRYGD(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_KOMPLETT_SØKNAD_FOR_PERIODE,
        AksjonspunktType.AUTOPUNKT, "Venter på punsjet søknad", BehandlingStatus.UTREDES, BehandlingStegType.PRECONDITION_BEREGNING, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.OVERSTYR_INPUT_BEREGNING, ENTRINN, TILBAKE, AVBRYTES, AVVENTER_SAKSBEHANDLER),
    @Deprecated
    TRENGER_SØKNAD_FOR_INFOTRYGD_PERIODE(AksjonspunktKodeDefinisjon.TRENGER_SØKNAD_FOR_INFOTRYGD_PERIODE,
        AksjonspunktType.MANUELL, "Mangler søknad for periode i inneværende år", BehandlingStatus.UTREDES, BehandlingStegType.OVERGANG_FRA_INFOTRYGD, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.INFOTRYGD_MIGRERING, TOTRINN, AVVENTER_SAKSBEHANDLER),
    @Deprecated
    TRENGER_SØKNAD_FOR_INFOTRYGD_PERIODE_ANNEN_PART(AksjonspunktKodeDefinisjon.TRENGER_SØKNAD_FOR_INFOTRYGD_PERIODE_ANNEN_PART,
        AksjonspunktType.MANUELL, "Mangler søknad for annen parts periode", BehandlingStatus.UTREDES, BehandlingStegType.OVERGANG_FRA_INFOTRYGD, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.INFOTRYGD_MIGRERING, TOTRINN, AVVENTER_SAKSBEHANDLER),

    // Gruppe: 92xx - Pleiepenger
    VURDER_NATTEVÅK(AksjonspunktKodeDefinisjon.VURDER_NATTEVÅK, AksjonspunktType.MANUELL,
        "Vurder nattevåk og beredskap", BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_FAKTA_UTTAK,
        VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.VURDER_NATTEVÅK, TOTRINN, TILBAKE, SKAL_IKKE_AVBRYTES, AVVENTER_SAKSBEHANDLER),
    VURDER_BEREDSKAP(AksjonspunktKodeDefinisjon.VURDER_BEREDSKAP, AksjonspunktType.MANUELL,
        "Vurder nattevåk og beredskap", BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_FAKTA_UTTAK,
        VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.VURDER_BEREDSKAP, TOTRINN, TILBAKE, SKAL_IKKE_AVBRYTES, AVVENTER_SAKSBEHANDLER),
    VURDER_RETT_ETTER_PLEIETRENGENDES_DØD(AksjonspunktKodeDefinisjon.VURDER_RETT_ETTER_PLEIETRENGENDES_DØD, AksjonspunktType.MANUELL,
        "Vurder rett etter pleietrengendes død", BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_FAKTA_UTTAK,
        VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.VURDER_RETT_ETTER_PLEIETRENGENDES_DØD, TOTRINN, TILBAKE, SKAL_IKKE_AVBRYTES, AVVENTER_SAKSBEHANDLER),
    MANGLER_AKTIVITETER(AksjonspunktKodeDefinisjon.MANGLER_AKTIVITETER, AksjonspunktType.MANUELL,
        "Bruker har ikke oppgitt alle arbeidsgiverne sine", BehandlingStatus.UTREDES, BehandlingStegType.KONTROLLER_FAKTA_UTTAK,
        VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, ENTRINN, FORBLI, AVBRYTES, AVVENTER_SAKSBEHANDLER),
    VENT_ANNEN_PSB_SAK(AksjonspunktKodeDefinisjon.VENT_ANNEN_PSB_SAK_KODE, AksjonspunktType.MANUELL,
        "En annen sak tilknyttet barnet må behandles frem til uttak, eller besluttes, før denne saken kan behandles videre.", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_UTTAK_V2, VurderingspunktType.UT, UTEN_VILKÅR,
        UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, AVVENTER_SAKSBEHANDLER),
    VURDER_DATO_NY_REGEL_UTTAK(AksjonspunktKodeDefinisjon.VURDER_DATO_NY_REGEL_UTTAK, AksjonspunktType.MANUELL,
        "Vurder hvilken dato ny regel for utbetalingsgrad i uttak skal gjelde fra.", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_STARTDATO_UTTAKSREGLER, VurderingspunktType.UT, UTEN_VILKÅR,
        SkjermlenkeType.UTTAK, ENTRINN, TILBAKE, AVBRYTES, AVVENTER_SAKSBEHANDLER),
    VURDER_OVERLAPPENDE_SØSKENSAKER(AksjonspunktKodeDefinisjon.VURDER_OVERLAPPENDE_SØSKENSAK_KODE, AksjonspunktType.MANUELL,
        "Vurder overlappende søskensaker.", BehandlingStatus.UTREDES, BehandlingStegType.VURDER_UTTAK_V2, VurderingspunktType.UT, UTEN_VILKÅR,
        SkjermlenkeType.UTTAK, TOTRINN, AVVENTER_SAKSBEHANDLER),

    // Gruppe: 93xx - Opplæringspenger
    VURDER_INSTITUSJON(AksjonspunktKodeDefinisjon.VURDER_INSTITUSJON, AksjonspunktType.MANUELL,
        "Vurder om institusjonen er godkjent", BehandlingStatus.UTREDES,
        BehandlingStegType.VURDER_INSTITUSJON_VILKÅR, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UDEFINERT, ENTRINN, TILBAKE, AVBRYTES, AVVENTER_SAKSBEHANDLER),
    VURDER_NØDVENDIGHET(AksjonspunktKodeDefinisjon.VURDER_NØDVENDIGHET, AksjonspunktType.MANUELL,
        "Vurder om opplæringen er nødvendig for å behandle og ta seg av barnet", BehandlingStatus.UTREDES,
        BehandlingStegType.VURDER_NØDVENDIGHETS_VILKÅR, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UDEFINERT, ENTRINN, TILBAKE, AVBRYTES, AVVENTER_SAKSBEHANDLER),
    VURDER_GJENNOMGÅTT_OPPLÆRING(AksjonspunktKodeDefinisjon.VURDER_GJENNOMGÅTT_OPPLÆRING, AksjonspunktType.MANUELL,
        "Vurder om opplæringen er gjennomgått", BehandlingStatus.UTREDES,
        BehandlingStegType.VURDER_GJENNOMGÅTT_OPPLÆRING, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UDEFINERT, TOTRINN, TILBAKE, AVBRYTES, AVVENTER_SAKSBEHANDLER),
    VURDER_REISETID(AksjonspunktKodeDefinisjon.VURDER_REISETID, AksjonspunktType.MANUELL,
        "Vurder reisetid", BehandlingStatus.UTREDES,
        BehandlingStegType.VURDER_GJENNOMGÅTT_OPPLÆRING, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UDEFINERT, TOTRINN, TILBAKE, AVBRYTES, AVVENTER_SAKSBEHANDLER),

    // Gruppe : 999x
    AUTO_VENT_FILTER_MANGLENDE_FUNKSJONALITET(AksjonspunktKodeDefinisjon.AUTO_VENT_FILTER_MANGLENDE_FUNKSJONALITET, AksjonspunktType.AUTOPUNKT, "Venter på manglende funksjonalitet.",
        BehandlingStatus.UTREDES, BehandlingStegType.VARIANT_FILTER, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P26W", AVVENTER_TEKNISK_FEIL),

    UNDEFINED,

    ;

    static final String KODEVERK = "AKSJONSPUNKT_DEF";

    /**
     * Liste av utgåtte aksjonspunkt. Ikke gjenbruk samme kode.
     */
    private static final Map<String, String> UTGÅTT = Map.of(
        "5022", "AVKLAR_FAKTA_FOR_PERSONSTATUS",
        "7007", "VENT_PÅ_SCANNING");

    private static final Map<String, AksjonspunktDefinisjon> KODER = new LinkedHashMap<>();


    static {
        for (var v : UTGÅTT.keySet()) {
            if (KODER.putIfAbsent(v, UNDEFINED) != null) {
                throw new IllegalArgumentException("Duplikat : " + v);
            }
        }
        // valider ingen unmapped koder
        var sjekkKodeBrukMap = new TreeMap<>(AksjonspunktKodeDefinisjon.KODER);

        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode + ", mulig utgått?");
            }
            if (v.kode != null) {
                sjekkKodeBrukMap.remove(v.kode);
            }
        }

        if (!sjekkKodeBrukMap.isEmpty()) {
            System.out.printf("Ubrukt sjekk: Har koder definert i %s som ikke er i bruk i %s: %s\n", AksjonspunktKodeDefinisjon.class, AksjonspunktDefinisjon.class, sjekkKodeBrukMap);
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

    @JsonIgnore
    private boolean kanOverstyreTotrinnEtterLukking = false;

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
    private VurderingspunktType vurderingspunktType;

    @JsonIgnore
    private boolean erUtgått = false;

    private String kode;

    @JsonIgnore
    private Set<BehandlingStatus> behandlingStatus;

    @JsonIgnore
    private boolean skalAvbrytesVedTilbakeføring = true;

    @JsonIgnore
    private Ventekategori defaultVentekategori;

    AksjonspunktDefinisjon() {
        // for hibernate
    }

    /**
     * Brukes for utgåtte aksjonspunkt. Disse skal ikke kunne gjenoppstå, men må kunne leses
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
                                   BehandlingStatus behandlingStatus,
                                   BehandlingStegType behandlingStegType,
                                   VurderingspunktType vurderingspunktType,
                                   VilkårType vilkårType,
                                   SkjermlenkeType skjermlenkeType,
                                   boolean defaultTotrinnBehandling,
                                   Ventekategori defaultVentekategori) {
        this.kode = Objects.requireNonNull(kode);
        this.behandlingStatus = Set.of(behandlingStatus);
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStegType = behandlingStegType;
        this.vurderingspunktType = vurderingspunktType;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = false;
        this.fristPeriode = null;
        this.defaultVentekategori = defaultVentekategori;
    }

    // Bruk for ordinære aksjonspunkt og overstyring
    private AksjonspunktDefinisjon(String kode,
                                   AksjonspunktType aksjonspunktType,
                                   String navn,
                                   Set<BehandlingStatus> behandlingStatus,
                                   BehandlingStegType behandlingStegType,
                                   VurderingspunktType vurderingspunktType,
                                   VilkårType vilkårType,
                                   SkjermlenkeType skjermlenkeType,
                                   boolean defaultTotrinnBehandling,
                                   Ventekategori defaultVentekategori) {
        this.kode = Objects.requireNonNull(kode);
        this.behandlingStatus = behandlingStatus;
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStegType = behandlingStegType;
        this.vurderingspunktType = vurderingspunktType;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = false;
        this.fristPeriode = null;
        this.defaultVentekategori = defaultVentekategori;
    }

    // Bruk for autopunkt i 7nnn serien
    private AksjonspunktDefinisjon(String kode,
                                   AksjonspunktType aksjonspunktType,
                                   String navn,
                                   BehandlingStatus behandlingStatus,
                                   BehandlingStegType behandlingStegType,
                                   VurderingspunktType vurderingspunktType,
                                   VilkårType vilkårType,
                                   SkjermlenkeType skjermlenkeType,
                                   boolean defaultTotrinnBehandling,
                                   boolean tilbakehoppVedGjenopptakelse,
                                   String fristPeriode,
                                   Ventekategori defaultVentekategori) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStatus = Set.of(behandlingStatus);
        this.behandlingStegType = behandlingStegType;
        this.vurderingspunktType = vurderingspunktType;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = tilbakehoppVedGjenopptakelse;
        this.fristPeriode = fristPeriode;
        this.defaultVentekategori = defaultVentekategori;
    }

    private AksjonspunktDefinisjon(String kode,
                                   AksjonspunktType aksjonspunktType,
                                   String navn,
                                   BehandlingStatus behandlingStatus,
                                   BehandlingStegType behandlingStegType,
                                   VurderingspunktType vurderingspunktType,
                                   VilkårType vilkårType,
                                   SkjermlenkeType skjermlenkeType,
                                   boolean defaultTotrinnBehandling,
                                   boolean tilbakehoppVedGjenopptakelse,
                                   boolean skalAvbrytesVedTilbakeføring,
                                   Ventekategori defaultVentekategori) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStatus = Set.of(behandlingStatus);
        this.behandlingStegType = behandlingStegType;
        this.vurderingspunktType = vurderingspunktType;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = tilbakehoppVedGjenopptakelse;
        this.skalAvbrytesVedTilbakeføring = skalAvbrytesVedTilbakeføring;
        this.defaultVentekategori = defaultVentekategori;
    }

    private AksjonspunktDefinisjon(String kode,
                                   AksjonspunktType aksjonspunktType,
                                   String navn,
                                   BehandlingStatus behandlingStatus,
                                   BehandlingStegType behandlingStegType,
                                   VurderingspunktType vurderingspunktType,
                                   VilkårType vilkårType,
                                   SkjermlenkeType skjermlenkeType,
                                   boolean defaultTotrinnBehandling,
                                   boolean kanOverstyreTotrinnEtterLukking,
                                   boolean tilbakehoppVedGjenopptakelse,
                                   boolean skalAvbrytesVedTilbakeføring,
                                   Ventekategori defaultVentekategori) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStatus = Set.of(behandlingStatus);
        this.behandlingStegType = behandlingStegType;
        this.vurderingspunktType = vurderingspunktType;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.kanOverstyreTotrinnEtterLukking = kanOverstyreTotrinnEtterLukking;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = tilbakehoppVedGjenopptakelse;
        this.skalAvbrytesVedTilbakeføring = skalAvbrytesVedTilbakeføring;
        this.defaultVentekategori = defaultVentekategori;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static AksjonspunktDefinisjon fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(AksjonspunktDefinisjon.class, node, "kode");
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

    public static void main(String[] args) {

        var sb = new StringBuilder(100 * 1000);

        sb.append("kode,type,navn,defaultTotrinn,behandlingSteg\n");

        for (var v : values()) {
            var k = v.getKode();

            var sb2 = new StringBuilder(300);
            sb2.append(k).append(",");
            sb2.append(v.aksjonspunktType.getKode()).append(",");
            String navn = v.navn == null ? "" : "\"" + v.navn + "\"";
            sb2.append(navn).append(",");
            sb2.append(v.defaultTotrinnBehandling).append(",");
            sb2.append(v.behandlingStegType == null ? "" : v.behandlingStegType.getKode() + (v.vurderingspunktType == null ? "" : ":" + v.vurderingspunktType));

            sb.append(sb2).append("\n");

        }

        System.out.println(sb);
    }

    /**
     * @deprecated Bruk heller
     * {@link no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder#medSkjermlenke(SkjermlenkeType)}
     * direkte og unngå å slå opp fra aksjonspunktdefinisjon
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

    public Ventekategori getDefaultVentekategori() {
        return defaultVentekategori;
    }


    @Override
    public String getNavn() {
        return navn;
    }

    public boolean validerGyldigStatusEndring(AksjonspunktStatus aksjonspunktStatus, BehandlingStatus status) {
        return behandlingStatus.contains(status) || isFatterVedtak(aksjonspunktStatus, status);
    }

    private boolean isFatterVedtak(AksjonspunktStatus aksjonspunktStatus, BehandlingStatus status) {
        // I FatterVedtak kan beslutter reåpne (derav OPPRETTET) eksisterende aksjonspunkter før det sendes tilbake til saksbehandler
        return Objects.equals(BehandlingStatus.FATTER_VEDTAK, status)
            && Objects.equals(aksjonspunktStatus, AksjonspunktStatus.OPPRETTET);
    }

    @JsonProperty(value = "kode")
    @Override
    public String getKode() {
        return kode;
    }

    public boolean getSkalAvbrytesVedTilbakeføring() {
        return skalAvbrytesVedTilbakeføring;
    }

    public BehandlingStegType getBehandlingSteg() {
        return behandlingStegType;
    }

    public VurderingspunktType getVurderingspunktType() {
        return vurderingspunktType;
    }

    public Set<BehandlingStatus> getGyldigBehandlingStatus() {
        return behandlingStatus;
    }

    public boolean kanOverstyreTotrinnEtterLukking() {
        return kanOverstyreTotrinnEtterLukking;
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

    /**
     * toString is set to output the kode value of the enum instead of the default that is the enum name.
     * This makes the generated openapi spec correct when the enum is used as a query param. Without this the generated
     * spec incorrectly specifies that it is the enum name string that should be used as input.
     */
    @Override
    public String toString() {
        return this.getKode();
    }

}
