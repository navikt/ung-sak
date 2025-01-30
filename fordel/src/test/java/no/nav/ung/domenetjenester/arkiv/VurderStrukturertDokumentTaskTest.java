package no.nav.ung.domenetjenester.arkiv;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.sak.typer.JournalpostId;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class VurderStrukturertDokumentTaskTest {

    @Test
    public void skal_hente_ut_informasjon_og_rute_meldingen_videre() {
        var payload = hentUtString("søknader/ungdomsytelse/søknadMedId.json");
        var data = new ProsessTaskData(VurderStrukturertDokumentTask.class);
        data.setSekvens("1");
        data.setPayload(payload);

        var melding = new MottattMelding(data);
        melding.setTema(OmrådeTema.OMS);
        melding.setJournalPostId(new JournalpostId("4247"));

        var task = new VurderStrukturertDokumentTask(null);

        var mottattMelding = task.doTask(melding);

        assertThat(mottattMelding).isNotNull();
        assertThat(mottattMelding.getFørsteUttaksdag()).isPresent().hasValue(LocalDate.parse("2021-01-21"));
    }

    @Test
    public void skal_hente_ut_informasjon_for_inntektrapportering_og_rute_meldingen_videre() {
        var payload = hentUtString("søknader/ungdomsytelse/innrapporteringInntekt.json");
        var data = new ProsessTaskData(VurderStrukturertDokumentTask.class);
        data.setSekvens("1");
        data.setPayload(payload);

        var melding = new MottattMelding(data);
        melding.setTema(OmrådeTema.OMS);
        melding.setJournalPostId(new JournalpostId("4247"));

        var task = new VurderStrukturertDokumentTask(null);

        var mottattMelding = task.doTask(melding);

        assertThat(mottattMelding).isNotNull();
        assertThat(mottattMelding.getFørsteUttaksdag()).isPresent().hasValue(LocalDate.parse("2021-01-21"));
    }


    private String hentUtString(String name) {
        InputStream inputStream = VurderStrukturertDokumentTaskTest.class.getClassLoader().getResourceAsStream(name);
        if (inputStream == null) throw new IllegalArgumentException("Finner ikke noe");
        try {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Feil ved lesing av json", e);
        }
    }
}
