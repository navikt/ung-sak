package no.nav.ung.sak.web.app.tjenester.kodeverk;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.web.app.jackson.ObjectMapperFactory;
import no.nav.ung.sak.web.app.tjenester.kodeverk.dto.AlleKodeverdierSomObjektResponse;
import no.nav.ung.sak.web.app.tjenester.kodeverk.dto.KodeverdiSomObjekt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class KodeverkRestTjenesteTest {

    @Inject
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    private static <K extends Kodeverdi> void checkResponseSet(final SortedSet<KodeverdiSomObjekt<K>> responseSet, final java.util.Set<K> statiskSet) {
        assertThat(responseSet.stream().map(ko -> ko.getMadeFrom()).toList()).containsExactlyInAnyOrderElementsOf(statiskSet);
    }

    private static List<LinkedHashMap<String, String>> getKodelisteMap(final Map<String, Object> gruppertKodelisteMap, final String kodelisteNavn) {
        final Object object = gruppertKodelisteMap.get(kodelisteNavn);
        assertThat(object).isNotNull();
        assertThat(object).isInstanceOf(List.class);
        assertThat(((List<?>)object).getFirst()).isInstanceOf(LinkedHashMap.class);
        return (List<LinkedHashMap<String, String>>) object;
    }

    /**
     * Smoke test på at FagsakYtelseType frå gammalt endepunkt gjev samme svar som nytt (minus - kode)
     */
    @Test
    public void hentGruppertKodeliste_fungerer_framleis() throws IOException {
        final KodeverkRestTjeneste tjeneste = new KodeverkRestTjeneste(behandlendeEnhetTjeneste);
        final Response hentGruppertKodelisteResponse = tjeneste.hentGruppertKodeliste();
        final ObjectMapper om = ObjectMapperFactory.createBaseObjectMapper();
        final Map<String, Object> hentGruppertKodelisteObjectMap = om.readValue((String)hentGruppertKodelisteResponse.getEntity(), Map.class);
        final List<LinkedHashMap<String, String>> legacyFagsakYtelseType = getKodelisteMap(hentGruppertKodelisteObjectMap, "FagsakYtelseType");
        final AlleKodeverdierSomObjektResponse alleKodeverdierSomObjektResponse = tjeneste.alleKodeverdierSomObjekt();
        for(final var fagsakYtelseType : alleKodeverdierSomObjektResponse.fagsakYtelseTyper()) {
            if(!fagsakYtelseType.getKode().equals("-")) {
                // Sjekk at den finnast i gammalt barnetilleggTidslinje
                final LinkedHashMap<String, String> funnet = legacyFagsakYtelseType.stream().filter(v -> {
                    final String kode = v.get("kode");
                    return kode.equals(fagsakYtelseType.getKode());
                }).findAny().get();
                assertThat(funnet.get("navn")).isEqualTo(fagsakYtelseType.getNavn());
                assertThat(funnet.get("kodeverk")).isEqualTo(fagsakYtelseType.getKodeverk());
            }
        }

    }

    @Test
    public void skal_hente_statiske_kodeverdier() {
        final KodeverkRestTjeneste tjeneste = new KodeverkRestTjeneste(behandlendeEnhetTjeneste);
        final AlleKodeverdierSomObjektResponse response = tjeneste.alleKodeverdierSomObjekt();
        final var statiske = StatiskeKodeverdier.alle;
        // Sjekk nokon av verdiane. Kan legge til fleire viss ein ønsker.
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
