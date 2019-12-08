package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;

/**
 * Interface for oppdaterere for fakta om beregning tilfeller.
 *
 */
public interface FaktaOmBeregningTilfelleOppdaterer {

    /**
     * Oppdaterer beregningsgrunnlaget med verdier fastsatt i gui og lager historikkinnslag.
     *
     * Metoder som overrider denne MÅ sjekke om dto.getFaktaOmBeregningTilfeller() inneholder aktuelt tilfelle.
     * @param dto Fakta om beregning dto sendt ned fra frontend
     * @param behandlingReferanse aktuell behandling
     * @param nyttBeregningsgrunnlag kopiert fra aktivt beregningsgrunnlag
     * @param forrigeBg Beregningsgrunnlag fra forrige avklaring av fakta om beregning
     */
    void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse,
                  BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                  Optional<BeregningsgrunnlagEntitet> forrigeBg);

}
