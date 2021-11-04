package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagSteg;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.internal.PSBKompletthetSjekkerTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk.PSBKompletthetsjekker;

@FagsakYtelseTypeRef("PSB")
@BehandlingStegRef(kode = "KOMPLETT_FOR_BEREGNING")
@BehandlingTypeRef
@ApplicationScoped
public class VurderKompletthetForBeregningSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private PSBKompletthetSjekkerTjeneste kompletthetSjekkerTjeneste;
    private PSBKompletthetsjekker kompletthetsjekker;
    private boolean benyttNyFlyt = false;

    protected VurderKompletthetForBeregningSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderKompletthetForBeregningSteg(BehandlingRepository behandlingRepository,
                                             BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                             PSBKompletthetSjekkerTjeneste kompletthetSjekkerTjeneste,
                                             @BehandlingTypeRef @FagsakYtelseTypeRef("PSB") PSBKompletthetsjekker kompletthetsjekker,
                                             @KonfigVerdi(value = "KOMPLETTHET_NY_FLYT", defaultVerdi = "false") Boolean benyttNyFlyt) {

        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kompletthetSjekkerTjeneste = kompletthetSjekkerTjeneste;
        this.kompletthetsjekker = kompletthetsjekker;
        this.benyttNyFlyt = benyttNyFlyt;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        if (benyttNyFlyt) {
            return nyKompletthetFlyt(ref, kontekst);
        } else {
            return legacykompletthetHåndtering(ref);
        }
    }

    private BehandleStegResultat nyKompletthetFlyt(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst) {
        var kompletthetsAksjon = kompletthetSjekkerTjeneste.utledHandlinger(ref, kontekst);

        if (kompletthetsAksjon.kanFortsette()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        } else if (kompletthetsAksjon.harFrist()) {
            var aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(kompletthetsAksjon.getAksjonspunktDefinisjon(),
                utledVenteÅrsak(kompletthetsAksjon),
                kompletthetsAksjon.getFrist());

            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(aksjonspunktResultat));
        } else if(!kompletthetsAksjon.erUavklart()) {
            var aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunkt(kompletthetsAksjon.getAksjonspunktDefinisjon());

            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(aksjonspunktResultat));
        } else {
            throw new IllegalStateException("Ugyldig kompletthetstilstand :: " + kompletthetsAksjon);
        }
    }

    private Venteårsak utledVenteÅrsak(KompletthetsAksjon kompletthetsAksjon) {
        var ap = kompletthetsAksjon.getAksjonspunktDefinisjon();
        if (AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_FOR_BEREGNING.equals(ap)) {
            return Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKTSMELDINGER;
        }
        if (AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING.equals(ap)) {
            return Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKTSMELDINGER;
        }
        throw new IllegalArgumentException("Ukjent autopunkt '" + ap + "'");
    }

    private BehandleStegResultat legacykompletthetHåndtering(BehandlingReferanse ref) {
        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);

        if (perioderTilVurdering.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var kompletthetsVurderinger = kompletthetsjekker.utledAlleManglendeVedleggForPerioder(ref);
        var relevanteKompletthetsvurderinger = kompletthetsVurderinger.entrySet()
            .stream()
            .filter(it -> perioderTilVurdering.contains(it.getKey()))
            .collect(Collectors.toList());

        if (relevanteKompletthetsvurderinger.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var erKomplett = relevanteKompletthetsvurderinger.stream()
            .allMatch(it -> it.getValue().isEmpty());

        if (erKomplett) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        } else {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_KOMPLETT_NOK_FOR_BEREGNING));
        }
    }

}
