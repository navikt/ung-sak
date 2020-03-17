package no.nav.k9.sak.kontrakt.mottak;

import java.util.Optional;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class FinnEllerOpprettSak {

    @Digits(integer = 18, fraction = 0)
    private String journalpostId;

    @NotNull
    @Size(max = 8)
    @Pattern(regexp = "^[a-zA-ZæøåÆØÅ_\\-0-9]*$")
    private String behandlingstemaOffisiellKode;

    @NotNull
    @Digits(integer = 19, fraction = 0)
    private String aktørId;

    public FinnEllerOpprettSak(String journalpostId, String behandlingstemaOffisiellKode, String aktørId) {
        this.journalpostId = journalpostId;
        this.behandlingstemaOffisiellKode = behandlingstemaOffisiellKode;
        this.aktørId = aktørId;
    }

    protected FinnEllerOpprettSak() {
        //For Jackson
    }

    public Optional<String> getJournalpostId() {
        return Optional.ofNullable(journalpostId);
    }

    public String getBehandlingstemaOffisiellKode() {
        return behandlingstemaOffisiellKode;
    }

    public String getAktørId() {
        return aktørId;
    }

}
