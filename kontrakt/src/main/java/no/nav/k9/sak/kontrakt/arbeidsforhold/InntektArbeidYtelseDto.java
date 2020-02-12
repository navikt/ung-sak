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
    @JsonProperty(value = "arbeidsforhold")
    private List<InntektArbeidYtelseArbeidsforhold> arbeidsforhold = Collections.emptyList();

    @Valid
    @Size(max = 1000)
    @JsonProperty(value = "inntektsmeldinger")
    private List<InntektsmeldingDto> inntektsmeldinger = Collections.emptyList();

    @Valid
    @Size(max = 1000)
    @JsonProperty(value = "innvilgetRelatertTilgrensendeYtelserForAnnenForelder")
    private List<RelaterteYtelserDto> innvilgetRelatertTilgrensendeYtelserForAnnenForelder = Collections.emptyList();

    @Valid
    @Size(max = 1000)
    @JsonProperty(value = "relatertTilgrensendeYtelserForAnnenForelder")
    private List<RelaterteYtelserDto> relatertTilgrensendeYtelserForAnnenForelder = Collections.emptyList();

    @Valid
    @Size(max = 1000)
    @JsonProperty(value = "relatertTilgrensendeYtelserForSoker")
    private List<RelaterteYtelserDto> relatertTilgrensendeYtelserForSoker = Collections.emptyList();

    @JsonProperty(value = "skalKunneLageArbeidsforholdBasertPaInntektsmelding")
    private boolean skalKunneLageArbeidsforholdBasertPaInntektsmelding = false;

    @JsonProperty(value = "skalKunneLeggeTilNyeArbeidsforhold")
    private boolean skalKunneLeggeTilNyeArbeidsforhold = false;

    public InntektArbeidYtelseDto() {
        //
    }

    public List<InntektArbeidYtelseArbeidsforhold> getArbeidsforhold() {
        return Collections.unmodifiableList(arbeidsforhold);
    }

    public List<InntektsmeldingDto> getInntektsmeldinger() {
        return Collections.unmodifiableList(inntektsmeldinger);
    }

    public List<RelaterteYtelserDto> getInnvilgetRelatertTilgrensendeYtelserForAnnenForelder() {
        return Collections.unmodifiableList(innvilgetRelatertTilgrensendeYtelserForAnnenForelder);
    }

    public List<RelaterteYtelserDto> getRelatertTilgrensendeYtelserForAnnenForelder() {
        return Collections.unmodifiableList(relatertTilgrensendeYtelserForAnnenForelder);
    }

    public List<RelaterteYtelserDto> getRelatertTilgrensendeYtelserForSoker() {
        return Collections.unmodifiableList(relatertTilgrensendeYtelserForSoker);
    }

    public boolean getSkalKunneLageArbeidsforholdBasertPaInntektsmelding() {
        return skalKunneLageArbeidsforholdBasertPaInntektsmelding;
    }

    public boolean getSkalKunneLeggeTilNyeArbeidsforhold() {
        return skalKunneLeggeTilNyeArbeidsforhold;
    }

    public void setArbeidsforhold(List<InntektArbeidYtelseArbeidsforhold> arbeidsforhold) {
        this.arbeidsforhold = List.copyOf(arbeidsforhold);
    }

    public void setInntektsmeldinger(List<InntektsmeldingDto> inntektsmeldinger) {
        this.inntektsmeldinger = inntektsmeldinger;
    }

    public void setRelatertTilgrensendeYtelserForSoker(List<RelaterteYtelserDto> relatertTilgrensendeYtelserForSoker) {
        this.relatertTilgrensendeYtelserForSoker = List.copyOf(relatertTilgrensendeYtelserForSoker);
    }

    public void setSkalKunneLageArbeidsforholdBasertPaInntektsmelding(boolean skalKunneLageArbeidsforholdBasertPaInntektsmelding) {
        this.skalKunneLageArbeidsforholdBasertPaInntektsmelding = skalKunneLageArbeidsforholdBasertPaInntektsmelding;
    }

    public void setSkalKunneLageArbeidsforholdBasrtPåInntektsmelding(boolean skalKunneLageArbeidsforholdBasertPaInntektsmelding) {
        this.skalKunneLageArbeidsforholdBasertPaInntektsmelding = skalKunneLageArbeidsforholdBasertPaInntektsmelding;
    }

    public void setSkalKunneLeggeTilNyeArbeidsforhold(boolean skalKunneLeggeTilNyeArbeidsforhold) {
        this.skalKunneLeggeTilNyeArbeidsforhold = skalKunneLeggeTilNyeArbeidsforhold;
    }

    void setInnvilgetRelatertTilgrensendeYtelserForAnnenForelder(List<RelaterteYtelserDto> innvilgetRelatertTilgrensendeYtelserForAnnenForelder) {
        this.innvilgetRelatertTilgrensendeYtelserForAnnenForelder = List.copyOf(innvilgetRelatertTilgrensendeYtelserForAnnenForelder);
    }

    void setRelatertTilgrensendeYtelserForAnnenForelder(List<RelaterteYtelserDto> relatertTilgrensendeYtelserForAnnenForelder) {
        this.relatertTilgrensendeYtelserForAnnenForelder = List.copyOf(relatertTilgrensendeYtelserForAnnenForelder);
    }
}
