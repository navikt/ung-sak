package no.nav.ung.kodeverk.behandling;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static no.nav.ung.kodeverk.behandling.BehandlingStatus.IVERKSETTER_VEDTAK;
import static no.nav.ung.kodeverk.behandling.BehandlingStatus.UTREDES;

public enum BehandlingStegType implements Kodeverdi {

    BEREGN_YTELSE("BERYT", "Beregn ytelse", UTREDES),
    FATTE_VEDTAK("FVEDSTEG", "Fatte Vedtak", BehandlingStatus.FATTER_VEDTAK),
    FORESLÅ_BEHANDLINGSRESULTAT("FORBRES", "Foreslå behandlingsresultat", UTREDES),
    FORESLÅ_VEDTAK("FORVEDSTEG", "Foreslå vedtak", UTREDES),
    VURDER_SØKNADSFRIST("VURDER_SØKNADSFRIST", "Vurder søknadsfrist", UTREDES),
    INIT_PERIODER("INIT_PERIODER", "Start", UTREDES),
    INIT_VILKÅR("INIT_VILKÅR", "Initier vilkår for behandling", UTREDES),
    INNHENT_REGISTEROPP("INREG", "Innhent registeropplysninger", UTREDES),
    IVERKSETT_VEDTAK("IVEDSTEG", "Iverksett Vedtak", IVERKSETTER_VEDTAK),
    KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT("VURDEROP", "Kontrollerer søkers opplysningsplikt", UTREDES),
    SIMULER_OPPDRAG("SIMOPP", "Simuler oppdrag", UTREDES),
    START_STEG("START", "Start behandling prosess", UTREDES),
    VURDER_KOMPLETTHET("VURDERKOMPLETT", "Vurder kompletthet", UTREDES),
    ALDERSVILKÅRET("VURDER_ALDER", "Vurder søkers alder", UTREDES),
    VURDER_TILBAKETREKK("VURDER_TILBAKETREKK", "Vurder tilbaketrekk", UTREDES),
    VURDER_UTTAK("VURDER_UTTAK", "Vurder antall dager og uttak", UTREDES),
    VURDER_UNGDOMSPROGRAMVILKÅR("VURDER_UNGDOMSPROGRAMVILKÅR", "Vurder deltakelse i ungdomsprogrammet", UTREDES),
    UNGDOMSYTELSE_BEREGNING("UNGDOMSYTELSE_BEREGNING", "Beregner sats for ungdomsytelsen", UTREDES),
    KONTROLLER_REGISTER_INNTEKT("KONTROLLER_REGISTER_INNTEKT", "Kontroller brukers rapporterte inntekt mot registerinntekt", UTREDES),

    // Utgåtte steg: Lagres på behandling og aksjonspunkt og kan ikke fjernes med mindre man håndterer dette i BehandlingStegTypeKodeverdiConverter
    @Deprecated
    KONTROLLER_UNGDOMSPROGRAM("KONTROLLER_UNGDOMSPROGRAM", "Utgått steg: Kontroller endring i ungdomsprogram", UTREDES);


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
    private BehandlingStatus definertBehandlingStatus;

    private String kode;

    private String navn;

    private BehandlingStegType(String kode) {
        this.kode = kode;
    }

    private BehandlingStegType(String kode, String navn, BehandlingStatus definertBehandlingStatus) {
        this.kode = kode;
        this.navn = navn;
        this.definertBehandlingStatus = definertBehandlingStatus;
    }

    public static BehandlingStegType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingStegType: for input " + kode);
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

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

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
