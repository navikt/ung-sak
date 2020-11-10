package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.time.LocalDateTime;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.kontrakt.Patterns;
import no.nav.k9.sak.typer.JournalpostId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MottattInntektsmeldingDto {

    @Valid
    @NotNull
    @JsonProperty(value = "journalpostId")
    private JournalpostId journalpostId;

    @NotNull
    @JsonProperty(value = "mottattTidspunkt")
    private LocalDateTime mottattTidspunkt;

    @Valid
    @NotNull
    @JsonProperty(value = "status")
    private DokumentStatus status;


    @JsonProperty(value = "begrunnelse")
    @Size(max = 400)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonCreator
    public MottattInntektsmeldingDto(@JsonProperty(value = "journalpostId") @Valid @NotNull JournalpostId journalpostId,
                                     @JsonProperty(value = "mottattTidspunkt") @NotNull LocalDateTime mottattTidspunkt,
                                     @JsonProperty(value = "status") @Valid @NotNull DokumentStatus status,
                                     @JsonProperty(value = "begrunnelse") @Size(max = 400) @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String begrunnelse) {
        this.journalpostId = journalpostId;
        this.mottattTidspunkt = mottattTidspunkt;
        this.status = status;
        this.begrunnelse = begrunnelse;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public LocalDateTime getMottattTidspunkt() {
        return mottattTidspunkt;
    }

    public DokumentStatus getStatus() {
        return status;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public String toString() {
        return "MottattInntektsmeldingDto{" +
            "journalpostId=" + journalpostId +
            '}';
    }
}
