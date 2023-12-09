package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class NyttInntektsforholdDto  {

    @JsonProperty("aktivitetStatus")
    @Valid
    @NotNull
    private AktivitetStatus aktivitetStatus;

    @JsonProperty("arbeidsgiverId")
    @Valid
    @Pattern(
        regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$",
        message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'"
    )
    private String arbeidsgiverIdentifikator;

    @JsonProperty("arbeidsforholdId")
    @Valid
    @Pattern(
        regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$",
        message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsforholdId;

    @JsonProperty("bruttoInntektPrÅr")
    @Valid
    @Min(0L)
    @Max(178956970L)
    private Integer bruttoInntektPrÅr;

    @JsonProperty("skalRedusereUtbetaling")
    private boolean skalRedusereUtbetaling;

    public NyttInntektsforholdDto() {
        //
    }

    public NyttInntektsforholdDto(AktivitetStatus aktivitetStatus,
                                  String arbeidsgiverIdentifikator,
                                  String arbeidsforholdId, Integer bruttoInntektPrÅr,
                                  boolean skalRedusereUtbetaling) {
        this.aktivitetStatus = aktivitetStatus;
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
        this.arbeidsforholdId = arbeidsforholdId;
        this.bruttoInntektPrÅr = bruttoInntektPrÅr;
        this.skalRedusereUtbetaling = skalRedusereUtbetaling;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public void setAktivitetStatus(AktivitetStatus aktivitetStatus) {
        this.aktivitetStatus = aktivitetStatus;
    }

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public void setArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public void setArbeidsforholdId(String arbeidsforholdId) {
        this.arbeidsforholdId = arbeidsforholdId;
    }

    public Integer getBruttoInntektPrÅr() {
        return bruttoInntektPrÅr;
    }

    public void setBruttoInntektPrÅr(Integer bruttoInntektPrÅr) {
        this.bruttoInntektPrÅr = bruttoInntektPrÅr;
    }

    public boolean isSkalRedusereUtbetaling() {
        return skalRedusereUtbetaling;
    }

    public void setSkalRedusereUtbetaling(boolean skalRedusereUtbetaling) {
        this.skalRedusereUtbetaling = skalRedusereUtbetaling;
    }
}
