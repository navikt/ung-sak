package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.tilfeller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FaktaBeregningLagreDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettEtterlønnSluttpakkeDto;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("FASTSETT_ETTERLØNN_SLUTTPAKKE")
public class EtterlønnSluttpakkeHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private static final BigDecimal MÅNEDER_I_ET_ÅR = BigDecimal.valueOf(12);

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        lagHistorikkForFastsetting(dto, tekstBuilder, forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
    }

    private void lagHistorikkForFastsetting(FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        FastsettEtterlønnSluttpakkeDto fastettDto = dto.getFastsettEtterlønnSluttpakke();
        Integer fastsattPrMnd = fastettDto.getFastsattPrMnd();
        BigDecimal fastsattPrMndForrige = forrigeBg.map(this::hentOpprinneligVerdiFastsattEtterlønnSluttpakke).orElse(null);
        lagHistorikkInnslagFastsattEtterlønnSluttpakke(BigDecimal.valueOf(fastsattPrMnd), fastsattPrMndForrige, tekstBuilder);
    }

    private BigDecimal hentOpprinneligVerdiFastsattEtterlønnSluttpakke(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        List<BeregningsgrunnlagPrStatusOgAndel> etterlønnSluttpakkeAndeler = finnEtterlønnSluttpakkeAndel(beregningsgrunnlag);
        if (etterlønnSluttpakkeAndeler.get(0).getBeregnetPrÅr() == null) {
            return null;
        }
        return etterlønnSluttpakkeAndeler.get(0).getBeregnetPrÅr().divide(MÅNEDER_I_ET_ÅR, 0, RoundingMode.HALF_UP);
    }

    private List<BeregningsgrunnlagPrStatusOgAndel> finnEtterlønnSluttpakkeAndel(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag) {
        return nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(bpsa -> bpsa.getArbeidsforholdType().equals(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE))
            .collect(Collectors.toList());
    }

    private void lagHistorikkInnslagFastsattEtterlønnSluttpakke(BigDecimal nyVerdiEtterlønnSLuttpakke, BigDecimal opprinneligEtterlønnSluttpakkeInntekt,
                                                                HistorikkInnslagTekstBuilder tekstBuilder) {
        Integer opprinneligInntektInt = opprinneligEtterlønnSluttpakkeInntekt == null ? null : opprinneligEtterlønnSluttpakkeInntekt.intValue();
        oppdaterVedEndretVerdi(nyVerdiEtterlønnSLuttpakke, opprinneligInntektInt, tekstBuilder);
    }

    private void oppdaterVedEndretVerdi(BigDecimal nyVerdiEtterlønnSLuttpakke, Integer opprinneligEtterlønnSluttpakkeInntekt, HistorikkInnslagTekstBuilder tekstBuilder) {
        if (!Objects.equals(nyVerdiEtterlønnSLuttpakke.intValue(), opprinneligEtterlønnSluttpakkeInntekt)) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_ETTERLØNN_SLUTTPAKKE, opprinneligEtterlønnSluttpakkeInntekt, nyVerdiEtterlønnSLuttpakke.intValue());
        }
    }
}
