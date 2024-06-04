package no.nav.k9.sak.web.app.tjenester.brukerdialog.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class Policy<T extends PolicyContext> {
    private final String id;
    private final String description;
    private final List<Policy<T>> policies;
    private final Function<T, PolicyEvaluation> evaluation;

    // Constructors, getters, and setters
    public Policy(String id, String description, List<Policy<T>> policies, Function<T, PolicyEvaluation> evaluation) {
        this.id = id;
        this.description = description;
        this.policies = policies != null ? policies : Collections.emptyList();
        this.evaluation = evaluation;
    }

    public Policy(String id, String description, Function<T, PolicyEvaluation> evaluation) {
        this.id = id;
        this.description = description;
        this.policies = Collections.emptyList();
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
        List<Policy<T>> newPolicies = new ArrayList<>(this.specOrChildren());
        newPolicies.addAll(other.specOrChildren());
        return new Policy<>(
            "",
            this.description + " AND " + other.description,
            newPolicies,
            t -> this.evaluate(t).and(other.evaluate(t))
        );
    }

    public Policy<T> or(Policy<T> other) {
        List<Policy<T>> newPolicies = new ArrayList<>(this.specOrChildren());
        newPolicies.addAll(other.specOrChildren());
        return new Policy<>(
            "",
            this.description + " OR " + other.description,
            newPolicies,
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
        return new Policy<>(id, description, this.policies, this.evaluation);
    }

    private List<Policy<T>> specOrChildren() {
        return this.id.isEmpty() && !this.policies.isEmpty() ? this.policies : Collections.singletonList(this);
    }
}
