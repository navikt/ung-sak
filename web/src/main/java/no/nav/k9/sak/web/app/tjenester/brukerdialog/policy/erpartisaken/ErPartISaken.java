package no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.erpartisaken;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.node.SingleEvaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.web.app.tjenester.brukerdialog.ErPartISakenGrunnlag;

public class ErPartISaken extends LeafSpecification<ErPartISakenGrunnlag> {
    private static final String ID = "sif.brukerdialog.1";

    public static ErPartISaken erPartISaken(AktørId aktørId, Aktør aktør) {
        return new ErPartISaken(aktørId, aktør);
    }

    private AktørId aktørId;
    private Aktør aktør;

    public ErPartISaken(AktørId aktørId, Aktør aktør) {
        super(ID);
        this.aktørId = aktørId;
        this.aktør = aktør;
    }

    @Override
    public SingleEvaluation endeligBeregnet(RuleReasonRef reasonKey, Object... reasonArgs) {
        return super.endeligBeregnet(reasonKey, reasonArgs);
    }

    @Override
    public Evaluation evaluate(ErPartISakenGrunnlag erPartISakenGrunnlag) {
        Behandling behandling = erPartISakenGrunnlag.behandling();
        if (behandling == null) {
            return kanIkkeVurdere(new ErPartISakenRuleReason(ErPartISakenUtfall.IKKE_VURDERT, ID, "Behandling er null"));
        }

        boolean erPartISaken = behandling.getFagsak().parterISaken()
            .filter(java.util.Objects::nonNull)
            .anyMatch(a -> a.getAktørId().equals(aktørId.getAktørId()));

        if (!erPartISaken) {
            return nei(new ErPartISakenRuleReason(ErPartISakenUtfall.NEI, ID, aktør + " er ikke part i saken"));
        }
        return ja(new ErPartISakenRuleReason(ErPartISakenUtfall.JA, ID, aktør + " er part i saken"));
    }

    public enum Aktør {
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
