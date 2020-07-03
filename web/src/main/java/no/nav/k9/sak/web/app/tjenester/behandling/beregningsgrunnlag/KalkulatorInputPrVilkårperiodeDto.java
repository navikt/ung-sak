package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KalkulatorInputPrVilkårperiodeDto {

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    @JsonProperty(value = "vilkårPeriode", required = true)
    @Valid
    private DatoIntervallEntitet vilkårPeriode;

    @JsonProperty(value = "kalkulatorInput", required = true)
    @Size(max = 100)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    private KalkulatorInputDto kalkulatorInput;

    @JsonCreator
    public KalkulatorInputPrVilkårperiodeDto(@JsonProperty(value = "vilkårPeriode", required = true) DatoIntervallEntitet vilkårPeriode,
                                             @JsonProperty(value = "kalkulatorInput", required = true) KalkulatorInputDto kalkulatorInput) {
        this.vilkårPeriode = vilkårPeriode;
        this.kalkulatorInput = kalkulatorInput;
    }

    public DatoIntervallEntitet getVilkårPeriode() {
        return vilkårPeriode;
    }

    public KalkulatorInputDto getKalkulatorInput() {
        return kalkulatorInput;
    }
}
