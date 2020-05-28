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
public class FastsettBeregningsgrunnlagATFLDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "skjæringstidspunkt")
    @Valid
    @NotNull
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "inntektFrilanser")
    @Min(0)
    @Max(100 * 1000 * 1000)
    private Integer inntektFrilanser;

    @JsonProperty(value = "inntektPrAndelList")
    @Valid
    @Size(max = 100)
    private List<InntektPrAndelDto> inntektPrAndelList;

    public FastsettBeregningsgrunnlagATFLDto() {
        // For Jackson
    }

    public FastsettBeregningsgrunnlagATFLDto(String begrunnelse, Integer inntektFrilanser) { // NOSONAR
        super(begrunnelse);
        this.inntektFrilanser = inntektFrilanser;
    }

    public FastsettBeregningsgrunnlagATFLDto(String begrunnelse, List<InntektPrAndelDto> inntektPrAndelList, Integer inntektFrilanser) { // NOSONAR
        super(begrunnelse);
        this.inntektPrAndelList = new ArrayList<>(inntektPrAndelList);
        this.inntektFrilanser = inntektFrilanser;
    }

    public Integer getInntektFrilanser() {
        return inntektFrilanser;
    }

    public void setInntektFrilanser(Integer inntektFrilanser) {
        this.inntektFrilanser = inntektFrilanser;
    }

    public List<InntektPrAndelDto> getInntektPrAndelList() {
        return inntektPrAndelList;
    }

    public void setInntektPrAndelList(List<InntektPrAndelDto> inntektPrAndelList) {
        this.inntektPrAndelList = inntektPrAndelList;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }
}
