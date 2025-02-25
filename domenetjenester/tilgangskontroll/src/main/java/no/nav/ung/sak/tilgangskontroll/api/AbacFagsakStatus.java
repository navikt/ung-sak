package no.nav.ung.sak.tilgangskontroll.api;

public enum AbacFagsakStatus {
    OPPRETTET("Opprettet"),
    UNDER_BEHANDLING("Under behandling");

    private String eksternKode;

    AbacFagsakStatus(String eksternKode) {
        this.eksternKode = eksternKode;
    }

    public String getEksternKode() {
        return eksternKode;
    }
}
