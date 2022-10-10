package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.kalkulus;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagKobling;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagListe;
import no.nav.k9.sak.behandling.BehandlingReferanse;

@Dependent
@Default
class KalkulusTjenesteAdapter {

    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    KalkulusTjenesteAdapter() {
        // CDI proxy
    }

    @Inject
    KalkulusTjenesteAdapter(BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
    }

    public Optional<BeregningsgrunnlagListe> hentBeregningsgrunnlagForGui(BehandlingReferanse ref) {
        return beregningsgrunnlagTjeneste.hentBeregningsgrunnlag(ref);

    }

    public List<BeregningsgrunnlagKobling> hentKoblingerForPerioderTilVurdering(BehandlingReferanse ref) {
        return beregningsgrunnlagTjeneste.hentKoblingerForPerioderTilVurdering(ref);

    }

}
