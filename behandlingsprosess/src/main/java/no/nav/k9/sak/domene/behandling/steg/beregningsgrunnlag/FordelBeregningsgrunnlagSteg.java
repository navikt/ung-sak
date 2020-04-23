package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG;
import static no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.output.KalkulusResultat;
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
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef("*")
@BehandlingStegRef(kode = "FORDEL_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class FordelBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandletPeriodeTjeneste behandletPeriodeTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private Instance<KalkulusTjeneste> kalkulusTjeneste;

    protected FordelBeregningsgrunnlagSteg() {
        // CDI Proxy
    }

    @Inject
    public FordelBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                        BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                        @Any Instance<KalkulusTjeneste> kalkulusTjeneste,
                                        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                        BehandletPeriodeTjeneste behandletPeriodeTjeneste) {

        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandletPeriodeTjeneste = behandletPeriodeTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId()));
        KalkulusResultat kalkulusResultat = FagsakYtelseTypeRef.Lookup.find(kalkulusTjeneste, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke kalkulustjeneste"))
            .fortsettBeregning(ref, FORDEL_BEREGNINGSGRUNNLAG);

        Boolean vilkårOppfylt = kalkulusResultat.getVilkårOppfylt();
        var vilkårsPeriode = behandletPeriodeTjeneste.utledPeriode(ref);
        var orginalVilkårsPeriode = behandletPeriodeTjeneste.utledOrginalVilkårsPeriode(ref);
        beregningsgrunnlagVilkårTjeneste.lagreVilkårresultat(kontekst, vilkårOppfylt, vilkårsPeriode, orginalVilkårsPeriode);

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
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId()));
            var vilkårsPeriode = behandletPeriodeTjeneste.utledPeriode(ref);
            beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst, vilkårsPeriode);
        }
    }
}
