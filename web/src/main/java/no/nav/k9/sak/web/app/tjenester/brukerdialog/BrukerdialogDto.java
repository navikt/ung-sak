package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.PolicyEvaluation;

import java.util.Objects;

public class BrukerdialogDto {
    private final PolicyEvaluation evaluation;

    public BrukerdialogDto(PolicyEvaluation policyEvaluation) {
        Objects.requireNonNull(policyEvaluation, "policyEvaluation");
        this.evaluation = policyEvaluation;
    }

    public PolicyEvaluation evaluation() {
        return evaluation;
    }
}
