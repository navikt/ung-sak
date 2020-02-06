package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.BeregningFaktaOgOverstyringHåndterer;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.FaktaBeregningHistorikkHåndterer;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.VurderFaktaOmBeregningDto;


@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaktaOmBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmBeregningOppdaterer implements AksjonspunktOppdaterer<VurderFaktaOmBeregningDto> {

    private BeregningFaktaOgOverstyringHåndterer beregningFaktaOgOverstyringHåndterer;
    private FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer;
    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    VurderFaktaOmBeregningOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBeregningOppdaterer(BeregningFaktaOgOverstyringHåndterer beregningFaktaOgOverstyringHåndterer,
                                            FaktaBeregningHistorikkHåndterer faktaBeregningHistorikkHåndterer,
                                            HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.beregningFaktaOgOverstyringHåndterer = beregningFaktaOgOverstyringHåndterer;
        this.faktaBeregningHistorikkHåndterer = faktaBeregningHistorikkHåndterer;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBeregningDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        var behandlingReferanse = param.getRef();
        Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag = beregningsgrunnlagTjeneste
            .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
                behandling.getId(),
                behandling.getOriginalBehandling().map(Behandling::getId),
                BeregningsgrunnlagTilstand.KOFAKBER_UT);
        beregningFaktaOgOverstyringHåndterer.håndter(behandlingReferanse, dto);
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = beregningsgrunnlagTjeneste
            .hentSisteBeregningsgrunnlagGrunnlagEntitet(behandling.getId(), BeregningsgrunnlagTilstand.KOFAKBER_UT)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .orElseThrow(() -> new IllegalStateException("Skal ha lagret beregningsgrunnlag fra KOFAKBER_UT."));
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        faktaBeregningHistorikkHåndterer.lagHistorikk(param, dto, nyttBeregningsgrunnlag, forrigeGrunnlag, inntektArbeidYtelseGrunnlag);
        return OppdateringResultat.utenOveropp();
    }

}
