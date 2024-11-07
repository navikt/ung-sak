package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
        this.journalpostId = Objects.requireNonNull(journalpostId);
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
        return getClass().getSimpleName() +
            "<journalpostId=" + journalpostId +
            ", mottattTidspunkt=" + mottattTidspunkt +
            ", status=" + status +
            ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var other = (MottattInntektsmeldingDto) obj;
        return Objects.equals(journalpostId, other.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId);
    }

}
