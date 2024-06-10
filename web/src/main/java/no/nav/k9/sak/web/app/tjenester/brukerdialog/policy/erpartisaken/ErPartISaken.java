package no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.erpartisaken;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.node.SingleEvaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.web.app.tjenester.brukerdialog.ErPartISakenGrunnlag;

import java.util.List;

public class ErPartISaken extends LeafSpecification<ErPartISakenGrunnlag> {
    private static final String ID = "sif.brukerdialog.1";

    public static ErPartISaken erPartISaken(AktørId aktørId, AktørType aktørType) {
        return new ErPartISaken(aktørId, aktørType);
    }

    private AktørId aktørId;
    private AktørType aktørType;

    public ErPartISaken(AktørId aktørId, AktørType aktørType) {
        super(ID);
        this.aktørId = aktørId;
        this.aktørType = aktørType;
    }

    @Override
    public SingleEvaluation endeligBeregnet(RuleReasonRef reasonKey, Object... reasonArgs) {
        return super.endeligBeregnet(reasonKey, reasonArgs);
    }

    @Override
    public Evaluation evaluate(ErPartISakenGrunnlag erPartISakenGrunnlag) {
        List<AktørId> parterISaken = erPartISakenGrunnlag.parterISaken();
        if (parterISaken == null) {
            return kanIkkeVurdere(new ErPartISakenRuleReason(ErPartISakenUtfall.IKKE_VURDERT, ID, "Behandling er null"));
        }

        boolean erPartISaken = parterISaken.stream()
            .anyMatch(a -> a.getAktørId().equals(aktørId.getAktørId()));

        if (!erPartISaken) {
            return nei(new ErPartISakenRuleReason(ErPartISakenUtfall.NEI, ID, aktørType + " er ikke part i saken"));
        }
        return ja(new ErPartISakenRuleReason(ErPartISakenUtfall.JA, ID, aktørType + " er part i saken"));
    }

    public enum AktørType {
        BRUKER_AKTØR,
        PLEIETRENGENDE_AKTØR,
    }

    public enum ErPartISakenUtfall {
        JA,
        NEI,
        IKKE_VURDERT
    }

    public record ErPartISakenRuleReason(ErPartISakenUtfall utfall, String reasonCode, String reason) implements RuleReasonRef {

        @Override
        public String getReasonTextTemplate() {
            return reason;
        }

        @Override
        public String getReasonCode() {
            return reasonCode;
        }
    }
}
