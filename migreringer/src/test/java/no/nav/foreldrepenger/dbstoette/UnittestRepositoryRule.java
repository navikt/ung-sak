package no.nav.foreldrepenger.dbstoette;

import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class UnittestRepositoryRule extends RepositoryRule {

    static {
        Databaseskjemainitialisering.settPlaceholdereOgJdniOppslag();
    }

    public UnittestRepositoryRule() {
        super();
    }

    @Override
    protected void init() {
    }

}
