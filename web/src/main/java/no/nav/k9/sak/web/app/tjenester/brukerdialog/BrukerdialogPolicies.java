package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.Policy;
import no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.PolicyDecision;
import no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.PolicyEvaluation;

import java.util.List;

import static no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.PolicyDecision.PERMIT;

public class BrukerdialogPolicies {

    static Policy<Fagsak> erPartISaken(String aktørId, String aktør) {
        return new Policy<Fagsak>(
            "sif.brukerdialog.1",
            aktør + " må være part i saken",
            (Fagsak fagsak) -> {
                List<AktørId> list =
                    fagsak.parterISaken().toList();

                boolean erPartISaken = fagsak.parterISaken()
                    .filter(java.util.Objects::nonNull)
                    .anyMatch(a -> a.getAktørId().equals(aktørId));

                if (!erPartISaken) {
                    return new PolicyEvaluation(PolicyDecision.DENY, aktør + " er ikke part i behandlingen");
                }
                return new PolicyEvaluation(PERMIT, "Permit");
            });
    }
}
