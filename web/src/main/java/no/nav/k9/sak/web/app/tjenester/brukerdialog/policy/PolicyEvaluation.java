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
    private final List<PolicyEvaluation> policies;

    public PolicyEvaluation(PolicyDecision decision, String reason, String description, String id, Operator operator, List<PolicyEvaluation> policies) {
        this.decision = decision;
        this.reason = reason;
        this.description = description;
        this.id = id;
        this.operator = operator;
        this.policies = policies != null ? policies : new ArrayList<>();
    }

    public PolicyEvaluation(PolicyDecision decision, String reason) {
        this(decision, reason, "", "", Operator.NONE, Collections.emptyList());
    }

    public PolicyEvaluation and(PolicyEvaluation other) {
        List<PolicyEvaluation> newChildren = new ArrayList<>(this.specOrPolicies());
        newChildren.addAll(other.specOrPolicies());
        return new PolicyEvaluation(
            this.decision.and(other.decision),
            "(" + this.reason + " AND " + other.reason + ")",
            this.description,
            this.id,
            Operator.AND,
            newChildren
        );
    }

    public PolicyEvaluation or(PolicyEvaluation other) {
        List<PolicyEvaluation> newChildren = new ArrayList<>(this.specOrPolicies());
        newChildren.addAll(other.specOrPolicies());
        return new PolicyEvaluation(
            this.decision.or(other.decision),
            "(" + this.reason + " OR " + other.reason + ")",
            this.description,
            this.id,
            Operator.OR,
            newChildren
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

    private List<PolicyEvaluation> specOrPolicies() {
        return this.id.isEmpty() && !this.policies.isEmpty() ? this.policies : Collections.singletonList(this);
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
        return new PolicyEvaluation(eval.decision, eval.reason, description, id, eval.operator, eval.policies);
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
