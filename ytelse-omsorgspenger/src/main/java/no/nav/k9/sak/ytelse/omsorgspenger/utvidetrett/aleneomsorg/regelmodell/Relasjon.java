package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell;

public class Relasjon {

    private final String fraAktørId;
    private final String tilAktørId;
    private final RelasjonsRolle relasjonsRolle;

    public Relasjon(String fraAktørId, String tilAktørId, RelasjonsRolle relasjonsRolle) {
        this.fraAktørId = fraAktørId;
        this.tilAktørId = tilAktørId;
        this.relasjonsRolle = relasjonsRolle;
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

    @Override
    public String toString() {
        return "Relasjon{" +
            "fraAktørId='" + fraAktørId + '\'' +
            ", tilAktørId='" + tilAktørId + '\'' +
            ", relasjonsRolle='" + relasjonsRolle + '\'' +
            '}';
    }
}
