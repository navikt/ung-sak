package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.response.v1.tilkommetAktivitet.UtledetTilkommetAktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class NyAktivitetPrVilkårperiodeDto {

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    @JsonProperty(value = "vilkårPeriode", required = true)
    @Valid
    private DatoIntervallEntitet vilkårPeriode;

    @JsonProperty(value = "aktiviteter", required = true)
    @Size(max = 100)
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    private List<UtledetTilkommetAktivitet> aktiviteter;

    @JsonCreator
    public NyAktivitetPrVilkårperiodeDto(@JsonProperty(value = "vilkårPeriode", required = true) DatoIntervallEntitet vilkårPeriode,
                                         @JsonProperty(value = "kalkulatorInput", required = true) List<UtledetTilkommetAktivitet> aktiviteter) {
        this.vilkårPeriode = vilkårPeriode;
        this.aktiviteter = aktiviteter;
    }

    public DatoIntervallEntitet getVilkårPeriode() {
        return vilkårPeriode;
    }

    public List<UtledetTilkommetAktivitet> getAktiviteter() {
        return aktiviteter;
    }
}
