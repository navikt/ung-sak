package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.refusjon;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
