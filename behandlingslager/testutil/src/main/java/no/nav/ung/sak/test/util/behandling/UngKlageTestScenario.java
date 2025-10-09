package no.nav.ung.sak.test.util.behandling;

import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingAdapter;

public record UngKlageTestScenario(
    KlageUtredningEntitet.Builder klageUtredning,
    KlageVurderingAdapter klageVurdering) {
}


