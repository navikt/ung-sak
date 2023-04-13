package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.kalkulus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagKobling;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagListe;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@Dependent
@Default
class KalkulusTjenesteAdapter {

    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private VilkårTjeneste vilkårTjeneste;

    KalkulusTjenesteAdapter() {
        // CDI proxy
    }

    @Inject
    KalkulusTjenesteAdapter(BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                            VilkårTjeneste vilkårTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    public Optional<BeregningsgrunnlagListe> hentBeregningsgrunnlagForGui(BehandlingReferanse ref) {
        return beregningsgrunnlagTjeneste.hentBeregningsgrunnlag(ref);

    }

    public List<Beregningsgrunnlag> hentBeregningsgrunnlagFastsatt(BehandlingReferanse ref) {
        var vilkårene = vilkårTjeneste.hentVilkårResultat(ref.getBehandlingId());
        var vilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        List<LocalDate> skjæringstidspunkter = vilkår.getPerioder()
            .stream()
            .filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()))
            .map(VilkårPeriode::getSkjæringstidspunkt)
            .toList();

        return beregningsgrunnlagTjeneste.hentEksaktFastsatt(ref, skjæringstidspunkter);

    }


    public List<BeregningsgrunnlagKobling> hentKoblingerForPerioderTilVurdering(BehandlingReferanse ref) {
        return beregningsgrunnlagTjeneste.hentKoblingerForPerioderTilVurdering(ref);

    }

}
