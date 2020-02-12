package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Beløp;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class InntektsmeldingDto {

    @JsonProperty(value = "arbeidsgiver")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiver;

    @JsonProperty(value = "arbeidsgiverOrgnr")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverOrgnr;

    @JsonProperty(value = "arbeidsgiverStartdato")
    private LocalDate arbeidsgiverStartdato;

    @JsonProperty(value = "getRefusjonBeløpPerMnd")
    @Valid
    private Beløp getRefusjonBeløpPerMnd;

    @JsonProperty(value = "graderingPerioder")
    @Size(max = 100)
    @Valid
    private List<GraderingPeriodeDto> graderingPerioder = new ArrayList<>();

    @JsonProperty(value = "innsendingstidspunkt")
    private LocalDateTime innsendingstidspunkt;

    @JsonProperty(value = "utsettelsePerioder")
    @Size(max = 100)
    @Valid
    private List<UtsettelsePeriodeDto> utsettelsePerioder = new ArrayList<>();

    public InntektsmeldingDto() {
        //
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public String getArbeidsgiverOrgnr() {
        return arbeidsgiverOrgnr;
    }

    public LocalDate getArbeidsgiverStartdato() {
        return arbeidsgiverStartdato;
    }

    public Beløp getGetRefusjonBeløpPerMnd() {
        return getRefusjonBeløpPerMnd;
    }

    public List<GraderingPeriodeDto> getGraderingPerioder() {
        return Collections.unmodifiableList(graderingPerioder);
    }

    public LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    public List<UtsettelsePeriodeDto> getUtsettelsePerioder() {
        return Collections.unmodifiableList(utsettelsePerioder);
    }

    public void setArbeidsgiver(String arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public void setArbeidsgiverOrgnr(String arbeidsgiverOrgnr) {
        this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
    }

    public void setArbeidsgiverStartdato(LocalDate arbeidsgiverStartdato) {
        this.arbeidsgiverStartdato = arbeidsgiverStartdato;
    }

    public void setGetRefusjonBeløpPerMnd(Beløp getRefusjonBeløpPerMnd) {
        this.getRefusjonBeløpPerMnd = getRefusjonBeløpPerMnd;
    }

    public void setGraderingPerioder(List<GraderingPeriodeDto> perioder) {
        this.graderingPerioder = List.copyOf(perioder);
    }

    public void setInnsendingstidspunkt(LocalDateTime tidspunkt) {
        this.innsendingstidspunkt = tidspunkt;
    }

    public void setUtsettelsePerioder(List<UtsettelsePeriodeDto> perioder) {
        this.utsettelsePerioder = List.copyOf(perioder);
    }
}
