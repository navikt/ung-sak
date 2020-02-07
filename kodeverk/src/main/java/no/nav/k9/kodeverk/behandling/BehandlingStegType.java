package no.nav.k9.kodeverk.behandling;

import static no.nav.k9.kodeverk.behandling.BehandlingStatus.IVERKSETTER_VEDTAK;
import static no.nav.k9.kodeverk.behandling.BehandlingStatus.UTREDES;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderingspunktType;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BehandlingStegType implements Kodeverdi {

    // Steg koder som deles av alle ytelser
    VARSEL_REVURDERING("VRSLREV", "Varsel om revurdering", UTREDES),
    INNHENT_SØKNADOPP("INSØK", "Innhent søknadsopplysninger", UTREDES),
    INNHENT_REGISTEROPP("INREG", "Innhent registeropplysninger - innledende oppgaver", UTREDES),
    KONTROLLER_FAKTA("KOFAK", "Kontroller Fakta", UTREDES),
    SØKERS_RELASJON_TIL_BARN("VURDERSRB", "Vurder søkers relasjon til barnet", UTREDES),
    VURDER_MEDLEMSKAPVILKÅR("VURDERMV", "Vurder medlemskapvilkår", UTREDES),
    BEREGN_YTELSE("BERYT", "Beregn ytelse", UTREDES),
    FATTE_VEDTAK("FVEDSTEG", "Fatte Vedtak", BehandlingStatus.FATTER_VEDTAK),
    IVERKSETT_VEDTAK("IVEDSTEG", "Iverksett Vedtak", IVERKSETTER_VEDTAK),
    FORESLÅ_BEHANDLINGSRESULTAT("FORBRES", "Foreslå behandlingsresultat", UTREDES),
    SIMULER_OPPDRAG("SIMOPP", "Simuler oppdrag", UTREDES),
    FORESLÅ_VEDTAK("FORVEDSTEG", "Foreslå vedtak", UTREDES),
    KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT("VURDEROP", "Kontrollerer søkers opplysningsplikt", UTREDES),
    REGISTRER_SØKNAD("REGSØK", "Registrer søknad", UTREDES),
    VURDER_INNSYN("VURDINNSYN", "Vurder innsynskrav", UTREDES),
    INNHENT_PERSONOPPLYSNINGER("INPER", "Innhent personopplysninger", UTREDES),
    VURDER_KOMPLETTHET("VURDERKOMPLETT", "Vurder kompletthet", UTREDES),
    VURDER_SAMLET("VURDERSAMLET", "Vurder vilkår samlet", UTREDES),
    VURDER_TILBAKETREKK("VURDER_TILBAKETREKK", "Vurder tilbaketrekk", UTREDES),
    VURDER_FARESIGNALER("VURDER_FARESIGNALER", "Vurder faresignaler", UTREDES),

    // Kun for Foreldrepenger
    VURDER_UTTAK("VURDER_UTTAK", "Vurder uttaksvilkår", UTREDES),
    VURDER_OPPTJENINGSVILKÅR("VURDER_OPPTJ", "Vurder opptjeningsvilkåret", UTREDES),
    FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING("FASTSETT_STP_BER",
        "Fastsett skjæringstidspunkt beregning", UTREDES),
    KONTROLLER_FAKTA_BEREGNING("KOFAKBER", "Kontroller fakta for beregning", UTREDES),
    FORESLÅ_BEREGNINGSGRUNNLAG("FORS_BERGRUNN", "Foreslå beregningsgrunnlag", UTREDES),
    FASTSETT_BEREGNINGSGRUNNLAG("FAST_BERGRUNN", "Fastsett beregningsgrunnlag", UTREDES),
    SØKNADSFRIST("SØKNADSFRIST", "Vurder søknadsfrist foreldrepenger", UTREDES),
    KONTROLLER_FAKTA_UTTAK("KOFAKUT", "Kontroller fakta for uttak", UTREDES),
    KONTROLLER_FAKTA_ARBEIDSFORHOLD("KOARB", "Kontroller arbeidsforhold", UTREDES),
    FASTSETT_OPPTJENINGSPERIODE("VURDER_OPPTJ_PERIODE", "Vurder Opptjening Periode", UTREDES),
    KONTROLLER_LØPENDE_MEDLEMSKAP("KOFAK_LOP_MEDL", "Kontroller løpende medlemskap", UTREDES),
    HINDRE_TILBAKETREKK("BERYT_OPPDRAG", "Hindre tilbaketrekk", UTREDES),

    FORDEL_BEREGNINGSGRUNNLAG("FORDEL_BERGRUNN", "Fordel beregningsgrunnlag", UTREDES),
    VULOMED("VULOMED", "Vurder løpende medlemskap", UTREDES),
    INREG_AVSL("INREG_AVSL", "Innhent registeropplysninger - resterende oppgaver", UTREDES),
    VURDER_OPPTJENING_FAKTA("VURDER_OPPTJ_FAKTA", "Vurder opptjeningfakta", UTREDES),
    VURDER_UTLAND("VURDER_UTLAND", "Vurder utland (SED)", UTREDES),

    VURDER_MEDISINSKVILKÅR("VURDER_MEDISINSK", "Vurder medisinskvilkår", UTREDES),
    ;


    static final String KODEVERK = "BEHANDLING_STEG_TYPE";

    private static final Map<String, BehandlingStegType> KODER = new LinkedHashMap<>();

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

    @JsonIgnore
    private String navn;

    private String kode;

    private BehandlingStegType(String kode) {
        this.kode = kode;
    }

    private BehandlingStegType(String kode, String navn, BehandlingStatus definertBehandlingStatus) {
        this.kode = kode;
        this.navn = navn;
        this.definertBehandlingStatus = definertBehandlingStatus;
    }

    public BehandlingStatus getDefinertBehandlingStatus() {
        return definertBehandlingStatus;
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktDefinisjonerInngang() {
        return AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(this, VurderingspunktType.INN);
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktDefinisjonerUtgang() {
        return AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(this, VurderingspunktType.UT);
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktDefinisjoner(VurderingspunktType type) {
        return AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(this, type);
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
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

    @Override
    public String getOffisiellKode() {
        return getKode();
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

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

    public static Map<String, BehandlingStegType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }


}
