package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor.regelmodell;

public class Relasjon {

    private final String fraAktørId;
    private final String tilAktørId;
    private final RelasjonsRolle relasjonsRolle;
    private final boolean harSammeBosted;

    public Relasjon(String fraAktørId, String tilAktørId, RelasjonsRolle relasjonsRolle, boolean harSammeBosted) {
        this.fraAktørId = fraAktørId;
        this.tilAktørId = tilAktørId;
        this.relasjonsRolle = relasjonsRolle;
        this.harSammeBosted = harSammeBosted;
    }

    public String getFraAktørId() {
        return fraAktørId;
    }

    public String getTilAktørId() {
        return tilAktørId;
    }

    public RelasjonsRolle getRelasjonsRolle() {
        return relasjonsRolle;
    }

    public boolean getHarSammeBosted() {
        return harSammeBosted;
    }

    @Override
    public String toString() {
        return "Relasjon{" +
            "fraAktørId='" + fraAktørId + '\'' +
            ", tilAktørId='" + tilAktørId + '\'' +
            ", relasjonsRolle='" + relasjonsRolle + '\'' +
            ", harSammeBosted=" + harSammeBosted +
            '}';
    }
}
