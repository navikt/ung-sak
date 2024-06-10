package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.fpsak.nare.evaluation.Evaluation;

import java.util.Objects;

public class BrukerdialogTilgangEvaluation {
    @JsonIgnore private final Evaluation evaluation;

    public BrukerdialogTilgangEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public Evaluation evaluation() {
        return evaluation;
    }
}
