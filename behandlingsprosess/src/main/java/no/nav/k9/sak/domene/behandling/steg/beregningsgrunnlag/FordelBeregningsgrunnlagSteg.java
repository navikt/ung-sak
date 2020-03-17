package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT;
import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.output.KalkulusResultat;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;

@FagsakYtelseTypeRef("*")
@BehandlingStegRef(kode = "FORDEL_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class FordelBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private KalkulusTjeneste kalkulusTjeneste;

    protected FordelBeregningsgrunnlagSteg() {
        // CDI Proxy
    }

    @Inject
    public FordelBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                        BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                        KalkulusTjeneste kalkulusTjeneste) {

        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        KalkulusResultat kalkulusResultat = kalkulusTjeneste.fortsettBeregning(BehandlingReferanse.fra(behandling), FORDEL_BEREGNINGSGRUNNLAG);

        Boolean vilkårOppfylt = kalkulusResultat.getVilkårOppfylt();
        beregningsgrunnlagVilkårTjeneste.lagreVilkårresultat(kontekst, vilkårOppfylt);

        if (vilkårOppfylt) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(kalkulusResultat.getBeregningAksjonspunktResultat().stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));
        } else {
            return BehandleStegResultat.fremoverført(FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
        }
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (tilSteg.equals(FORDEL_BEREGNINGSGRUNNLAG)) {
            throw new IllegalStateException("imp i kalkulus");
//            Set<Aksjonspunkt> aps = behandlingRepository.hentBehandling(kontekst.getBehandlingId()).getAksjonspunkter();
//            boolean harAksjonspunktSomErUtførtIUtgang = tilSteg.getAksjonspunktDefinisjonerUtgang().stream()
//                    .anyMatch(ap -> aps.stream().filter(a -> a.getAksjonspunktDefinisjon().equals(ap))
//                            .anyMatch(a -> !a.erAvbrutt()));
//            beregningsgrunnlagTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddFordelBeregningsgrunnlagVedTilbakeføring(harAksjonspunktSomErUtførtIUtgang);
        } else {
            beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst);
        }
    }
}
