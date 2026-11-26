package no.nav.ung.sak.web.server.abac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.ung.sak.behandlingslager.pip.PipRepository;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.web.app.konfig.RestApiTester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tilgangsbeslutningen for {@link BeskyttetRessursResourceType#EKSTERN_SYSTEM} tas i k9-felles (PepImpl) og testes der.
 * Her sikres ung-sak sin konfigurasjon: at hvert eksterne system kun får lesetilgang til sine egne endepunkter,
 * og at annotasjonene stemmer med Nais-oppsettet.
 * <p>
 * Systemene står som tekst og ikke som konstanter, slik at en endret konstant også gir feil her.
 */
class EksterneSystemerTilgangTest {

    private static final String TS_PERIODER = "TilleggsstonaderRestTjeneste.hentAktivitetspengerPerioder";
    private static final String YTELSE_VEDTAK = "EksternYtelseRestTjeneste.hentVedtak";
    private static final String YTELSE_BARNETILLEGG = "EksternYtelseRestTjeneste.hentBarnetillegg";

    private static final Map<String, Set<String>> TILGANGER = Map.of(
        "dev-gcp:tilleggsstonader:tilleggsstonader-integrasjoner", Set.of(TS_PERIODER),
        "prod-gcp:tilleggsstonader:tilleggsstonader-integrasjoner", Set.of(TS_PERIODER),
        "dev-gcp:aap:behandlingsflyt", Set.of(YTELSE_VEDTAK),
        "dev-gcp:teamdagpenger:dp-oppslag-ytelser", Set.of(YTELSE_VEDTAK),
        "dev-gcp:tpts:tiltakspenger-saksbehandling-api", Set.of(YTELSE_VEDTAK),
        "dev-gcp:bidrag:bidrag-grunnlag", Set.of(YTELSE_BARNETILLEGG),
        "dev-gcp:bidrag:bidrag-grunnlag-feature", Set.of(YTELSE_BARNETILLEGG),
        "dev-gcp:nais:azure-token-generator", Set.of(YTELSE_VEDTAK, YTELSE_BARNETILLEGG));

    private static final String AZURE_TOKEN_GENERATOR = ":nais:azure-token-generator";

    private final AppPdpRequestBuilderImpl requestBuilder = new AppPdpRequestBuilderImpl(mock(PipRepository.class), mock(AktørTjeneste.class));

    @Test
    void eksterne_systemer_skal_kun_ha_tilgang_til_sine_egne_endepunkter() {
        Map<String, Set<String>> faktiskeTilganger = new TreeMap<>();
        for (Method metode : endepunkterForEksterneSystemer()) {
            for (String system : metode.getAnnotation(BeskyttetRessurs.class).eksterneSystemer()) {
                faktiskeTilganger.computeIfAbsent(system, s -> new TreeSet<>()).add(navn(metode));
            }
        }

        assertThat(faktiskeTilganger).isEqualTo(new TreeMap<>(TILGANGER));
    }

    @Test
    void endepunkter_for_eksterne_systemer_skal_kun_gi_lesetilgang() {
        assertThat(endepunkterForEksterneSystemer()).isNotEmpty().allSatisfy(metode -> {
            var annotasjon = metode.getAnnotation(BeskyttetRessurs.class);
            assertThat(annotasjon.resource()).as(navn(metode)).isEqualTo(BeskyttetRessursResourceType.EKSTERN_SYSTEM);
            assertThat(annotasjon.action()).as(navn(metode)).isEqualTo(BeskyttetRessursActionType.READ);
        });
    }

    @Test
    void alle_konstanter_i_eksterne_systemer_skal_være_i_bruk() {
        var konstanter = Arrays.stream(EksterneSystemer.class.getDeclaredFields())
            .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
            .map(EksterneSystemerTilgangTest::verdi)
            .collect(Collectors.toSet());

        assertThat(konstanter).isEqualTo(TILGANGER.keySet());
    }

    @Test
    void azure_token_generator_skal_aldri_ha_tilgang_i_prod() throws IOException {
        assertThat(TILGANGER.keySet().stream().filter(s -> s.endsWith(AZURE_TOKEN_GENERATOR)))
            .allMatch(s -> s.startsWith("dev-gcp:"));
        assertThat(inboundRegler("prod-gcp").stream().map(r -> r.path("application").asText()))
            .doesNotContain("azure-token-generator");
    }

    @ParameterizedTest
    @MethodSource("eksterneSystemer")
    void eksternt_system_skal_ikke_regnes_som_intern_konsument(String system) {
        // Ellers ville systemet fått systemtilgang til alle endepunkter via PepImpl.vurderTilgangTilApp
        assertThat(requestBuilder.internAzureConsumer(system)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("eksterneSystemer")
    void eksternt_system_skal_ha_inbound_regel_i_nais_for_samme_cluster(String system) throws IOException {
        var deler = system.split(":");
        var cluster = deler[0];
        var namespace = deler[1];
        var app = deler[2];

        var treff = inboundRegler(cluster).stream()
            .filter(r -> app.equals(r.path("application").asText()))
            .filter(r -> namespace.equals(r.path("namespace").asText()))
            .filter(r -> r.path("cluster").isMissingNode() || cluster.equals(r.path("cluster").asText()))
            .toList();

        assertThat(treff).as("inbound-regel for %s i deploy/%s.yml", system, cluster).hasSize(1);
    }

    static Stream<String> eksterneSystemer() {
        return TILGANGER.keySet().stream().sorted();
    }

    private static List<Method> endepunkterForEksterneSystemer() {
        return RestApiTester.finnAlleRestMetoder().stream()
            .filter(m -> m.getAnnotation(BeskyttetRessurs.class) != null)
            .filter(m -> m.getAnnotation(BeskyttetRessurs.class).eksterneSystemer().length > 0
                || m.getAnnotation(BeskyttetRessurs.class).resource() == BeskyttetRessursResourceType.EKSTERN_SYSTEM)
            .toList();
    }

    private static List<JsonNode> inboundRegler(String cluster) throws IOException {
        JsonNode regler = new ObjectMapper(new YAMLFactory())
            .readTree(Path.of("..", "deploy", cluster + ".yml").toFile())
            .at("/spec/accessPolicy/inbound/rules");
        return StreamSupport.stream(regler.spliterator(), false).toList();
    }

    private static String navn(Method metode) {
        return metode.getDeclaringClass().getSimpleName() + "." + metode.getName();
    }

    private static String verdi(Field felt) {
        try {
            return (String) felt.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
