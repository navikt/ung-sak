package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaBeregningLagreDto;

@ApplicationScoped
public class FaktaOmBeregningTilfellerOppdaterer {

    private Instance<FaktaOmBeregningTilfelleOppdaterer> faktaOmBeregningTilfelleOppdaterer;

    FaktaOmBeregningTilfellerOppdaterer() {
        // for CDI proxy
    }

    @Inject
    FaktaOmBeregningTilfellerOppdaterer(@Any Instance<FaktaOmBeregningTilfelleOppdaterer> faktaOmBeregningTilfelleOppdaterer) {
        this.faktaOmBeregningTilfelleOppdaterer = faktaOmBeregningTilfelleOppdaterer;
    }

    public void oppdater(FaktaBeregningLagreDto faktaDto, BehandlingReferanse behandlingReferanse,
                  BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        kjørOppdateringForTilfeller(faktaDto, behandlingReferanse, nyttBeregningsgrunnlag, forrigeBg);
        List<FaktaOmBeregningTilfelle> tilfeller = faktaDto.getFaktaOmBeregningTilfeller();
        settNyeFaktaOmBeregningTilfeller(nyttBeregningsgrunnlag, tilfeller);
    }

    private void kjørOppdateringForTilfeller(FaktaBeregningLagreDto faktaDto, BehandlingReferanse behandlingReferanse, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        faktaDto.getFaktaOmBeregningTilfeller()
            .stream()
            .map(kode -> FaktaOmBeregningTilfelleRef.Lookup.find(faktaOmBeregningTilfelleOppdaterer, kode))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList())
            .forEach(oppdaterer -> oppdaterer.oppdater(faktaDto, behandlingReferanse, nyttBeregningsgrunnlag, forrigeBg));
    }

    private void settNyeFaktaOmBeregningTilfeller(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        List<FaktaOmBeregningTilfelle> utledetTilfeller = nyttBeregningsgrunnlag.getFaktaOmBeregningTilfeller();
        List<FaktaOmBeregningTilfelle> tilfellerLagtTilManuelt = faktaOmBeregningTilfeller.stream()
            .filter(tilfelle -> !utledetTilfeller.contains(tilfelle)).collect(Collectors.toList());
        if (!tilfellerLagtTilManuelt.isEmpty()) {
            BeregningsgrunnlagEntitet.builder(nyttBeregningsgrunnlag).leggTilFaktaOmBeregningTilfeller(tilfellerLagtTilManuelt);
        }
    }
}
