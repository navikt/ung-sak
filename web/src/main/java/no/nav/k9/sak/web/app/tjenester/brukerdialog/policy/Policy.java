package no.nav.k9.sak.web.app.tjenester.brukerdialog.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class Policy<T> {
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

    public Policy(Function<T, PolicyEvaluation> evaluation) {
        this("", "", Collections.emptyList(), evaluation);
    }

    // Evaluate method
    public PolicyEvaluation evaluate(T t) {
        return PolicyEvaluation.evaluate(
            this.id,
            this.description,
            this.evaluation.apply(t)
        );
    }

    // and, or, not methods
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

    public static class Builder<T> {
        private String id = "";
        private String description = "";
        private List<Policy<T>> children = Collections.emptyList();
        private Function<T, PolicyEvaluation> evaluation = t -> PolicyEvaluation.deny("not implemented");

        public Builder<T> id(String id) {
            this.id = id;
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> children(List<Policy<T>> children) {
            this.children = children;
            return this;
        }

        public Builder<T> evaluation(Function<T, PolicyEvaluation> evaluation) {
            this.evaluation = evaluation;
            return this;
        }

        public Policy<T> build() {
            return new Policy<>(this.id, this.description, this.children, this.evaluation);
        }
    }

    public static <T> Policy<T> policy(Function<Builder<T>, Builder<T>> block) {
        Builder<T> builder = new Builder<>();
        builder = block.apply(builder);
        return builder.build();
    }
}
