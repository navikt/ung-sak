package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk;

class GradOgBegrunnelse {

    private final int grad;
    private final String begrunnelse;
    private Boolean årsaksammenheng;
    private String årsaksammenhengBegrunnelse;

    GradOgBegrunnelse(int grad, String begrunnelse) {
        this.grad = grad;
        this.begrunnelse = begrunnelse;
    }

    public GradOgBegrunnelse(int grad, String begrunnelse, Boolean årsaksammenheng, String årsaksammenhengBegrunnelse) {
        this.grad = grad;
        this.begrunnelse = begrunnelse;
        this.årsaksammenheng = årsaksammenheng;
        this.årsaksammenhengBegrunnelse = årsaksammenhengBegrunnelse;
    }

    public int getGrad() {
        return grad;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Boolean getÅrsaksammenheng() {
        return årsaksammenheng;
    }

    public String getÅrsaksammenhengBegrunnelse() {
        return årsaksammenhengBegrunnelse;
    }
}
