package no.nav.k9.sak.behandlingslager.hendelser;

public abstract class Forretningshendelse {
    private ForretningshendelseType forretningshendelseType;

    protected Forretningshendelse(ForretningshendelseType forretningshendelseType) {
        this.forretningshendelseType = forretningshendelseType;
    }

    public ForretningshendelseType getForretningshendelseType() {
        return forretningshendelseType;
    }
}
