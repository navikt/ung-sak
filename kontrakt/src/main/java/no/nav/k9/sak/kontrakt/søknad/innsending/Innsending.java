package no.nav.k9.sak.kontrakt.søknad.innsending;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Innsending {

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private Saksnummer saksnummer;

    @JsonProperty(value = "journalpostId")
    @Valid
    private JournalpostId journalpostId;

    @JsonProperty(value = "innhold", required = true)
    @NotNull
    @Valid
    private InnsendingInnhold innhold;

    @JsonCreator
    public Innsending(@JsonProperty(value = "saksnummer", required = true) @NotNull @Valid Saksnummer saksnummer,
                      @JsonProperty(value = "innhold", required = true) @NotNull @Valid InnsendingInnhold innhold,
                      @JsonProperty(value = "journalpostId") @Valid JournalpostId journalpostId
                      ) {
        this.journalpostId = journalpostId;
        this.saksnummer = Objects.requireNonNull(saksnummer, "saksnummer");
        this.innhold = Objects.requireNonNull(innhold, "innhold");
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    @AbacAttributt("journalpostId")
    public JournalpostId getJournalpostId() {
        return journalpostId;
    }
    
    public InnsendingInnhold getInnhold() {
        return innhold;
    }

    public FagsakYtelseType getYtelseType() {
        return innhold.getYtelseType();
    }
}
