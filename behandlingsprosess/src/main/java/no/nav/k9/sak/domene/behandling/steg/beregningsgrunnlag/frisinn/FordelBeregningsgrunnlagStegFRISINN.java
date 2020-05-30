package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.frisinn;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG;

import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningResultatMapper;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagSteg;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef("FRISINN")
@BehandlingStegRef(kode = "FORDEL_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class FordelBeregningsgrunnlagStegFRISINN implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BeregningTjeneste kalkulusTjeneste;

    protected FordelBeregningsgrunnlagStegFRISINN() {
        // CDI Proxy
    }

    @Inject
    public FordelBeregningsgrunnlagStegFRISINN(BehandlingRepository behandlingRepository,
                                               BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                               BeregningTjeneste kalkulusTjeneste) {

        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);
        var aksjonspunktResultater = new ArrayList<AksjonspunktResultat>();
        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            var kalkulusResultat = kalkulusTjeneste.fortsettBeregning(ref, periode.getFomDato(), FORDEL_BEREGNINGSGRUNNLAG);
            kalkulusResultat.getVilkårResultatPrPeriode().forEach((key, value) -> {
                if (!value.getVilkårOppfylt()) {
                    beregningsgrunnlagVilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, key, value.getAvslagsårsak());
                } else {
                    beregningsgrunnlagVilkårTjeneste.lagreVilkårresultat(kontekst, key, value.getVilkårOppfylt());
                }
            });
            aksjonspunktResultater.addAll(kalkulusResultat.getBeregningAksjonspunktResultat().stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));
        }
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultater);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (tilSteg.equals(FORDEL_BEREGNINGSGRUNNLAG)) {
            throw new IllegalStateException("imp i kalkulus");
        } else {
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            var ref = BehandlingReferanse.fra(behandling);
            beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, false)
                .forEach(periode -> deaktiverResultatOgSettPeriodeTilVurdering(ref, kontekst, periode));
        }
    }

    private void deaktiverResultatOgSettPeriodeTilVurdering(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst, DatoIntervallEntitet periode) {
        beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst, periode);
        kalkulusTjeneste.deaktiverBeregningsgrunnlag(ref, periode.getFomDato());
    }
}
