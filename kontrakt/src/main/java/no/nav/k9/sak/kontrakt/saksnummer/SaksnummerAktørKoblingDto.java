package no.nav.k9.sak.kontrakt.saksnummer;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SaksnummerAktørKoblingDto {

    @JsonProperty(value = "saksnummer", required = true)
    @Valid
    @NotNull
    private Saksnummer saksnummer;

    @JsonProperty(value = "aktørId", required = true)
    @Valid
    @NotNull
    private AktørId aktørId;

    @JsonProperty(value = "journalpostId", required = true)
    @Valid
    @NotNull
    private JournalpostId journalpostId;

    public SaksnummerAktørKoblingDto(Saksnummer saksnummer, AktørId aktørId, JournalpostId journalpostId) {
        this.saksnummer = saksnummer;
        this.aktørId = aktørId;
        this.journalpostId = journalpostId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer, aktørId, journalpostId);
    }
}
