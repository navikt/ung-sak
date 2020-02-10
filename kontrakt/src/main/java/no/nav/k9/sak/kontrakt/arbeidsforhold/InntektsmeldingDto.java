package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @JsonProperty(value = "innsendingstidspunkt")
    private LocalDateTime innsendingstidspunkt;

    @JsonProperty(value = "utsettelsePerioder")
    @Size(max = 100)
    @Valid
    private List<UtsettelsePeriodeDto> utsettelsePerioder = new ArrayList<>();

    @JsonProperty(value = "graderingPerioder")
    @Size(max = 100)
    @Valid
    private List<GraderingPeriodeDto> graderingPerioder = new ArrayList<>();

    @JsonProperty(value = "getRefusjonBeløpPerMnd")
    @Valid
    private Beløp getRefusjonBeløpPerMnd;

    public InntektsmeldingDto() {
        //
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public String getArbeidsgiverOrgnr() {
        return arbeidsgiverOrgnr;
    }

    public void setArbeidsgiver(String arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public void setArbeidsgiverOrgnr(String arbeidsgiverOrgnr) {
        this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
    }

    public LocalDate getArbeidsgiverStartdato() {
        return arbeidsgiverStartdato;
    }

    public void setArbeidsgiverStartdato(LocalDate arbeidsgiverStartdato) {
        this.arbeidsgiverStartdato = arbeidsgiverStartdato;
    }

    public List<UtsettelsePeriodeDto> getUtsettelsePerioder() {
        return utsettelsePerioder;
    }

    public List<GraderingPeriodeDto> getGraderingPerioder() {
        return graderingPerioder;
    }

    public LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    public Beløp getGetRefusjonBeløpPerMnd() {
        return getRefusjonBeløpPerMnd;
    }

    public void setGetRefusjonBeløpPerMnd(Beløp getRefusjonBeløpPerMnd) {
        this.getRefusjonBeløpPerMnd = getRefusjonBeløpPerMnd;
    }

    public void setInnsendingstidspunkt(LocalDateTime tidspunkt) {
        this.innsendingstidspunkt = tidspunkt;
    }

    public void setUtsettelsePerioder(List<UtsettelsePeriodeDto> perioder) {
        this.utsettelsePerioder = List.copyOf(perioder);
    }

    public void setGraderingPerioder(List<GraderingPeriodeDto> perioder) {
        this.graderingPerioder = List.copyOf(perioder);
    }
}
