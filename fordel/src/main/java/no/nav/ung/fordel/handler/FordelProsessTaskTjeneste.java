package no.nav.ung.fordel.handler;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;

@Dependent
public class FordelProsessTaskTjeneste {

    private final ProsessTaskTjeneste prosessTaskTjeneste;

    private final MottattMeldingTjeneste meldingTjeneste;

    @Inject
    public FordelProsessTaskTjeneste(ProsessTaskTjeneste prosessTaskTjeneste, MottattMeldingTjeneste meldingTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.meldingTjeneste = meldingTjeneste;
    }

    public ProsessTaskTjeneste getProsessTaskTjeneste() {
        return prosessTaskTjeneste;
    }

    public MottattMeldingTjeneste getMeldingTjeneste() {
        return meldingTjeneste;
    }

}
