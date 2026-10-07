package no.nav.ung.sak.web.server.abac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.ung.sak.behandlingslager.pip.PipRepository;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.web.app.tjenester.ekstern.tilleggsstonader.TilleggsstonaderRestTjeneste;
import no.nav.ung.sak.web.app.konfig.RestApiTester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tilgangsbeslutningen for {@link BeskyttetRessursResourceType#EKSTERN_SYSTEM} tas i k9-felles (PepImpl) og testes der.
 * Her sikres ung-sak sin konfigurasjon: at tilleggsstønader kun får lesetilgang til sitt eget endepunkt,
 * og at annotasjonen stemmer med Nais-oppsettet.
 */
class TilleggsstønaderTilgangTest {

    private static final String TS_NAMESPACE = "tilleggsstonader";
    private static final String TS_APP = "tilleggsstonader-integrasjoner";

    private final AppPdpRequestBuilderImpl requestBuilder = new AppPdpRequestBuilderImpl(mock(PipRepository.class), mock(AktørTjeneste.class));

    @Test
    void endepunktet_skal_kun_gi_lesetilgang_til_tilleggsstonader() throws NoSuchMethodException {
        BeskyttetRessurs annotasjon = tilleggsstonaderEndepunkt().getAnnotation(BeskyttetRessurs.class);

        assertThat(annotasjon.resource()).isEqualTo(BeskyttetRessursResourceType.EKSTERN_SYSTEM);
        assertThat(annotasjon.action()).isEqualTo(BeskyttetRessursActionType.READ);
        assertThat(annotasjon.eksterneSystemer()).containsExactlyInAnyOrder(
            "dev-gcp:tilleggsstonader:tilleggsstonader-integrasjoner",
            "prod-gcp:tilleggsstonader:tilleggsstonader-integrasjoner");
    }

    @Test
    void tilleggsstonader_skal_ikke_ha_tilgang_til_andre_endepunkter() {
        var endepunkterMedTilleggsstonader = RestApiTester.finnAlleRestMetoder().stream()
            .filter(m -> m.getAnnotation(BeskyttetRessurs.class) != null)
            .filter(m -> Arrays.stream(m.getAnnotation(BeskyttetRessurs.class).eksterneSystemer())
                .anyMatch(system -> system.endsWith(":" + TS_NAMESPACE + ":" + TS_APP)))
            .map(m -> m.getDeclaringClass().getSimpleName() + "." + m.getName())
            .toList();

        assertThat(endepunkterMedTilleggsstonader).containsExactly("TilleggsstonaderRestTjeneste.hentAktivitetspengerPerioder");
    }

    @ParameterizedTest
    @ValueSource(strings = {EksterneSystemer.TILLEGGSSTØNADER_DEV, EksterneSystemer.TILLEGGSSTØNADER_PROD})
    void tilleggsstonader_skal_ikke_regnes_som_intern_konsument(String azp) {
        // Ellers ville TS fått systemtilgang til alle endepunkter via PepImpl.vurderTilgangTilApp
        assertThat(requestBuilder.internAzureConsumer(azp)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {EksterneSystemer.TILLEGGSSTØNADER_DEV, EksterneSystemer.TILLEGGSSTØNADER_PROD})
    void eksternt_system_skal_ha_inbound_regel_i_nais_for_samme_cluster(String eksterntSystem) throws IOException {
        var deler = eksterntSystem.split(":");
        var cluster = deler[0];
        var namespace = deler[1];
        var app = deler[2];

        JsonNode regler = new ObjectMapper(new YAMLFactory())
            .readTree(Path.of("..", "deploy", cluster + ".yml").toFile())
            .at("/spec/accessPolicy/inbound/rules");

        List<JsonNode> treff = StreamSupport.stream(regler.spliterator(), false)
            .filter(r -> app.equals(r.path("application").asText()))
            .filter(r -> namespace.equals(r.path("namespace").asText()))
            .filter(r -> r.path("cluster").isMissingNode() || cluster.equals(r.path("cluster").asText()))
            .toList();

        assertThat(treff).as("inbound-regel for %s i deploy/%s.yml", eksterntSystem, cluster).hasSize(1);
    }

    private static Method tilleggsstonaderEndepunkt() throws NoSuchMethodException {
        return Arrays.stream(TilleggsstonaderRestTjeneste.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("hentAktivitetspengerPerioder"))
            .findFirst()
            .orElseThrow(NoSuchMethodException::new);
    }
}
