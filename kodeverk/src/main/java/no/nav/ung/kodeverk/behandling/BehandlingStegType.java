package no.nav.ung.kodeverk.behandling;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static no.nav.ung.kodeverk.behandling.BehandlingStatus.IVERKSETTER_VEDTAK;
import static no.nav.ung.kodeverk.behandling.BehandlingStatus.UTREDES;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BehandlingStegType implements Kodeverdi {

    BEREGN_YTELSE("BERYT", "Beregn ytelse", UTREDES),
    PRECONDITION_BEREGNING("PRECONDITION_BERGRUNN", "Vurderer om det er mulig å beregne", UTREDES),
    FASTSETT_BEREGNINGSGRUNNLAG("FAST_BERGRUNN", "Fastsett beregningsgrunnlag", UTREDES),
    FASTSETT_OPPTJENINGSPERIODE("VURDER_OPPTJ_PERIODE", "Vurder Opptjening Periode", UTREDES),
    FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING("FASTSETT_STP_BER", "Fastsett skjæringstidspunkt beregning", UTREDES),
    FATTE_VEDTAK("FVEDSTEG", "Fatte Vedtak", BehandlingStatus.FATTER_VEDTAK),
    VURDER_VILKAR_BERGRUNN("VURDER_VILKAR_BERGRUNN", "Vurder beregingsgrunnlagsvilkåret", UTREDES),
    VURDER_REF_BERGRUNN("VURDER_REF_BERGRUNN", "Vurder refusjon for beregningsgrunnlaget", UTREDES),
    FORDEL_BEREGNINGSGRUNNLAG("FORDEL_BERGRUNN", "Fordel beregningsgrunnlag", UTREDES),
    FORESLÅ_BEHANDLINGSRESULTAT("FORBRES", "Foreslå behandlingsresultat", UTREDES),
    FORESLÅ_BEREGNINGSGRUNNLAG("FORS_BERGRUNN", "Foreslå beregningsgrunnlag", UTREDES),
    FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG("FORS_BERGRUNN_2", "Foreslå beregningsgrunnlag del 2", UTREDES),
    VURDER_MANUELT_BREV("VURDER_MANUELT_BREV", "Vurder manuelt brev", UTREDES),
    FORESLÅ_VEDTAK("FORVEDSTEG", "Foreslå vedtak", UTREDES),
    HINDRE_TILBAKETREKK("BERYT_OPPDRAG", "Hindre tilbaketrekk", UTREDES),
    VURDER_SØKNADSFRIST("VURDER_SØKNADSFRIST", "Vurder søknadsfrist", UTREDES),
    INIT_PERIODER("INIT_PERIODER", "Start", UTREDES),
    INIT_VILKÅR("INIT_VILKÅR", "Initier vilkår for behandling", UTREDES),
    INNHENT_PERSONOPPLYSNINGER("INPER", "Innhent personopplysninger", UTREDES),
    INNHENT_REGISTEROPP("INREG", "Innhent registeropplysninger - innledende oppgaver", UTREDES),
    INNHENT_SØKNADOPP("INSØK", "Innhent søknadsopplysninger", UTREDES),
    IVERKSETT_VEDTAK("IVEDSTEG", "Iverksett Vedtak", IVERKSETTER_VEDTAK),
    KONTROLLER_FAKTA("KOFAK", "Kontroller Fakta", UTREDES),
    KONTROLLER_FAKTA_ARBEIDSFORHOLD("KOARB", "Kontroller arbeidsforhold", UTREDES),
    VURDER_KOMPLETTHET_BEREGNING("KOMPLETT_FOR_BEREGNING", "Opplysninger til beregning", UTREDES),
    INNHENT_INNTEKTSMELDING("INNINN", "Innhent inntektsmelding", UTREDES),
    KONTROLLER_FAKTA_BEREGNING("KOFAKBER", "Kontroller fakta for beregning", UTREDES),
    KONTROLLER_FAKTA_UTTAK("KOFAKUT", "Kontroller fakta for uttak", UTREDES),
    KONTROLLER_LØPENDE_MEDLEMSKAP("KOFAK_LOP_MEDL", "Kontroller løpende medlemskap", UTREDES),
    KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT("VURDEROP", "Kontrollerer søkers opplysningsplikt", UTREDES),
    SIMULER_OPPDRAG("SIMOPP", "Simuler oppdrag", UTREDES),
    START_STEG("START", "Start behandling prosess", UTREDES),
    KONTROLLER_UNGDOMSPROGRAM("KONTROLLER_UNGDOMSPROGRAM", "Kontroller endringer i ungdomsprogram", UTREDES),
    VULOMED("VULOMED", "Vurder løpende medlemskap", UTREDES),
    /**
     * @deprecated pt. ikke i bruk i K9
     */
    @Deprecated(forRemoval = true)
    VURDER_FARESIGNALER("VURDER_FARESIGNALER", "Vurder faresignaler", UTREDES),
    VURDER_INNSYN("VURDINNSYN", "Vurder innsynskrav", UTREDES),
    VURDER_KOMPLETTHET("VURDERKOMPLETT", "Vurder kompletthet", UTREDES),
    VURDER_STARTDATO_UTTAKSREGLER("VURDER_STARTDATO_UTTAKSREGLER", "Vurder startdato uttaksregler", UTREDES),
    VURDER_TILKOMMET_INNTEKT("VURDER_TILKOMMET_INNTEKT", "Vurder tilkommet inntekt", UTREDES),
    POSTCONDITION_KOMPLETTHET("POSTCONDITION_KOMPLETTHET", "Sjekker om det er mulig å fortsette etter kompletthetssjekk", UTREDES),
    VARIANT_FILTER("VARIANT_FILTER", "Filtrer ut varianter", UTREDES),
    VURDER_MEDISINSKE_VILKÅR("VURDER_MEDISINSK", "Vurder medisinske vilkår", UTREDES),
    VURDER_NØDVENDIGHETS_VILKÅR("VURDER_NODVENDIGHET", "Vurder nødvendighetens vilkår", UTREDES),
    VURDER_INSTITUSJON_VILKÅR("VURDER_INSTITUSJON", "Vurder krav til institusjonen", UTREDES),
    VURDER_GJENNOMGÅTT_OPPLÆRING("VURDER_GJENNOMGATT_OPPLAERING", "Vurder gjennomgått opplæring", UTREDES),
    POST_VURDER_MEDISINSKVILKÅR("POST_MEDISINSK", "Post vurder medisinskvilkår", UTREDES),
    VURDER_MEDLEMSKAPVILKÅR("VURDERMV", "Vurder medlemskapvilkår", UTREDES),
    VURDER_OMSORG_FOR("VURDER_OMSORG_FOR", "Vurder omsorgen for", UTREDES),
    ALDERSVILKÅRET("VURDER_ALDER", "Vurder søkers alder", UTREDES),
    VURDER_ALDERSVILKÅR_BARN("VURDER_ALDER_BARN", "Vurder barnets alder", UTREDES),
    VURDER_OPPTJENING_FAKTA("VURDER_OPPTJ_FAKTA", "Vurder opptjeningfakta", UTREDES),
    VURDER_OPPTJENINGSVILKÅR("VURDER_OPPTJ", "Vurder opptjeningsvilkåret", UTREDES),
    VURDER_TILBAKETREKK("VURDER_TILBAKETREKK", "Vurder tilbaketrekk", UTREDES),
    VURDER_UTLAND("VURDER_UTLAND", "Vurder utland (SED)", UTREDES),
    VURDER_UTTAK("VURDER_UTTAK", "Vurder antall dager og uttak", UTREDES),
    VURDER_UTTAK_V2("VURDER_UTTAK_V2", "Uttak", UTREDES),
    BEKREFT_UTTAK("BEKREFT_UTTAK", "Bekreft uttak", UTREDES),
    MANUELL_VILKÅRSVURDERING("MANUELL_VILKÅRSVURDERING", "Manuell vilkårsvurdering", UTREDES),
    MANUELL_TILKJENNING_YTELSE("MANUELL_TILKJENNING_YTELSE", "Manuell tilkjenning av ytelse", UTREDES),
    OVERGANG_FRA_INFOTRYGD("OVERGANG_FRA_INFOTRYGD", "Direkte overgang fra infotrygd", UTREDES),
    VURDER_UNGDOMSPROGRAMVILKÅR("VURDER_UNGDOMSPROGRAMVILKÅR", "Vurder deltakelse i ungdomsprogrammet", UTREDES),
    UNGDOMSYTELSE_BEREGNING("UNGDOMSYTELSE_BEREGNING", "Beregner sats for ungdomsytelsen", UTREDES),
    KONTROLLER_REGISTER_INNTEKT("KONTROLLER_REGISTER_INNTEKT", "Kontroller brukers rapporterte inntekt mot registerinntekt", UTREDES);

    private static final Map<String, BehandlingStegType> KODER = new LinkedHashMap<>();

    static final String KODEVERK = "BEHANDLING_STEG_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    /**
     * Definisjon av hvilken status behandlingen skal rapporteres som når dette steget er aktivt.
     */
    @JsonIgnore
    private BehandlingStatus definertBehandlingStatus;

    private String kode;

    @JsonIgnore
    private String navn;

    private BehandlingStegType(String kode) {
        this.kode = kode;
    }

    private BehandlingStegType(String kode, String navn, BehandlingStatus definertBehandlingStatus) {
        this.kode = kode;
        this.navn = navn;
        this.definertBehandlingStatus = definertBehandlingStatus;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static BehandlingStegType fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(BehandlingStegType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingStegType: for input " + node);
        }
        return ad;
    }

    public static Map<String, BehandlingStegType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktDefinisjoner() {
        return AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(this);
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktDefinisjonerUtgang() {
        return AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(this);
    }

    public BehandlingStatus getDefinertBehandlingStatus() {
        return definertBehandlingStatus;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
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
