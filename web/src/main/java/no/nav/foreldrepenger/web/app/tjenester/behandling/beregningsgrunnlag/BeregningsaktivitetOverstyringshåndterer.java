package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.AvklarAktiviteterHåndterer;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.OverstyrBeregningsaktiviteterDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.BeregningsaktivitetHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrBeregningsaktiviteterDto.class, adapter = Overstyringshåndterer.class)
public class BeregningsaktivitetOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyrBeregningsaktiviteterDto> {

    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningsaktivitetHistorikkTjeneste beregningsaktivitetHistorikkTjeneste;
    private AvklarAktiviteterHåndterer avklarAktiviteterHåndterer;

    BeregningsaktivitetOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningsaktivitetOverstyringshåndterer(HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                    HistorikkTjenesteAdapter historikkAdapter,
                                                    BeregningsaktivitetHistorikkTjeneste beregningsaktivitetHistorikkTjeneste,
                                                    AvklarAktiviteterHåndterer avklarAktiviteterHåndterer) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER);
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningsaktivitetHistorikkTjeneste = beregningsaktivitetHistorikkTjeneste;
        this.avklarAktiviteterHåndterer = avklarAktiviteterHåndterer;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrBeregningsaktiviteterDto dto, Behandling behandling,
                                                  BehandlingskontrollKontekst kontekst) {
        avklarAktiviteterHåndterer.håndterOverstyring(dto, behandling.getId());
        return OppdateringResultat.utenOveropp();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyrBeregningsaktiviteterDto dto) {
        BeregningsgrunnlagGrunnlagEntitet grunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler BeregningsgrunnlagGrunnlagEntitet"));
        Optional<Long> originalBehandlingId = behandling.getOriginalBehandling().map(Behandling::getId);
        Optional<BeregningAktivitetAggregatEntitet> forrige = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandling.getId(), originalBehandlingId,
            BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER)
            .map(BeregningsgrunnlagGrunnlagEntitet::getGjeldendeAktiviteter);
        BeregningAktivitetAggregatEntitet registerAktiviteter = grunnlag.getRegisterAktiviteter();
        BeregningAktivitetAggregatEntitet overstyrteAktiviteter = grunnlag.getGjeldendeAktiviteter();
        beregningsaktivitetHistorikkTjeneste.lagHistorikk(behandling.getId(),
            getHistorikkAdapter().tekstBuilder().medHendelse(HistorikkinnslagType.OVERSTYRT),
            registerAktiviteter,
            overstyrteAktiviteter,
            dto.getBegrunnelse(),
            forrige);
    }
}
