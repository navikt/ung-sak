package no.nav.k9.sak.web.app.tjenester.kodeverk;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import no.nav.k9.sak.web.app.jackson.ObjectMapperFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class KodeverkRestTjenesteTest {

    @Inject
    private HentKodeverkTjeneste hentKodeverkTjeneste;

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

    @Test
    public void serialize_kodeverdi_enums() throws Exception {
        // TODO Dette er ein ganske verdilaus test. Fjern eller erstatt med noko nyttig.
        ObjectMapper om = ObjectMapperFactory.createBaseObjectMapper();

        String json = om.writer().withDefaultPrettyPrinter().writeValueAsString(AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT);

        assertThat(json).isNotEmpty();
    }

}
