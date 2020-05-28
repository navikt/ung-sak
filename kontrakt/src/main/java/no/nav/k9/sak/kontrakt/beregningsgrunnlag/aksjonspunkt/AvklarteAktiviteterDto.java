package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.time.LocalDate;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_AKTIVITETER_KODE)
public class AvklarteAktiviteterDto extends BekreftetBeregningsgrunnlagDto {

    @JsonProperty(value = "skjæringstidspunkt")
    @Valid
    @NotNull
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "beregningsaktivitetLagreDtoList")
    @Valid
    @Size(max = 1000)
    private List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList;

    public AvklarteAktiviteterDto() {
        //
    }

    public AvklarteAktiviteterDto(LocalDate skjæringstidspunkt, List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList, String begrunnelse) { // NOSONAR
        super(begrunnelse);
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.beregningsaktivitetLagreDtoList = beregningsaktivitetLagreDtoList;
    }

    public List<BeregningsaktivitetLagreDto> getBeregningsaktivitetLagreDtoList() {
        return beregningsaktivitetLagreDtoList;
    }

    public void setBeregningsaktivitetLagreDtoList(List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList) {
        this.beregningsaktivitetLagreDtoList = beregningsaktivitetLagreDtoList;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }
}
