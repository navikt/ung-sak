package no.nav.ung.sak.formidling.bestilling;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

public class BrevbestillingTaskGenerator {

    static final String FORMIDLING_GRUPPE_PREFIX = "formidling-";

    public static ProsessTaskData formidlingProsessTaskIGruppe(Class<? extends ProsessTaskHandler> taskKlasse, Long fagsakId) {
        return formidlingProsessTaskIGruppe(taskKlasse, fagsakId, 0);
    }

    public static ProsessTaskData formidlingProsessTaskIGruppe(Class<? extends ProsessTaskHandler> taskKlasse, Long fagsakId, int indeks) {
        var prosessTaskData = ProsessTaskData.forProsessTask(taskKlasse);
        String nesteSekvens = String.format("%d-%d", System.currentTimeMillis(), indeks);
        prosessTaskData
            .medSekvens(nesteSekvens)
            .medGruppe((FORMIDLING_GRUPPE_PREFIX + "%d").formatted(fagsakId));
        return prosessTaskData;
    }

}
