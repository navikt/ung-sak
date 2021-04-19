package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagSteg;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk.PSBKompletthetsjekker;

@FagsakYtelseTypeRef
@BehandlingStegRef(kode = "KOMPLETT_FOR_BEREGNING")
@BehandlingTypeRef
@ApplicationScoped
public class VurderKompletthetForBeregningSteg implements BeregningsgrunnlagSteg {

    private BeregningTjeneste kalkulusTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private PSBKompletthetsjekker kompletthetsjekker;

    protected VurderKompletthetForBeregningSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderKompletthetForBeregningSteg(BeregningTjeneste kalkulusTjeneste,
                                             BehandlingRepository behandlingRepository,
                                             BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                             @BehandlingTypeRef @FagsakYtelseTypeRef("PSB") PSBKompletthetsjekker kompletthetsjekker) {

        this.kalkulusTjeneste = kalkulusTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kompletthetsjekker = kompletthetsjekker;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);

        if (perioderTilVurdering.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var kompletthetsVurderinger = kompletthetsjekker.utledAlleManglendeVedleggForPerioder(ref);

        var erKomplett = kompletthetsVurderinger.entrySet()
            .stream()
            .filter(it -> perioderTilVurdering.contains(it.getKey()))
            .allMatch(it -> it.getValue().isEmpty());

        if (erKomplett) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        } else {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_KOMPLETT_NOK_FOR_BEREGNING));
        }
    }
}
