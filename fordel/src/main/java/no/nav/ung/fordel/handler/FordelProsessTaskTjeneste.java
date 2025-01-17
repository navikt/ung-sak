package no.nav.ung.fordel.handler;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;

@Dependent
public class FordelProsessTaskTjeneste {

    private final ProsessTaskRepository prosessTaskRepository;

    private final MottattMeldingTjeneste meldingTjeneste;

    @Inject
    public FordelProsessTaskTjeneste(ProsessTaskRepository prosessTaskRepository, MottattMeldingTjeneste meldingTjeneste) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.meldingTjeneste = meldingTjeneste;
    }

    public ProsessTaskRepository getProsessTaskRepository() {
        return prosessTaskRepository;
    }

    public MottattMeldingTjeneste getMeldingTjeneste() {
        return meldingTjeneste;
    }

}
