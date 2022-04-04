package no.nav.k9.sak.domene.registerinnhenting.task;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;

public class DiffOgReposisjonerTaskTest {

    private DiffOgReposisjonerTask task = new DiffOgReposisjonerTask();

    @Test
    public void skal_få_ut_snapshot_fra_json() throws IOException {
        var data =  ProsessTaskData.forProsessTask(DiffOgReposisjonerTask.class);
        var preSnapshot = EndringsresultatSnapshot.medSnapshot(PersonopplysningGrunnlagEntitet.class, 1L);
        data.setPayload(JsonObjectMapper.getJson(preSnapshot));

        var endringsresultatSnapshot = task.hentUtSnapshotFraPayload(data);

        assertThat(endringsresultatSnapshot).isEqualTo(preSnapshot);
    }

    @Test
    public void skal_få_ut_snapshot_fra_json_1() {
        var data =  ProsessTaskData.forProsessTask(DiffOgReposisjonerTask.class);

        var endringsresultatSnapshot = task.hentUtSnapshotFraPayload(data);

        assertThat(endringsresultatSnapshot).isEqualTo(EndringsresultatSnapshot.opprett());
    }
}
