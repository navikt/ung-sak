package no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.erpartisaken;

import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.web.app.tjenester.brukerdialog.ErPartISakenGrunnlag;

public class ErPartISakenVilkår implements RuleService<ErPartISakenGrunnlag> {
    private AktørId brukerAktørId;
    private AktørId pleietrengendeAktørId;

    public ErPartISakenVilkår(AktørId brukerAktørId, AktørId pleietrengendeAktørId) {
        this.brukerAktørId = brukerAktørId;
        this.pleietrengendeAktørId = pleietrengendeAktørId;
    }

    @Override
    public Evaluation evaluer(ErPartISakenGrunnlag erPartISakenGrunnlag) {
        return getSpecification().evaluate(erPartISakenGrunnlag);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<ErPartISakenGrunnlag> getSpecification() {
        Ruleset<ErPartISakenGrunnlag> behandlingContextRuleset = new Ruleset<>();
        return behandlingContextRuleset.regel(
                "sif.brukerdialog.1",
                "AktørType må være part i saken",
                new ErPartISaken(this.brukerAktørId, ErPartISaken.AktørType.BRUKER_AKTØR)
                    .og(new ErPartISaken(this.pleietrengendeAktørId, ErPartISaken.AktørType.PLEIETRENGENDE_AKTØR))
        );
    }
}
