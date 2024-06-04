package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.PolicyContext;

public record BehandlingContext(Behandling behandling) implements PolicyContext {
}
