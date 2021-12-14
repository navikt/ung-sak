package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.refusjon;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VurderRefusjonAndelBeregningsgrunnlagDto {

    @JsonProperty(value = "arbeidsgiverOrgnr")
    @Valid
    @Pattern(regexp = "[\\d]{9}")
    private String arbeidsgiverOrgnr;

    @JsonProperty(value = "arbeidsgiverAktoerId")
    @Valid
    @Pattern(regexp = "[\\d]{13}")
    private String arbeidsgiverAktoerId;

    @JsonProperty(value = "internArbeidsforholdRef")
    @Valid
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String internArbeidsforholdRef;

    @JsonProperty(value = "fastsattRefusjonFom")
    @Valid
    @NotNull
    private LocalDate fastsattRefusjonFom;

    @JsonProperty(value = "delvisRefusjonPrMndFørStart")
    @Valid
    @Min(0)
    @Max(10000000)
    private Integer delvisRefusjonPrMndFørStart;

    VurderRefusjonAndelBeregningsgrunnlagDto() { // NOSONAR
        // Jackson
    }

    public VurderRefusjonAndelBeregningsgrunnlagDto(@Valid String arbeidsgiverOrgnr,
                                                    @Valid String arbeidsgiverAktoerId,
                                                    @Valid String internArbeidsforholdRef,
                                                    @Valid @NotNull LocalDate fastsattRefusjonFom,
                                                    @Valid Integer delvisRefusjonPrMndFørStart) {
        this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
        this.arbeidsgiverAktoerId = arbeidsgiverAktoerId;
        this.internArbeidsforholdRef = internArbeidsforholdRef;
        this.fastsattRefusjonFom = fastsattRefusjonFom;
        this.delvisRefusjonPrMndFørStart = delvisRefusjonPrMndFørStart;
    }

    public String getArbeidsgiverOrgnr() {
        return arbeidsgiverOrgnr;
    }

    public String getArbeidsgiverAktoerId() {
        return arbeidsgiverAktoerId;
    }

    public String getInternArbeidsforholdRef() {
        return internArbeidsforholdRef;
    }

    public LocalDate getFastsattRefusjonFom() {
        return fastsattRefusjonFom;
    }

    public Integer getDelvisRefusjonPrMndFørStart() {
        return delvisRefusjonPrMndFørStart;
    }
}
