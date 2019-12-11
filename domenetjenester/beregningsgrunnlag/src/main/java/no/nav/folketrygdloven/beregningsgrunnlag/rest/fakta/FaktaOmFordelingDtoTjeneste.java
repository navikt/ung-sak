package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FordelingDto;

@ApplicationScoped
public class FaktaOmFordelingDtoTjeneste {

    private FordelBeregningsgrunnlagDtoTjeneste fordelBeregningsgrunnlagDtoTjeneste;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    FaktaOmFordelingDtoTjeneste() {
        // For CDI
    }

    @Inject
    FaktaOmFordelingDtoTjeneste(FordelBeregningsgrunnlagDtoTjeneste fordelBeregningsgrunnlagDtoTjeneste, BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.fordelBeregningsgrunnlagDtoTjeneste = fordelBeregningsgrunnlagDtoTjeneste;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public Optional<FordelingDto> lagDto(BeregningsgrunnlagInput input) {
        var ref = input.getBehandlingReferanse();
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagFraFordelSteg = beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitet(ref.getBehandlingId(),
                BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
        BeregningsgrunnlagTilstand tilstandForAktivtGrunnlag = input.getBeregningsgrunnlagGrunnlag().getBeregningsgrunnlagTilstand();
        if (grunnlagFraFordelSteg.isPresent() && !tilstandForAktivtGrunnlag.erFør(BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING)) {
            FordelingDto dto = new FordelingDto();
            fordelBeregningsgrunnlagDtoTjeneste.lagDto(input, dto);
            return Optional.of(dto);
        }
        return Optional.empty();
    }

}
