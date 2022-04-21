package no.nav.k9.sak.kontrakt.kompletthet.inntektsmelding;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.kontrakt.kompletthet.ArbeidsgiverArbeidsforholdIdV2;
import no.nav.k9.sak.typer.JournalpostId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class InntektsmeldingVurdering {

    @NotNull
    @Valid
    @JsonProperty(value = "arbeidsgiver")
    private ArbeidsgiverArbeidsforholdIdV2 arbeidsgiver;

    @NotNull
    @Valid
    @JsonProperty(value = "vurdering")
    private Vurdering vurdering;

    @Valid
    @NotNull
    @JsonProperty(value = "journalpostId")
    private JournalpostId journalpostId;

    @Valid
    @JsonProperty(value = "førsteFraværsdag")
    private LocalDate førsteFraværsdag;

    @NotNull
    @Valid
    @JsonProperty(value = "mottatt")
    private LocalDateTime mottatt;

    @NotNull
    @Valid
    @Pattern(regexp = "^[\\p{Graph}\\s\\t\\p{Sc}\\p{L}\\p{M}\\p{N}]+$",
        message = "Inntektsmelding kildeSystem [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "eksternReferanse")
    private String eksternReferanse;

    @NotNull
    @Valid
    @Size
    @JsonProperty(value = "erstattetAv")
    private List<JournalpostId> erstattetAv;

    public InntektsmeldingVurdering(@Valid @NotNull @JsonProperty(value = "arbeidsgiver") ArbeidsgiverArbeidsforholdIdV2 arbeidsgiver,
                                    @Valid @NotNull @JsonProperty(value = "vurdering") Vurdering vurdering,
                                    @Valid @NotNull @JsonProperty(value = "journalpostId") JournalpostId journalpostId,
                                    @Valid @JsonProperty(value = "førsteFraværsdag") LocalDate førsteFraværsdag,
                                    @Valid @NotNull @JsonProperty(value = "mottatt") LocalDateTime mottatt,
                                    @Valid @NotNull @JsonProperty(value = "eksternReferanse") String eksternReferanse,
                                    @Valid @NotNull @Size @JsonProperty(value = "erstattetAv") List<JournalpostId> erstattetAv) {
        this.arbeidsgiver = arbeidsgiver;
        this.vurdering = vurdering;
        this.journalpostId = journalpostId;
        this.førsteFraværsdag = førsteFraværsdag;
        this.mottatt = mottatt;
        this.eksternReferanse = eksternReferanse;
        this.erstattetAv = erstattetAv;
    }

    public ArbeidsgiverArbeidsforholdIdV2 getArbeidsgiver() {
        return arbeidsgiver;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public Vurdering getVurdering() {
        return vurdering;
    }

    public LocalDate getFørsteFraværsdag() {
        return førsteFraværsdag;
    }

    public LocalDateTime getMottatt() {
        return mottatt;
    }

    public String getEksternReferanse() {
        return eksternReferanse;
    }

    public List<JournalpostId> getErstattetAv() {
        return erstattetAv;
    }
}
