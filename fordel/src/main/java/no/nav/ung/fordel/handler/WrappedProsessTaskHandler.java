package no.nav.ung.fordel.handler;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

public abstract class WrappedProsessTaskHandler implements ProsessTaskHandler {

    private final FordelProsessTaskTjeneste tjenester;

    public WrappedProsessTaskHandler(FordelProsessTaskTjeneste fordelProsessTaskTjeneste) {
        this.tjenester = fordelProsessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        MottattMelding mm = new MottattMelding(prosessTaskData);

        tjenester.getMeldingTjeneste().initLogContext(mm);

        precondition(mm);

        MottattMelding nesteStegMm = doTask(mm);

        if (nesteStegMm != null) {
            tjenester.getMeldingTjeneste().oppdaterMottattMelding(nesteStegMm);
            postcondition(nesteStegMm);
            tjenester.getProsessTaskTjeneste().lagre(nesteStegMm.getProsessTaskData());
        }
    }

    public abstract void precondition(MottattMelding dataWrapper);

    public void postcondition(@SuppressWarnings("unused") MottattMelding dataWrapper) {
        // Override i subtasks hvor det er krav til precondition. Det er typisk i tasker hvor tasken henter data og det er behov for å sjekke at alt
        // er OK etter at task er kjørt.
    }

    public abstract MottattMelding doTask(MottattMelding dataWrapper);
}
