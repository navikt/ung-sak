package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.tilfeller;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_SN_NY_I_ARBEIDSLIVET")
public class VurderSNNyIArbeidslivetHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto nyIArbeidslivetDto = dto.getVurderNyIArbeidslivet();
        Boolean opprinneligNyIArbeidslivetVerdi = getOpprinneligNyIArbeidslivetVerdi(forrigeGrunnlag);
        lagHistorikkInnslag(nyIArbeidslivetDto, opprinneligNyIArbeidslivetVerdi, tekstBuilder);
    }

    private Boolean getOpprinneligNyIArbeidslivetVerdi(Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        return forrigeGrunnlag
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(bg -> bg.getBeregningsgrunnlagPerioder().get(0))
            .stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(bpsa -> bpsa.getAktivitetStatus().erSelvstendigNæringsdrivende())
            .findFirst()
            .map(BeregningsgrunnlagPrStatusOgAndel::getNyIArbeidslivet)
            .orElse(null);
    }

    private void lagHistorikkInnslag(VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto dto, Boolean opprinneligNyIArbeidslivetVerdi,
                                     HistorikkInnslagTekstBuilder tekstBuilder) {
        oppdaterVedEndretVerdi(HistorikkEndretFeltType.SELVSTENDIG_NÆRINGSDRIVENDE, dto, opprinneligNyIArbeidslivetVerdi, tekstBuilder);
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean erNyIArbeidslivet) {
        if (erNyIArbeidslivet == null) {
            return null;
        }
        return erNyIArbeidslivet ? HistorikkEndretFeltVerdiType.NY_I_ARBEIDSLIVET : HistorikkEndretFeltVerdiType.IKKE_NY_I_ARBEIDSLIVET;
    }

    private void oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto dto,
                                        Boolean opprinneligNyIArbeidslivetVerdi, HistorikkInnslagTekstBuilder tekstBuilder) {
        HistorikkEndretFeltVerdiType opprinneligVerdi = konvertBooleanTilFaktaEndretVerdiType(opprinneligNyIArbeidslivetVerdi);
        HistorikkEndretFeltVerdiType nyVerdi = konvertBooleanTilFaktaEndretVerdiType(dto.erNyIArbeidslivet());
        if(opprinneligVerdi != nyVerdi) {
            tekstBuilder.medEndretFelt(historikkEndretFeltType, opprinneligVerdi, nyVerdi);
        }
    }

}
