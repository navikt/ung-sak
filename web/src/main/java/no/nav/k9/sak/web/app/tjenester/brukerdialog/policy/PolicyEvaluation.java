package no.nav.k9.sak.web.app.tjenester.brukerdialog.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PolicyEvaluation {
    private final PolicyDecision decision;
    private final String reason;
    private final String description;
    private final String id;
    private final Operator operator;
    private final List<PolicyEvaluation> policyChain;

    public PolicyEvaluation(PolicyDecision decision, String reason, String description, String id, Operator operator, List<PolicyEvaluation> policyChain) {
        this.decision = decision;
        this.reason = reason;
        this.description = description;
        this.id = id;
        this.operator = operator;
        this.policyChain = policyChain != null ? policyChain : new ArrayList<>();
    }

    public PolicyEvaluation(PolicyDecision decision, String reason) {
        this(decision, reason, "", "", Operator.NONE, Collections.emptyList());
    }

    public PolicyEvaluation and(PolicyEvaluation other) {
        List<PolicyEvaluation> newPolicyChain = new ArrayList<>(this.specOrPolicyChain());
        newPolicyChain.addAll(other.specOrPolicyChain());
        return new PolicyEvaluation(
            this.decision.and(other.decision),
            "(" + this.reason + " AND " + other.reason + ")",
            this.description,
            this.id,
            Operator.AND,
            newPolicyChain
        );
    }

    public PolicyEvaluation or(PolicyEvaluation other) {
        List<PolicyEvaluation> newPolicyChain = new ArrayList<>(this.specOrPolicyChain());
        newPolicyChain.addAll(other.specOrPolicyChain());
        return new PolicyEvaluation(
            this.decision.or(other.decision),
            "(" + this.reason + " OR " + other.reason + ")",
            this.description,
            this.id,
            Operator.OR,
            newPolicyChain
        );
    }

    public PolicyEvaluation not() {
        return new PolicyEvaluation(
            this.decision.not(),
            "(NOT " + this.reason + ")",
            this.description,
            this.id,
            Operator.NOT,
            Collections.singletonList(this)
        );
    }

    public PolicyDecision getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    private List<PolicyEvaluation> specOrPolicyChain() {
        return this.id.isEmpty() && !this.policyChain.isEmpty() ? this.policyChain : Collections.singletonList(this);
    }

    public static PolicyEvaluation permit(String reason) {
        return new PolicyEvaluation(PolicyDecision.PERMIT, reason);
    }

    public static PolicyEvaluation deny(String reason) {
        return new PolicyEvaluation(PolicyDecision.DENY, reason);
    }

    public static PolicyEvaluation notApplicable(String reason) {
        return new PolicyEvaluation(PolicyDecision.NOT_APPLICABLE, reason);
    }

    public static PolicyEvaluation evaluate(String id, String description, PolicyEvaluation eval) {
        return new PolicyEvaluation(eval.decision, eval.reason, description, id, eval.operator, eval.policyChain);
    }

    public boolean isPermit() {
        return this.decision == PolicyDecision.PERMIT;
    }

    public boolean isDeny() {
        return this.decision == PolicyDecision.DENY;
    }

    public boolean equalTo(PolicyDecision decision) {
        return this.decision == decision;
    }

    public boolean notEqualTo(PolicyDecision decision) {
        return this.decision != decision;
    }
}
