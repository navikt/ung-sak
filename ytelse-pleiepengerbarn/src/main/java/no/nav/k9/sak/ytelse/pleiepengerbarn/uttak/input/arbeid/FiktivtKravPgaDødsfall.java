package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import no.nav.k9.sak.typer.JournalpostId;

class FiktivtKravPgaDødsfall {

    private final JournalpostId sisteKravFørDødsfall;
    private boolean harHåndtertDødsfall;

    private FiktivtKravPgaDødsfall(JournalpostId sisteKravFørDødsfall, boolean harHåndtertDødsfall) {
        this.sisteKravFørDødsfall = sisteKravFørDødsfall;
        this.harHåndtertDødsfall = harHåndtertDødsfall;
    }

    static FiktivtKravPgaDødsfall ikkeDød() {
        return new FiktivtKravPgaDødsfall(null, true);
    }

    static FiktivtKravPgaDødsfall død(JournalpostId journalpostId) {
        return new FiktivtKravPgaDødsfall(journalpostId, false);
    }

    static FiktivtKravPgaDødsfall dødUtenSøknadFørDødsfall() {
        return new FiktivtKravPgaDødsfall(null, false);
    }

    public boolean getHarKravdokumentInnsendtFørDødsfall() {
        return sisteKravFørDødsfall != null;
    }

    public JournalpostId getSisteKravFørDødsfall() {
        return sisteKravFørDødsfall;
    }

    public boolean getHarHåndtertDødsfall() {
        return harHåndtertDødsfall;
    }

    public void markerHåndtert() {
        harHåndtertDødsfall = true;
    }
}
