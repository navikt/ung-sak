package no.nav.k9.sak.web.app.tjenester.brukerdialog.policy;

import java.util.function.Function;
import java.util.function.Supplier;

public class PolicyUtils {
    public static <T> Policy<T> not(Policy<T> spec) {
        return spec.not();
    }

    public static <T, R> R requirePermitOrFail(T ctx, Policy<T> policy, Function<PolicyEvaluation, R> block) {
        PolicyEvaluation eval = policy.evaluate(ctx);
        if (eval.getDecision() == PolicyDecision.PERMIT) {
            return block.apply(eval);
        } else {
            throw new PolicyEvaluationException(eval);
        }
    }

    public static <T, R> Object requirePermit(T ctx, Policy<T> policy, Supplier<R> block) {
        PolicyEvaluation eval = policy.evaluate(ctx);
        if (eval.getDecision() == PolicyDecision.PERMIT) {
            return block.get();
        } else {
            return eval;
        }
    }

    public static <T, R> R evaluate(T ctx, Policy<T> policy, Function<PolicyEvaluation, R> block) {
        return block.apply(policy.evaluate(ctx));
    }

    public static <T, R> R authorize(T ctx, Policy<T> policy, Function<PolicyEvaluation, R> block) {
        return requirePermitOrFail(ctx, policy, block);
    }
}
