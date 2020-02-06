package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.tilfeller;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FaktaBeregningLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderLønnsendringDto;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_LØNNSENDRING")
public class VurderLønnsendringHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (!dto.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING)) {
            return;
        }
        VurderLønnsendringDto lønnsendringDto = dto.getVurdertLonnsendring();
        Boolean opprinneligVerdiErLønnsendring = hentOpprinneligVerdiErLønnsendring(forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
        lagHistorikkinnslag(lønnsendringDto, opprinneligVerdiErLønnsendring, tekstBuilder);
    }


    private Boolean hentOpprinneligVerdiErLønnsendring(Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        return forrigeBg.stream()
            .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .map(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(bgAndelArbeidsforhold -> bgAndelArbeidsforhold.erLønnsendringIBeregningsperioden() != null)
            .map(BGAndelArbeidsforhold::erLønnsendringIBeregningsperioden)
            .findFirst()
            .orElse(null);
    }

    private void lagHistorikkinnslag(VurderLønnsendringDto dto, Boolean opprinneligVerdiErLønnsendring, HistorikkInnslagTekstBuilder tekstBuilder) {
        if (!dto.erLønnsendringIBeregningsperioden().equals(opprinneligVerdiErLønnsendring)) {
            tekstBuilder
                .medEndretFelt(HistorikkEndretFeltType.LØNNSENDRING_I_PERIODEN, opprinneligVerdiErLønnsendring, dto.erLønnsendringIBeregningsperioden());
        }
    }
}
