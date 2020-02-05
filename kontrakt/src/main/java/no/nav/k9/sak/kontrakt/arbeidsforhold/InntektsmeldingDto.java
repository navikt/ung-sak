package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private String arbeidsgiver;

    @JsonProperty(value = "arbeidsgiverOrgnr")
    private String arbeidsgiverOrgnr;

    @JsonProperty(value = "arbeidsgiverStartdato")
    private LocalDate arbeidsgiverStartdato;

    @JsonProperty(value = "innsendingstidspunkt")
    private LocalDateTime innsendingstidspunkt;

    @JsonProperty(value = "utsettelsePerioder")
    private List<UtsettelsePeriodeDto> utsettelsePerioder = new ArrayList<>();

    @JsonProperty(value = "graderingPerioder")
    private List<GraderingPeriodeDto> graderingPerioder = new ArrayList<>();

    @JsonProperty(value = "getRefusjonBeløpPerMnd")
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
