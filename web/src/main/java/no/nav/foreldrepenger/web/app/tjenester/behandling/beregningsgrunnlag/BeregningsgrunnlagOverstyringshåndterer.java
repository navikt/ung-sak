package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.BeregningFaktaOgOverstyringHåndterer;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.FaktaBeregningHistorikkHåndterer;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.OverstyrBeregningsgrunnlagDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsgrunnlagDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsgrunnlagOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyrBeregningsgrunnlagDto> {

    private BeregningFaktaOgOverstyringHåndterer beregningFaktaOgOverstyringHåndterer;
    private FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer;
    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    BeregningsgrunnlagOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsgrunnlagOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                   BeregningFaktaOgOverstyringHåndterer beregningFaktaOgOverstyringHåndterer,
                                                   FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer,
                                                   HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG);
        this.beregningFaktaOgOverstyringHåndterer = beregningFaktaOgOverstyringHåndterer;
        this.faktaBeregningHistorikkHåndterer = faktaBeregningHistorikkHåndterer;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsgrunnlagDto dto,
                                                  Behandling behandling, BehandlingskontrollKontekst kontekst) {
        beregningFaktaOgOverstyringHåndterer.håndterMedOverstyring(BehandlingReferanse.fra(behandling), dto);
        // Lag historikk
        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
        fjernOverstyrtAksjonspunkt(behandling)
            .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        return builder.build();
    }

    private Optional<Aksjonspunkt> fjernOverstyrtAksjonspunkt(Behandling behandling) {
        return behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyrBeregningsgrunnlagDto dto) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag = beregningsgrunnlagTjeneste
            .hentNestSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandling.getId(), behandling.getOriginalBehandling().map(Behandling::getId), BeregningsgrunnlagTilstand.KOFAKBER_UT);
        BeregningsgrunnlagEntitet aktivtGrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagAggregatForBehandling(behandling.getId());
        faktaBeregningHistorikkHåndterer.lagHistorikkOverstyringInntekt(behandling, dto, aktivtGrunnlag, forrigeGrunnlag);
    }
}
