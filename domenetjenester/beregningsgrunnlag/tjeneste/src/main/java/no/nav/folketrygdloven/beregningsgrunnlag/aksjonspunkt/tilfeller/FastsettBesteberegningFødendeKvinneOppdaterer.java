package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.FastsettBeregningVerdierTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.BesteberegningFødendeKvinneAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.BesteberegningFødendeKvinneDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.DagpengeAndelLagtTilBesteberegningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsatteVerdierDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsatteVerdierForBesteberegningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.RedigerbarAndelFaktaOmBeregningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE")
public class FastsettBesteberegningFødendeKvinneOppdaterer implements FaktaOmBeregningTilfelleOppdaterer {

    @Override
    public void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse,
                         BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                         Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        BesteberegningFødendeKvinneDto besteberegningDto = dto.getBesteberegningAndeler();
        List<BesteberegningFødendeKvinneAndelDto> andelListe = besteberegningDto.getBesteberegningAndelListe();
        for (var periode : nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder()) {
            Optional<BeregningsgrunnlagPeriode> forrigePeriode = forrigeBg
                .flatMap(beregningsgrunnlag -> beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
                    .filter(periode1 -> periode1.getPeriode().overlapper(periode.getPeriode())).findFirst());
            andelListe.stream()
                .filter(a -> a.getNyAndel() == null || !a.getNyAndel())
                .forEach(dtoAndel -> FastsettBeregningVerdierTjeneste.fastsettVerdierForAndel(mapTilRedigerbarAndel(dtoAndel), mapTilFastsatteVerdier(dtoAndel), periode, forrigePeriode));
            if (besteberegningDto.getNyDagpengeAndel() != null) {
                FastsettBeregningVerdierTjeneste.fastsettVerdierForAndel(lagRedigerbarAndelDtoForDagpenger(), mapTilFastsatteVerdier(besteberegningDto.getNyDagpengeAndel()), periode, forrigePeriode);
            }
        }
        if (nyttBeregningsgrunnlag.getAktivitetStatuser().stream().noneMatch(status -> AktivitetStatus.DAGPENGER.equals(status.getAktivitetStatus()))) {
            BeregningsgrunnlagEntitet.builder(nyttBeregningsgrunnlag)
                .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.DAGPENGER));
        }
    }

    private FastsatteVerdierDto mapTilFastsatteVerdier(DagpengeAndelLagtTilBesteberegningDto nyDagpengeAndel) {
        FastsatteVerdierForBesteberegningDto fastsatteVerdier = nyDagpengeAndel.getFastsatteVerdier();
        return new FastsatteVerdierDto(fastsatteVerdier.finnFastsattBeløpPrÅr().intValue(), fastsatteVerdier.getInntektskategori(), true);
    }

    private RedigerbarAndelFaktaOmBeregningDto lagRedigerbarAndelDtoForDagpenger() {
        return new RedigerbarAndelFaktaOmBeregningDto(AktivitetStatus.DAGPENGER);
    }

    private RedigerbarAndelFaktaOmBeregningDto mapTilRedigerbarAndel(BesteberegningFødendeKvinneAndelDto dtoAndel) {
        return new RedigerbarAndelFaktaOmBeregningDto(false, dtoAndel.getAndelsnr(), dtoAndel.getLagtTilAvSaksbehandler());
    }

    private FastsatteVerdierDto mapTilFastsatteVerdier(BesteberegningFødendeKvinneAndelDto dtoAndel) {
        FastsatteVerdierForBesteberegningDto fastsatteVerdier = dtoAndel.getFastsatteVerdier();
        return new FastsatteVerdierDto(fastsatteVerdier.finnFastsattBeløpPrÅr().intValue(), fastsatteVerdier.getInntektskategori(), fastsatteVerdier.getSkalHaBesteberegning());
    }

}
