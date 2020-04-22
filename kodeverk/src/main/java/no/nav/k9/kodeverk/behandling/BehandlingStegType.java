package no.nav.k9.kodeverk.behandling;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderingspunktType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static no.nav.k9.kodeverk.behandling.BehandlingStatus.IVERKSETTER_VEDTAK;
import static no.nav.k9.kodeverk.behandling.BehandlingStatus.UTREDES;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BehandlingStegType implements Kodeverdi {

    BEREGN_YTELSE("BERYT", "Beregn ytelse", UTREDES),
    FASTSETT_BEREGNINGSGRUNNLAG("FAST_BERGRUNN", "Fastsett beregningsgrunnlag", UTREDES),
    FASTSETT_OPPTJENINGSPERIODE("VURDER_OPPTJ_PERIODE", "Vurder Opptjening Periode", UTREDES),
    FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING("FASTSETT_STP_BER", "Fastsett skjæringstidspunkt beregning", UTREDES),
    FATTE_VEDTAK("FVEDSTEG", "Fatte Vedtak", BehandlingStatus.FATTER_VEDTAK),
    FORDEL_BEREGNINGSGRUNNLAG("FORDEL_BERGRUNN", "Fordel beregningsgrunnlag", UTREDES),
    FORESLÅ_BEHANDLINGSRESULTAT("FORBRES", "Foreslå behandlingsresultat", UTREDES),
    FORESLÅ_BEREGNINGSGRUNNLAG("FORS_BERGRUNN", "Foreslå beregningsgrunnlag", UTREDES),
    FORESLÅ_VEDTAK("FORVEDSTEG", "Foreslå vedtak", UTREDES),
    HINDRE_TILBAKETREKK("BERYT_OPPDRAG", "Hindre tilbaketrekk", UTREDES),
    INIT_PERIODER("INIT_PERIODER", "Initier perioder for behandling", UTREDES),
    INIT_VILKÅR("INIT_VILKÅR", "Initier vilkår for behandling", UTREDES),
    INNHENT_PERSONOPPLYSNINGER("INPER", "Innhent personopplysninger", UTREDES),
    INNHENT_REGISTEROPP("INREG", "Innhent registeropplysninger - innledende oppgaver", UTREDES),
    INNHENT_SØKNADOPP("INSØK", "Innhent søknadsopplysninger", UTREDES),
    INREG_AVSL("INREG_AVSL", "Innhent registeropplysninger - resterende oppgaver", UTREDES),
    IVERKSETT_VEDTAK("IVEDSTEG", "Iverksett Vedtak", IVERKSETTER_VEDTAK),
    KONTROLLER_FAKTA("KOFAK", "Kontroller Fakta", UTREDES),
    KONTROLLER_FAKTA_ARBEIDSFORHOLD("KOARB", "Kontroller arbeidsforhold", UTREDES),
    KONTROLLER_FAKTA_BEREGNING("KOFAKBER", "Kontroller fakta for beregning", UTREDES),
    KONTROLLER_FAKTA_UTTAK("KOFAKUT", "Kontroller fakta for uttak", UTREDES),
    KONTROLLER_LØPENDE_MEDLEMSKAP("KOFAK_LOP_MEDL", "Kontroller løpende medlemskap", UTREDES),
    KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT("VURDEROP", "Kontrollerer søkers opplysningsplikt", UTREDES),
    SIMULER_OPPDRAG("SIMOPP", "Simuler oppdrag", UTREDES),
    SØKNADSFRIST("SØKNADSFRIST", "Vurder søknadsfrist foreldrepenger", UTREDES),
    START_STEG("START", "Start behandling prosess", UTREDES),
    VARSEL_REVURDERING("VRSLREV", "Varsel om revurdering", UTREDES),
    VULOMED("VULOMED", "Vurder løpende medlemskap", UTREDES),
    VURDER_FARESIGNALER("VURDER_FARESIGNALER", "Vurder faresignaler", UTREDES),
    VURDER_INNSYN("VURDINNSYN", "Vurder innsynskrav", UTREDES),
    VURDER_KOMPLETTHET("VURDERKOMPLETT", "Vurder kompletthet", UTREDES),
    VURDER_MEDISINSKVILKÅR("VURDER_MEDISINSK", "Vurder medisinskvilkår", UTREDES),
    VURDER_MEDLEMSKAPVILKÅR("VURDERMV", "Vurder medlemskapvilkår", UTREDES),
    VURDER_OMSORG_FOR("VURDER_OMSORG_FOR", "Vurder omsorgen for", UTREDES),
    VURDER_ÅRSKVANTUM("VURDER_ÅRSKVANTUM", "Vurder årskvantum", UTREDES),
    VURDER_OPPTJENING_FAKTA("VURDER_OPPTJ_FAKTA", "Vurder opptjeningfakta", UTREDES),
    VURDER_OPPTJENINGSVILKÅR("VURDER_OPPTJ", "Vurder opptjeningsvilkåret", UTREDES),
    VURDER_TILBAKETREKK("VURDER_TILBAKETREKK", "Vurder tilbaketrekk", UTREDES),
    VURDER_UTLAND("VURDER_UTLAND", "Vurder utland (SED)", UTREDES),
    VURDER_UTTAK("VURDER_UTTAK", "Vurder uttaksvilkår", UTREDES),
    ;

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

    @JsonCreator
    public static BehandlingStegType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingStegType: " + kode);
        }
        return ad;
    }

    public static Map<String, BehandlingStegType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktDefinisjoner(VurderingspunktType type) {
        return AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(this, type);
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktDefinisjonerInngang() {
        return AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(this, VurderingspunktType.INN);
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktDefinisjonerUtgang() {
        return AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(this, VurderingspunktType.UT);
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

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

}
