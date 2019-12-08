package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.tilfeller;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderNyoppstartetFLDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_NYOPPSTARTET_FL")
public class VurderNyoppstartetFLHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        VurderNyoppstartetFLDto nyoppstartetDto = dto.getVurderNyoppstartetFL();
        Boolean opprinneligErNyoppstartetFLVerdi = getOpprinneligErNyoppstartetFLVerdi(forrigeGrunnlag);
        lagHistorikkInnslag(nyoppstartetDto, opprinneligErNyoppstartetFLVerdi, tekstBuilder);
    }

    private Boolean getOpprinneligErNyoppstartetFLVerdi(Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        return forrigeGrunnlag
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(bg -> bg.getBeregningsgrunnlagPerioder().get(0))
            .stream()
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(bpsa -> bpsa.getAktivitetStatus().erFrilanser())
            .findFirst()
            .flatMap(BeregningsgrunnlagPrStatusOgAndel::erNyoppstartet)
            .orElse(null);
    }

    private void lagHistorikkInnslag(VurderNyoppstartetFLDto dto, Boolean opprinneligErNyoppstartetFLVerdi, HistorikkInnslagTekstBuilder tekstBuilder) {
        oppdaterVedEndretVerdi(dto, opprinneligErNyoppstartetFLVerdi, tekstBuilder);
    }

    private void oppdaterVedEndretVerdi(VurderNyoppstartetFLDto dto,
                                        Boolean opprinneligNyoppstartetFLVerdi, HistorikkInnslagTekstBuilder tekstBuilder) {
        HistorikkEndretFeltVerdiType opprinneligVerdi = konvertBooleanTilFaktaEndretVerdiType(opprinneligNyoppstartetFLVerdi);
        HistorikkEndretFeltVerdiType nyVerdi = konvertBooleanTilFaktaEndretVerdiType(dto.erErNyoppstartetFL());
        if(opprinneligVerdi != nyVerdi) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FRILANSVIRKSOMHET, opprinneligVerdi, nyVerdi);
        }
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean erNyoppstartet) {
        if (erNyoppstartet == null) {
            return null;
        }
        return erNyoppstartet ? HistorikkEndretFeltVerdiType.NYOPPSTARTET : HistorikkEndretFeltVerdiType.IKKE_NYOPPSTARTET;
    }

}
