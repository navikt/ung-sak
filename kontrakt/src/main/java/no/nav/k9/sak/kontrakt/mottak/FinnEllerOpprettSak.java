package no.nav.k9.sak.kontrakt.mottak;

import java.util.Optional;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FinnEllerOpprettSak {

    @JsonProperty("journalpostId")
    @Digits(integer = 18, fraction = 0)
    private String journalpostId;

    @JsonProperty(value = "behandlingstemaOffisiellKode", required = true)
    @NotNull
    @Size(max = 8)
    @Pattern(regexp = "^[a-zA-ZæøåÆØÅ_\\-0-9]*$")
    private String behandlingstemaOffisiellKode;

    @JsonProperty(value = "aktørId", required = true)
    @NotNull
    @Digits(integer = 19, fraction = 0)
    private String aktørId;

    @JsonCreator
    public FinnEllerOpprettSak(@JsonProperty("journalpostId") @Digits(integer = 18, fraction = 0) String journalpostId,
                               @JsonProperty(value = "behandlingstemaOffisiellKode", required = true) @NotNull @Size(max = 8) @Pattern(regexp = "^[a-zA-ZæøåÆØÅ_\\-0-9]*$") String behandlingstemaOffisiellKode,
                               @JsonProperty(value = "aktørId", required = true) @NotNull @Digits(integer = 19, fraction = 0)String aktørId) {
        this.journalpostId = journalpostId;
        this.behandlingstemaOffisiellKode = behandlingstemaOffisiellKode;
        this.aktørId = aktørId;
    }

    public Optional<String> getJournalpostId() {
        return Optional.ofNullable(journalpostId);
    }

    public String getBehandlingstemaOffisiellKode() {
        return behandlingstemaOffisiellKode;
    }

    @AbacAttributt(value = "aktorId", masker = true)
    public String getAktørId() {
        return aktørId;
    }

}
