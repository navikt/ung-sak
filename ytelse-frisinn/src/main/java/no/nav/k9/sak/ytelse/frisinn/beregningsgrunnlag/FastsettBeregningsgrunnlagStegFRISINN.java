package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagSteg;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.FastsettBeregningsgrunnlagSteg;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef("FRISINN")
@BehandlingStegRef(kode = "FAST_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class FastsettBeregningsgrunnlagStegFRISINN extends FastsettBeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste kalkulusTjeneste;
    private BeregningsgrunnlagVilkårTjenesteFRISINN vilkårTjeneste;

    protected FastsettBeregningsgrunnlagStegFRISINN() {
        // for CDI proxy
    }

    @Inject
    public FastsettBeregningsgrunnlagStegFRISINN(BehandlingRepository behandlingRepository,
                                                 BeregningTjeneste kalkulusTjeneste,
                                                 BeregningsgrunnlagVilkårTjenesteFRISINN vilkårTjeneste) {

        this.kalkulusTjeneste = kalkulusTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);

        var perioderTilVurdering = vilkårTjeneste.utledPerioderTilVurdering(ref, false);

        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            var kalkulusResultat = kalkulusTjeneste.fortsettBeregning(ref, periode.getFomDato(), FASTSETT_BEREGNINGSGRUNNLAG);
            if (kalkulusResultat.getVilkårOppfylt() != null && !kalkulusResultat.getVilkårOppfylt()) {
                vilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, periode, kalkulusResultat.getAvslagsårsak());
            } else {
                kalkulusResultat.getVilkårResultatPrPeriode().forEach((key, value) -> {
                    if (!value.getVilkårOppfylt()) {
                        vilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, key, value.getAvslagsårsak());
                    }
                });
            }
        }

        return BehandleStegResultat.utførtMedAksjonspunktResultater(Collections.emptyList());
    }
}
