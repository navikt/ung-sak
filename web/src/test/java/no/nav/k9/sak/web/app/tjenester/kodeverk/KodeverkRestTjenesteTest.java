package no.nav.k9.sak.web.app.tjenester.kodeverk;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.sak.web.app.jackson.ObjectMapperFactory;
import no.nav.k9.sak.web.app.tjenester.kodeverk.dto.AlleKodeverdierSomObjektResponse;
import no.nav.k9.sak.web.app.tjenester.kodeverk.dto.KodeverdiSomObjekt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class KodeverkRestTjenesteTest {

    @Inject
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    /**
     * Denne test sjekker at gammalt endepunkt for kodeverk returnerer det samme som før etter at det er implementert
     * ved å kalle det nye endepunktet. Test og tilhøyrande json fil kan slettast når det har gått gjennom pipeline ein gang.
     */
    @Test
    public void nytt_endepunkt_returnerer_samme_som_gamle() throws IOException {
        KodeverkRestTjeneste tjeneste = new KodeverkRestTjeneste(behandlendeEnhetTjeneste);
        @SuppressWarnings("resource")
        Response response = tjeneste.hentGruppertKodeliste();

        final ObjectMapper om = ObjectMapperFactory.createBaseObjectMapper();
        String rawJson = (String) response.getEntity();
        final Map<String, Object> fetchedKodeverdisMap = om.readValue(rawJson, Map.class);
        final String storedResult = new String(Files.readAllBytes(Paths.get("src/test/resources/KodeverkRestTjenesteTestOldOutput.json")));
        final Map<String, Object> storedKodeverdisMap = om.readValue(storedResult, Map.class);
        assertThat(fetchedKodeverdisMap.values().size()).isEqualTo(storedKodeverdisMap.values().size());
        assertThat(fetchedKodeverdisMap.values().size()).isGreaterThan(0);

        for(Map.Entry<String, Object> fetchedEntry : fetchedKodeverdisMap.entrySet()) {
            try {
                final var fetchedKodeverdis = fetchedEntry.getValue();
                assertThat(fetchedKodeverdis).isNotNull();
                final Object storedKodeverdis = storedKodeverdisMap.get(fetchedEntry.getKey());
                assertThat(storedKodeverdis).isNotNull();
                if(fetchedKodeverdis instanceof List) {
                    final List<Map<String, String>> fetchedKodeverdiList = (List<Map<String, String>>) fetchedKodeverdis;
                    assertThat(storedKodeverdis).isInstanceOf(List.class);
                    final List<Map<String, String>> storedKodeverdiList = (List<Map<String, String>>) storedKodeverdis;
                    //assertThat(fetchedKodeverdiList.size()).isEqualTo(storedKodeverdiList.size());
                    assertThat(fetchedKodeverdiList).containsExactlyInAnyOrderElementsOf(storedKodeverdiList);
                } else {
                    final Map<String, List<Map<String, String>>> fetchedKodeverdiMap = (Map<String, List<Map<String, String>>>) fetchedKodeverdis;
                    assertThat(storedKodeverdis).isInstanceOf(Map.class);
                    final Map<String, List<Map<String, String>>> storedKodeverdiMap = (Map<String, List<Map<String, String>>>) storedKodeverdis;
                    for(Map.Entry<String, List<Map<String, String>>> fetchedSubEntry : fetchedKodeverdiMap.entrySet()) {
                        assertThat(fetchedSubEntry.getValue()).isInstanceOf(List.class);
                        final List<Map<String, String>> storedSubKodeverdiList = storedKodeverdiMap.get(fetchedSubEntry.getKey());
                        assertThat(fetchedSubEntry.getValue().size()).isEqualTo(storedSubKodeverdiList.size());
                        assertThat(fetchedSubEntry.getValue()).containsExactlyInAnyOrderElementsOf(storedSubKodeverdiList);
                    }
                }
            } catch (ClassCastException e) {
                throw new RuntimeException("class cast exception for " + fetchedEntry.getKey(), e);
            }
        }
    }

    private static <K extends Kodeverdi> void checkResponseSet(final SortedSet<KodeverdiSomObjekt<K>> responseSet, final java.util.Set<K> statiskSet) {
        assertThat(responseSet.stream().map(ko -> ko.getMadeFrom()).toList()).containsExactlyInAnyOrderElementsOf(statiskSet);
    }

    @Test
    public void skal_hente_statiske_kodeverdier() {
        final KodeverkRestTjeneste tjeneste = new KodeverkRestTjeneste(behandlendeEnhetTjeneste);
        final AlleKodeverdierSomObjektResponse response = tjeneste.alleKodeverdierSomObjekt();
        final var statiske = StatiskeKodeverdier.alle;
        checkResponseSet(response.aktivitetStatuser(), statiske.aktivitetStatuser());
        checkResponseSet(response.arbeidskategorier(), statiske.arbeidskategorier());
        checkResponseSet(response.arbeidsforholdHandlingTyper(), statiske.arbeidsforholdHandlingTyper());
        checkResponseSet(response.avslagsårsaker(), statiske.avslagsårsaker());

        // Sjekk spesialtilfelle i respons, Avslagsprsaker gruppert pr vilkårtype.
        final VilkårType vilkårtype = VilkårType.ALDERSVILKÅR;
        final var k9Vk3Avslagsårsaker = VilkårType.finnAvslagårsakerGruppertPåVilkårType().get(vilkårtype);
        checkResponseSet(response.avslagårsakerPrVilkårTypeKode().get(vilkårtype.getKode()), k9Vk3Avslagsårsaker);

        // Venteårsak er også litt spesiell
        final List<Venteårsak> got = response.venteårsaker().stream().map(ko -> ko.getMadeFrom()).toList();
        final List<Venteårsak> expected = statiske.venteårsaker().stream().toList();
        assertThat(got).containsExactlyInAnyOrderElementsOf(expected);
    }

}
