package no.nav.k9.sak.kontrakt.sykdom.dokument;

import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.typer.JournalpostId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomDokumentOpprettelseDto {

    @JsonProperty(value = "behandlingUuid", required = true)
    @Valid
    private UUID behandlingUuid;

    @JsonProperty(value = "journalpostId", required = true)
    @NotNull
    @Digits(integer = 18, fraction = 0)
    private String journalpostId;


    public SykdomDokumentOpprettelseDto() {
     
    }
    
    public SykdomDokumentOpprettelseDto(String behandlingUuid, String journalpostId) {
        this.behandlingUuid = UUID.fromString(behandlingUuid);
        this.journalpostId = Objects.requireNonNull(journalpostId);
    }
    
    public SykdomDokumentOpprettelseDto(UUID behandlingUuid, JournalpostId journalpostId) {
        this.behandlingUuid = behandlingUuid;
        this.journalpostId = journalpostId.getVerdi();
    }
    
    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    @AbacAttributt("journalpostId")
    public JournalpostId getJournalpostId() {
        return new JournalpostId(journalpostId);
    }
}