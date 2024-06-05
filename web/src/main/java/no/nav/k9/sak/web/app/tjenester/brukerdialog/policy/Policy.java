package no.nav.k9.sak.web.app.tjenester.brukerdialog.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class Policy<T extends PolicyContext> {
    private final String id;
    private final String description;
    private final List<Policy<T>> policyChain;
    private final Function<T, PolicyEvaluation> evaluation;

    public Policy(String id, String description, List<Policy<T>> policyChain, Function<T, PolicyEvaluation> evaluation) {
        this.id = id;
        this.description = description;
        this.policyChain = policyChain != null ? policyChain : Collections.emptyList();
        this.evaluation = evaluation;
    }

    public Policy(String id, String description, Function<T, PolicyEvaluation> evaluation) {
        this.id = id;
        this.description = description;
        this.policyChain = Collections.emptyList();
        this.evaluation = evaluation;
    }

    public PolicyEvaluation evaluate(T t) {
        return PolicyEvaluation.evaluate(
            this.id,
            this.description,
            this.evaluation.apply(t)
        );
    }

    public Policy<T> and(Policy<T> other) {
        List<Policy<T>> newPolicyChain = new ArrayList<>(this.specOrPolicyChain());
        newPolicyChain.addAll(other.specOrPolicyChain());
        return new Policy<>(
            "",
            this.description + " AND " + other.description,
            newPolicyChain,
            t -> this.evaluate(t).and(other.evaluate(t))
        );
    }

    public Policy<T> or(Policy<T> other) {
        List<Policy<T>> newPolicyChain = new ArrayList<>(this.specOrPolicyChain());
        newPolicyChain.addAll(other.specOrPolicyChain());
        return new Policy<>(
            "",
            this.description + " OR " + other.description,
            newPolicyChain,
            t -> this.evaluate(t).or(other.evaluate(t))
        );
    }

    public Policy<T> not() {
        return new Policy<>(
            "!" + this.id,
            "!" + this.description,
            Collections.singletonList(this),
            t -> this.evaluate(t).not()
        );
    }

    public Policy<T> with(String id, String description) {
        return new Policy<>(id, description, this.policyChain, this.evaluation);
    }

    private List<Policy<T>> specOrPolicyChain() {
        return this.id.isEmpty() && !this.policyChain.isEmpty() ? this.policyChain : Collections.singletonList(this);
    }
}
