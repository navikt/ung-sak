package no.nav.ung.sak.behandlingskontroll.impl.transisjoner;

import java.util.Arrays;
import java.util.List;

import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.ung.sak.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.ung.sak.behandlingskontroll.transisjoner.TransisjonIdentifikator;

public class Transisjoner {

    private static final List<StegTransisjon> ALLE_TRANSISJONER = Arrays.asList(
        new Startet(),
        new Utført(),
        new HenleggelseTransisjon(),
        new SettPåVent(),
        new TilbakeføringTilAksjonspunktTransisjon(),
        new TilbakeføringTilStegTransisjon(),
        new FremoverhoppTransisjon(FellesTransisjoner.FREMHOPP_TIL_IVERKSETT_VEDTAK.getId(), BehandlingStegType.IVERKSETT_VEDTAK),
        new SpolFremoverTransisjon(BehandlingStegType.KONTROLLER_FAKTA),
        new SpolFremoverTransisjon(BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING),
        new SpolFremoverTransisjon(BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT),
        new SpolFremoverTransisjon(BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR),
        new SpolFremoverTransisjon(BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP),
        new SpolFremoverTransisjon(BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE)
    );

    private Transisjoner() {
        //skal ikke instansieres
    }

    public static StegTransisjon finnTransisjon(TransisjonIdentifikator transisjonIdentifikator) {
        for (StegTransisjon transisjon : ALLE_TRANSISJONER) {
            if (transisjon.getId().equals(transisjonIdentifikator.getId())) {
                return transisjon;
            }
        }
        throw new IllegalArgumentException("Ukjent transisjon: " + transisjonIdentifikator);
    }
}
