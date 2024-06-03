package no.nav.k9.sak.web.app.tjenester.brukerdialog.policy;

public class PolicyEvaluationException extends RuntimeException {
    private final PolicyEvaluation evaluation;

    public PolicyEvaluationException(PolicyEvaluation evaluation) {
        super("policy evaluation failed with reason=" + evaluation.getReason());
        this.evaluation = evaluation;
    }

    public PolicyEvaluationException(PolicyEvaluation evaluation, Throwable t) {
        super("policy evaluation failed with reason=" + evaluation.getReason(), t);
        this.evaluation = evaluation;
    }

    public PolicyEvaluation getEvaluation() {
        return evaluation;
    }
}
