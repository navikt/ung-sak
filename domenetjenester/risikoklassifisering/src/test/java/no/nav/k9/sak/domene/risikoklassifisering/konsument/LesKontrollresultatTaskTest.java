package no.nav.k9.sak.domene.risikoklassifisering.konsument;

import no.nav.k9.kodeverk.risikoklassifisering.Kontrollresultat;
import no.nav.k9.sak.domene.risikoklassifisering.json.JsonObjectMapper;
import no.nav.k9.sak.domene.risikoklassifisering.json.KontrollresultatMapper;
import no.nav.k9.sak.domene.risikoklassifisering.tjeneste.KontrollresultatWrapper;
import no.nav.k9.sak.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.kontroll.kodeverk.KontrollResultatkode;
import no.nav.vedtak.kontroll.v1.KontrollResultatV1;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LesKontrollresultatTaskTest {

    private RisikovurderingTjeneste risikovurderingTjeneste;
    private KontrollresultatMapper kontrollresultatMapper;
    private LesKontrollresultatTask task;


    @Before
    public void setup() {
        risikovurderingTjeneste = mock(RisikovurderingTjeneste.class);
        kontrollresultatMapper = mock(KontrollresultatMapper.class);
        task = new LesKontrollresultatTask(risikovurderingTjeneste, kontrollresultatMapper);
    }

    @Test
    public void skal_kalle_mapper_og_tjeneste() throws IOException {
        // Arrange
        KontrollResultatV1.Builder builder = new KontrollResultatV1.Builder();
        UUID uuid = UUID.randomUUID();
        KontrollResultatV1 resultatV1 = builder.medBehandlingUuid(uuid).medResultatkode(KontrollResultatkode.HØY).build();
        ProsessTaskData prosessTaskData = new ProsessTaskData(LesKontrollresultatTask.TASKTYPE);
        prosessTaskData.setPayload(JsonObjectMapper.getJson(resultatV1));
        KontrollresultatWrapper wrapper = new KontrollresultatWrapper(uuid, Kontrollresultat.HØY);
        when(kontrollresultatMapper.fraKontrakt(any(KontrollResultatV1.class))).thenReturn(wrapper);

        // Act
        task.doTask(prosessTaskData);

        // Assert
        verify(kontrollresultatMapper).fraKontrakt(any(KontrollResultatV1.class));
        verify(risikovurderingTjeneste).lagreKontrollresultat(wrapper);

    }

}
