package no.nav.ung.sak.behandlingslager.behandling.vilkår;

class RegelInputOgEvaluering {

    private final String regelEvaluering;
    private final String regelInput;

    RegelInputOgEvaluering(String regelEvaluering, String regelInput) {
        this.regelEvaluering = regelEvaluering;
        this.regelInput = regelInput;
    }

    String getRegelEvaluering() {
        return regelEvaluering;
    }

    String getRegelInput() {
        return regelInput;
    }

}
