package no.nav.ung.sak.formidling.bestilling;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;

public class BrevbestillingTaskGenerator {

    public static ProsessTaskData formidlingProsessTaskIGruppe(Class<? extends ProsessTaskHandler> taskKlasse, Long fagsakId) {
        return formidlingProsessTaskIGruppe(taskKlasse, fagsakId, 0);
    }

    public static ProsessTaskData formidlingProsessTaskIGruppe(Class<? extends ProsessTaskHandler> taskKlasse, Long fagsakId, int indeks) {
        var prosessTaskData = ProsessTaskData.forProsessTask(taskKlasse);
        String nesteSekvens = String.format("%d-%d", System.currentTimeMillis(), indeks);
        prosessTaskData
            .medSekvens(nesteSekvens)
            .medGruppe(("formidling-%d").formatted(fagsakId));
        return prosessTaskData;
    }

}
