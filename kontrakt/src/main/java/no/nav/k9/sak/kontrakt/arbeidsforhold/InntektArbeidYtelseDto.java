package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class InntektArbeidYtelseDto {

    @Valid
    @Size(max = 1000)
    @JsonProperty(value = "inntektsmeldinger")
    private List<InntektsmeldingDto> inntektsmeldinger = Collections.emptyList();

    @Valid
    @Size(max = 1000)
    @JsonProperty(value = "relatertTilgrensendeYtelserForSoker")
    private List<RelaterteYtelserDto> relatertTilgrensendeYtelserForSoker = Collections.emptyList();

    @Valid
    @Size(max = 1000)
    @JsonProperty(value = "relatertTilgrensendeYtelserForAnnenForelder")
    private List<RelaterteYtelserDto> relatertTilgrensendeYtelserForAnnenForelder = Collections.emptyList();

    @Valid
    @Size(max = 1000)
    @JsonProperty(value = "innvilgetRelatertTilgrensendeYtelserForAnnenForelder")
    private List<RelaterteYtelserDto> innvilgetRelatertTilgrensendeYtelserForAnnenForelder = Collections.emptyList();

    @Valid
    @Size(max = 1000)
    @JsonProperty(value = "arbeidsforhold")
    private List<ArbeidsforholdDto> arbeidsforhold = Collections.emptyList();

    @JsonProperty(value = "skalKunneLeggeTilNyeArbeidsforhold")
    private boolean skalKunneLeggeTilNyeArbeidsforhold = false;

    @JsonProperty(value = "skalKunneLageArbeidsforholdBasertPaInntektsmelding")
    private boolean skalKunneLageArbeidsforholdBasertPaInntektsmelding = false;

    public void setInntektsmeldinger(List<InntektsmeldingDto> inntektsmeldinger) {
        this.inntektsmeldinger = inntektsmeldinger;
    }

    public List<InntektsmeldingDto> getInntektsmeldinger() {
        return inntektsmeldinger;
    }

    public void setRelatertTilgrensendeYtelserForSoker(List<RelaterteYtelserDto> relatertTilgrensendeYtelserForSoker) {
        this.relatertTilgrensendeYtelserForSoker = relatertTilgrensendeYtelserForSoker;
    }

    void setRelatertTilgrensendeYtelserForAnnenForelder(List<RelaterteYtelserDto> relatertTilgrensendeYtelserForAnnenForelder) {
        this.relatertTilgrensendeYtelserForAnnenForelder = relatertTilgrensendeYtelserForAnnenForelder;
    }

    void setInnvilgetRelatertTilgrensendeYtelserForAnnenForelder(List<RelaterteYtelserDto> innvilgetRelatertTilgrensendeYtelserForAnnenForelder) {
        this.innvilgetRelatertTilgrensendeYtelserForAnnenForelder = innvilgetRelatertTilgrensendeYtelserForAnnenForelder;
    }

    public List<RelaterteYtelserDto> getRelatertTilgrensendeYtelserForSoker() {
        return relatertTilgrensendeYtelserForSoker;
    }

    public List<RelaterteYtelserDto> getRelatertTilgrensendeYtelserForAnnenForelder() {
        return relatertTilgrensendeYtelserForAnnenForelder;
    }

    public List<RelaterteYtelserDto> getInnvilgetRelatertTilgrensendeYtelserForAnnenForelder() {
        return innvilgetRelatertTilgrensendeYtelserForAnnenForelder;
    }

    public List<ArbeidsforholdDto> getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(List<ArbeidsforholdDto> arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public boolean getSkalKunneLeggeTilNyeArbeidsforhold() {
        return skalKunneLeggeTilNyeArbeidsforhold;
    }

    public void setSkalKunneLeggeTilNyeArbeidsforhold(boolean skalKunneLeggeTilNyeArbeidsforhold) {
        this.skalKunneLeggeTilNyeArbeidsforhold = skalKunneLeggeTilNyeArbeidsforhold;
    }

    public void setSkalKunneLageArbeidsforholdBasrtPåInntektsmelding(boolean skalKunneLageArbeidsforholdBasertPaInntektsmelding) {
        this.skalKunneLageArbeidsforholdBasertPaInntektsmelding = skalKunneLageArbeidsforholdBasertPaInntektsmelding;
    }

    public boolean getSkalKunneLageArbeidsforholdBasertPaInntektsmelding() {
        return skalKunneLageArbeidsforholdBasertPaInntektsmelding;
    }
}
