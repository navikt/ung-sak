package no.nav.k9.sak.web.app.tjenester.kodeverk;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.web.app.jackson.ObjectMapperFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class KodeverkRestTjenesteTest {

    @Inject
    private HentKodeverkTjeneste hentKodeverkTjeneste;

    /**
     * Denne test sjekker at gammalt endepunkt for kodeverk returnerer det samme som før etter at det er implementert
     * ved å kalle det nye endepunktet. Test og tilhøyrande json fil kan slettast når det har gått gjennom pipeline ein gang.
     */
    @Test
    public void nytt_endepunkt_returnerer_samme_som_gamle() throws IOException {
        KodeverkRestTjeneste tjeneste = new KodeverkRestTjeneste(hentKodeverkTjeneste);
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void skal_hente_kodeverk_og_gruppere_på_kodeverknavn() throws IOException {

        KodeverkRestTjeneste tjeneste = new KodeverkRestTjeneste(hentKodeverkTjeneste);
        @SuppressWarnings("resource")
        Response response = tjeneste.hentGruppertKodeliste();

        String rawJson = (String) response.getEntity();
        assertThat(rawJson).isNotNull();

        Map<String, Object> gruppertKodeliste = ObjectMapperFactory.createBaseObjectMapper().readValue(rawJson, Map.class);

        assertThat(gruppertKodeliste.keySet())
            .contains(FagsakStatus.class.getSimpleName(), Avslagsårsak.class.getSimpleName(), Landkoder.class.getSimpleName(), Region.class.getSimpleName());

        assertThat(gruppertKodeliste.keySet())
            .containsAll(new HashSet<>(HentKodeverkTjeneste.KODEVERDIER_SOM_BRUKES_PÅ_KLIENT.keySet()));

        assertThat(gruppertKodeliste.keySet()).hasSize(HentKodeverkTjeneste.KODEVERDIER_SOM_BRUKES_PÅ_KLIENT.size());

        var fagsakStatuser = (List<Map<String, String>>) gruppertKodeliste.get(FagsakStatus.class.getSimpleName());
        assertThat(fagsakStatuser.stream().map(k -> k.get("kode")).collect(Collectors.toList())).isNotEmpty();

        var map = (Map<String, List<?>>) gruppertKodeliste.get(Avslagsårsak.class.getSimpleName());
        assertThat(map.keySet()).contains(VilkårType.OPPTJENINGSVILKÅRET.getKode(), VilkårType.MEDLEMSKAPSVILKÅRET.getKode());

        var avslagsårsaker = (List<Map<String, String>>) map.get(VilkårType.OPPTJENINGSVILKÅRET.getKode());
        assertThat(avslagsårsaker.stream().map(k -> ((Map) k).get("kode")).collect(Collectors.toList())).isNotEmpty();
    }

}
