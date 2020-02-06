package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;


import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.AvklarAktiviteterHåndterer;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.BeregningsaktivitetHistorikkTjeneste;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.AvklarteAktiviteterDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarteAktiviteterDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarAktiviteterOppdaterer implements AksjonspunktOppdaterer<AvklarteAktiviteterDto> {

    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningsaktivitetHistorikkTjeneste beregningsaktivitetHistorikkTjeneste;
    private AvklarAktiviteterHåndterer avklarAktiviteterHåndterer;

    AvklarAktiviteterOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarAktiviteterOppdaterer(HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                       BeregningsaktivitetHistorikkTjeneste beregningsaktivitetHistorikkTjeneste,
                                       AvklarAktiviteterHåndterer avklarAktiviteterHåndterer) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningsaktivitetHistorikkTjeneste = beregningsaktivitetHistorikkTjeneste;
        this.avklarAktiviteterHåndterer = avklarAktiviteterHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(AvklarteAktiviteterDto dto, AksjonspunktOppdaterParameter param) {

        Optional<Long> originalBehandlingId = param.getRef().getOriginalBehandlingId();
        Long behandlingId = param.getBehandlingId();
        Optional<BeregningAktivitetAggregatEntitet> forrige = beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandlingId, originalBehandlingId,
            BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getSaksbehandletAktiviteter);

        avklarAktiviteterHåndterer.håndter(dto, param.getRef());

        BeregningsgrunnlagGrunnlagEntitet lagretGrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Har ikke et aktivt grunnlag"));
        BeregningAktivitetAggregatEntitet registerAktiviteter = lagretGrunnlag.getRegisterAktiviteter();
        BeregningAktivitetAggregatEntitet saksbehandledeAktiviteter = lagretGrunnlag.getSaksbehandletAktiviteter()
            .orElseThrow(() -> new IllegalStateException("Forventer å ha lagret ned saksbehandlet grunnlag"));
        beregningsaktivitetHistorikkTjeneste.lagHistorikk(behandlingId, registerAktiviteter, saksbehandledeAktiviteter, dto.getBegrunnelse(), forrige);

        return OppdateringResultat.utenOveropp();
    }

}
