package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.Barn;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.BehovRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.BehovResponse;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.BehovType;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.Json;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.KroniskSyktBarn;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.RawJson;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.StatusType;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.Søker;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;

public class UtvidetRettModellTest {

    private ObjectMapper om = Json.getObjectMapper();

    @Test
    void skal_genrere_KroniskSyktBarnSøknadRequest_json() throws Exception {
        var barn = new Barn(NorskIdentitetsnummer.of("1234"), LocalDate.now(), true);
        var søker = new Søker(NorskIdentitetsnummer.of("4321"));

        var req = new KroniskSyktBarn()
            .setBehandlingUuid(UUID.randomUUID())
            .setSaksnummer(new Saksnummer("ABC"))
            .setTidspunkt(ZonedDateTime.now())
            .setSøknadMottatt(ZonedDateTime.now().minusDays(1))
            .setSøker(søker)
            .setBarn(barn);

        var json = om.writerWithDefaultPrettyPrinter().writeValueAsString(req);

        var parsed = om.readValue(json, KroniskSyktBarn.class);

        assertThat(parsed).isNotNull();
    }

    @Test
    void skal_verfisere_mot_eksisterende_KroniskSyktBarnSøknadRequest_json() throws Exception {
        String json = getJson("/utvidetrett/KroniskSyktBarnSøknadRequest.json");
        var parsed = om.readValue(json, KroniskSyktBarn.class);
        assertThat(parsed).isNotNull();
    }

    @Test
    void skal_generere_BehovRequest_json() throws Exception {
        var my = new BehovRequest()
            .setBehov(Map.of(BehovType.LEGEERKLÆRING, new RawJson("{ \"world\" : \"World\" }")));
        var json = om.writerWithDefaultPrettyPrinter().writeValueAsString(my);
        System.out.println(json);
        var parsed = om.readValue(json, BehovRequest.class);
        assertThat(parsed).isNotNull();
        assertThat(parsed.getBehov()).isNotEmpty();
    }

    @Test
    void skal_verifisere_eksisterende_BehovRequest_json() throws Exception {
        String json = getJson("/utvidetrett/BehovRequest.json");
        var parsed = om.readValue(json, BehovRequest.class);
        assertThat(parsed).isNotNull();
        assertThat(parsed.getBehov()).isNotEmpty();
    }

    @Test
    void skal_generere_BehovResponse_json() throws Exception {
        var my = new BehovResponse()
            .setStatus(StatusType.FORESLÅTT)
            .setPotensielleStatuser(Map.of(StatusType.INNVILGET, new RawJson("{ \"hello\" : \"Hello\" }")))
            .setUløsteBehov(Map.of(BehovType.LEGEERKLÆRING, new RawJson("{ \"world\" : \"World\" }")));

        var json = om.writerWithDefaultPrettyPrinter().writeValueAsString(my);

        var parsed = om.readValue(json, BehovResponse.class);

        assertThat(parsed).isNotNull();
        assertThat(parsed.getStatus()).isNotNull();
        assertThat(parsed.getUløsteBehov()).isNotEmpty();
        assertThat(parsed.getUløsteBehov().entrySet()).allMatch(e -> e.getValue() != null);
        assertThat(parsed.getPotensielleStatuser()).isNotEmpty();
        assertThat(parsed.getPotensielleStatuser().entrySet()).allMatch(e -> e.getValue() != null);
    }

    @Test
    void skal_verifisere_eksisterende_BehovResponse_json() throws Exception {
        String json = getJson("/utvidetrett/BehovResponse.json");
        var parsed = om.readValue(json, BehovResponse.class);
        assertThat(parsed).isNotNull();
        assertThat(parsed.getStatus()).isNotNull();
        assertThat(parsed.getUløsteBehov().entrySet()).allMatch(e -> e.getValue() != null);
        assertThat(parsed.getPotensielleStatuser().entrySet()).allMatch(e -> e.getValue() != null);
    }

    private String getJson(String fileName) throws IOException, URISyntaxException {
        return new String(Files.readAllBytes(Paths.get(getClass().getResource(fileName).toURI())));
    }

}
