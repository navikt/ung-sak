package no.nav.ung.sak.test.util.behandling;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingAdapter;

import java.util.List;

public record UngKlageTestScenario(
    KlageUtredningEntitet.Builder klageUtredning,
    KlageVurderingAdapter klageVurdering,
    TestScenarioBuilder originalBehandlingScenario,
    List<AksjonspunktDefinisjon> utførteAksjonspunkter) {
}


