package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.Policy;
import no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.PolicyEvaluation;

public class BrukerdialogPolicies {

    private BrukerdialogPolicies() {
    }

    static Policy<BehandlingContext> erPartISaken(String aktørId, String aktør) {
        return new Policy<>(
            "sif.brukerdialog.1",
            aktør + " må være part i saken",
            (BehandlingContext context) -> {

                boolean erPartISaken = context.behandling().getFagsak().parterISaken()
                    .filter(java.util.Objects::nonNull)
                    .anyMatch(a -> a.getAktørId().equals(aktørId));

                if (!erPartISaken) {
                    return PolicyEvaluation.deny(aktør + " er ikke part i saken");
                }
                return PolicyEvaluation.permit("Permit");
            });
    }
}
