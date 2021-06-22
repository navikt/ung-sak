package no.nav.k9.sak.kontrakt.søknadsfrist;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.JournalpostId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KravDokumentStatus {

    @NotNull
    @Valid
    @JsonProperty(value = "journalpostId")
    private JournalpostId journalpostId;

    @Valid
    @JsonProperty(value = "type")
    private KravDokumenType type;

    @Valid
    @JsonProperty(value = "innsendingstidspunkt")
    private LocalDateTime innsendingstidspunkt;

    @Size
    @Valid
    @JsonProperty(value = "status")
    private List<SøknadsfristPeriodeDto> statusPerPeriode;

    @Size
    @Valid
    @JsonProperty(value = "avklarteOpplysninger")
    private AvklarteOpplysninger avklarteOpplysninger;

    @Size
    @Valid
    @JsonProperty(value = "overstyrteOpplysninger")
    private AvklarteOpplysninger overstyrteOpplysninger;

    public KravDokumentStatus() {
    }

    @JsonCreator
    public KravDokumentStatus(@Valid @JsonProperty(value = "type") KravDokumenType type,
                              @Size @Valid @JsonProperty(value = "status") List<SøknadsfristPeriodeDto> statusPerPeriode,
                              @Valid @JsonProperty(value = "innsendingstidspunkt") LocalDateTime innsendingstidspunkt,
                              @NotNull @Valid @JsonProperty(value = "journalpostId") JournalpostId journalpostId,
                              @Size @Valid @JsonProperty(value = "avklarteOpplysninger") AvklarteOpplysninger avklarteOpplysninger,
                              @Size @Valid @JsonProperty(value = "overstyrteOpplysninger") AvklarteOpplysninger overstyrteOpplysninger) {
        this.type = type;
        this.statusPerPeriode = statusPerPeriode;
        this.innsendingstidspunkt = innsendingstidspunkt;
        this.journalpostId = journalpostId;
        this.avklarteOpplysninger = avklarteOpplysninger;
        this.overstyrteOpplysninger = overstyrteOpplysninger;
    }

    public KravDokumenType getType() {
        return type;
    }

    public LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    public List<SøknadsfristPeriodeDto> getStatus() {
        return statusPerPeriode;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public AvklarteOpplysninger getAvklarteOpplysninger() {
        return avklarteOpplysninger;
    }

    public AvklarteOpplysninger getOverstyrteOpplysninger() {
        return overstyrteOpplysninger;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KravDokumentStatus that = (KravDokumentStatus) o;
        return Objects.equals(journalpostId, that.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId);
    }
}
