package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.fpsak.nare.evaluation.Evaluation;

import java.util.Objects;

public class BrukerdialogEvaluation {
    @JsonIgnore private final Evaluation evaluation;

    public BrukerdialogEvaluation(Evaluation evaluation) {
        Objects.requireNonNull(evaluation, "evaluation");
        this.evaluation = evaluation;
    }

    public Evaluation evaluation() {
        return evaluation;
    }
}
