package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.ytelse.dagpengerelleraap;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatusMedHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkOmBrukerKunHarStatusDPellerAAP.ID)
class SjekkOmBrukerKunHarStatusDPellerAAP extends LeafSpecification<BeregningsgrunnlagPeriode> {

    static final String ID = "FP_BR_10.3";
    static final String BESKRIVELSE = "Har bruker kun status dagpenger/AAP?";

    SjekkOmBrukerKunHarStatusDPellerAAP() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsgrunnlagPeriode grunnlag) {
        List<AktivitetStatus> aktivitetStatuser = grunnlag.getAktivitetStatuser().stream().map(AktivitetStatusMedHjemmel::getAktivitetStatus).collect(Collectors.toList());
        if (!(aktivitetStatuser.contains(AktivitetStatus.DP) || aktivitetStatuser.contains(AktivitetStatus.AAP))) {
            throw new IllegalStateException("Utviklerfeil: Skal ikke inntreffe. Ingen aktivitetstatuser funnet med aktivitetstatus DP eller AAP.");
        }
        return aktivitetStatuser.size() == 1 ? ja() : nei();
    }
}
