package no.nav.k9.sak.web.app.tjenester.brukerdialog.policy;

import java.util.function.Function;

public class PolicyUtils {
    private PolicyUtils() {
    }

    public static <T extends PolicyContext, R> R evaluate(T ctx, Policy<T> policy, Function<PolicyEvaluation, R> block) {
        return block.apply(policy.evaluate(ctx));
    }
}
