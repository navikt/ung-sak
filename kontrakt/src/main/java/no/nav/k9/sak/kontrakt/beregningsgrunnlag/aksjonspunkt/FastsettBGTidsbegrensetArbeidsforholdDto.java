package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FastsettBGTidsbegrensetArbeidsforholdDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "skjæringstidspunkt")
    @Valid
    @NotNull
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "fastsatteTidsbegrensendePerioder")
    @Valid
    @Size(max = 100)
    private List<FastsattePerioderTidsbegrensetDto> fastsatteTidsbegrensedePerioder;

    @JsonProperty(value = "frilansInntekt")
    @Min(0)
    @Max(1000 * 1000 * 10)
    private Integer frilansInntekt;

    public FastsettBGTidsbegrensetArbeidsforholdDto() {
        // For Jackson
    }

    public FastsettBGTidsbegrensetArbeidsforholdDto(String begrunnelse, List<FastsattePerioderTidsbegrensetDto> fastsatteTidsbegrensedePerioder) { // NOSONAR
        super(begrunnelse);
        this.fastsatteTidsbegrensedePerioder = new ArrayList<>(fastsatteTidsbegrensedePerioder);
    }

    public List<FastsattePerioderTidsbegrensetDto> getFastsatteTidsbegrensedePerioder() {
        return fastsatteTidsbegrensedePerioder;
    }

    public void setFastsatteTidsbegrensedePerioder(List<FastsattePerioderTidsbegrensetDto> fastsatteTidsbegrensedePerioder) {
        this.fastsatteTidsbegrensedePerioder = fastsatteTidsbegrensedePerioder;
    }

    public Integer getFrilansInntekt() {
        return frilansInntekt;
    }

    public void setFrilansInntekt(Integer frilansInntekt) {
        this.frilansInntekt = frilansInntekt;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }
}
