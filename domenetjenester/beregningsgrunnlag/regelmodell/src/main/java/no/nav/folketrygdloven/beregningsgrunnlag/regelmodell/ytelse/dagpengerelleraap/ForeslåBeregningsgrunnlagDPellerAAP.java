package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.ytelse.dagpengerelleraap;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.BeregningsgrunnlagHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektskilde;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Periodeinntekt;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrStatus;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(ForeslåBeregningsgrunnlagDPellerAAP.ID)
class ForeslåBeregningsgrunnlagDPellerAAP extends LeafSpecification<BeregningsgrunnlagPeriode> {

    static final String ID = "FP_BR_10.1";
    static final String BESKRIVELSE = "Foreslå beregningsgrunnlag for Dagpenger/AAP";

    ForeslåBeregningsgrunnlagDPellerAAP() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsgrunnlagPeriode grunnlag) {
        BeregningsgrunnlagPrStatus bgPerStatus = grunnlag.getBeregningsgrunnlagPrStatus().stream()
            .filter(bgps -> bgps.getAktivitetStatus().erAAPellerDP())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Ingen aktivitetstatus av type DP eller AAP funnet."));
        Periodeinntekt inntekt = grunnlag.getInntektsgrunnlag().getPeriodeinntekt(Inntektskilde.TILSTØTENDE_YTELSE_DP_AAP, grunnlag.getSkjæringstidspunkt())
            .orElseThrow(() -> new IllegalStateException("Ingen inntekter fra tilstøtende ytelser funnet i siste måned med inntekt"));
        BigDecimal antallPerioderPrÅr = inntekt.getInntektPeriodeType().getAntallPrÅr();
        BigDecimal beregnetPrÅr = inntekt.getInntekt().multiply(antallPerioderPrÅr);
        Long originalDagsats = inntekt.getInntekt().longValue();
        BeregningsgrunnlagPrStatus.builder(bgPerStatus)
            .medBeregnetPrÅr(beregnetPrÅr)
            .medÅrsbeløpFraTilstøtendeYtelse(beregnetPrÅr)
            .medOrginalDagsatsFraTilstøtendeYtelse(originalDagsats)
            .build();

        BeregningsgrunnlagHjemmel hjemmel = AktivitetStatus.AAP.equals(bgPerStatus.getAktivitetStatus()) ? BeregningsgrunnlagHjemmel.F_14_7
            : BeregningsgrunnlagHjemmel.F_14_7_8_49;
        grunnlag.getBeregningsgrunnlag().getAktivitetStatus(bgPerStatus.getAktivitetStatus()).setHjemmel(hjemmel);

        Map<String, Object> resultater = new HashMap<>();
        resultater.put("beregnetPrÅr." + bgPerStatus.getAktivitetStatus().name(), beregnetPrÅr);
        resultater.put("tilstøtendeYtelserPrÅr." + bgPerStatus.getAktivitetStatus().name(), beregnetPrÅr);
        resultater.put("hjemmel", hjemmel);
        return beregnet(resultater);
    }
}
