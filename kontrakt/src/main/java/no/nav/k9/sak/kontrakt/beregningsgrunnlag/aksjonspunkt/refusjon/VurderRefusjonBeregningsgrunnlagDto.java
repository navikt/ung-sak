package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.refusjon;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.BekreftetBeregningsgrunnlagDto;
import no.nav.k9.sak.typer.Periode;

public class VurderRefusjonBeregningsgrunnlagDto extends BekreftetBeregningsgrunnlagDto {

    @Valid
    @Size(max = 100)
    private List<VurderRefusjonAndelBeregningsgrunnlagDto> fastsatteAndeler;

    VurderRefusjonBeregningsgrunnlagDto() { // NOSONAR
        // Jackson
    }

    public VurderRefusjonBeregningsgrunnlagDto(List<VurderRefusjonAndelBeregningsgrunnlagDto> fastsatteAndeler, Periode periode) { // NOSONAR
        super(periode);
        this.fastsatteAndeler = fastsatteAndeler;
    }


    public List<VurderRefusjonAndelBeregningsgrunnlagDto> getFastsatteAndeler() {
        return fastsatteAndeler;
    }
}
