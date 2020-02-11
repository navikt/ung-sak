package no.nav.foreldrepenger.behandlingslager.behandling.medisinsk;

class GradOgBegrunnelse {

    private final int grad;
    private final String begrunnelse;

    GradOgBegrunnelse(int grad, String begrunnelse) {
        this.grad = grad;
        this.begrunnelse = begrunnelse;
    }

    public int getGrad() {
        return grad;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
