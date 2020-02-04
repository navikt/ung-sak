package no.nav.foreldrepenger.behandlingslager.geografisk;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.PersonstatusType;

public class RegionTest {

    @Test
    public void skal_verifisere_kodeverk_som_mottas_fra_regelmotor() {
        assertThat(PersonstatusType.personstatusTyperFortsattBehandling()).contains(PersonstatusType.DØD);
        assertThat(Region.finnHøyestRangertRegion(Collections.singletonList("SWE"))).isEqualTo(Region.NORDEN);
        assertThat(Region.finnRegioner("SWE")).contains(Region.NORDEN);
    }

    @Test
    public void skal_verifisere_region_fra_kodeverk_reølasjon(){
        assertThat(Region.finnRegioner("NOR")).contains(Region.NORDEN);
        assertThat(Region.finnRegioner("DNK")).contains(Region.NORDEN);
        assertThat(Region.finnRegioner("FRA")).contains(Region.EOS);
        assertThat(Region.finnRegioner("HUN")).contains(Region.EOS);
        assertThat(Region.finnRegioner("NZL")).contains(Region.TREDJELANDS_BORGER);
        assertThat(Region.finnRegioner("USA")).contains(Region.TREDJELANDS_BORGER);
    }

    @Test
    public void skal_verifisere_høyest_rangert_region_er_norden(){
        List<String> landkoder = new ArrayList<>();
        landkoder.add("NOR");
        landkoder.add("FRA");
        Region region = Region.finnHøyestRangertRegion(landkoder);
        assertThat(region).isEqualByComparingTo(Region.NORDEN);
    }

    @Test
    public void skal_verifisere_høyest_rangert_region_er_EØS(){
        List<String> landkoder = new ArrayList<>();
        landkoder.add("USA");
        landkoder.add("FRA");
        Region region = Region.finnHøyestRangertRegion(landkoder);
        assertThat(region).isEqualByComparingTo(Region.EOS);
    }

    @Test
    public void skal_verifisere_høyest_rangert_region_er_ikke_norden(){
        List<String> landkoder = new ArrayList<>();
        landkoder.add("FRA");
        Region region = Region.finnHøyestRangertRegion(landkoder);
        assertThat(region).isNotEqualByComparingTo(Region.NORDEN);
    }

    @Test
    public void skal_verifisere_høyest_rangert_region_er_udefinert(){
        List<String> landkoder = new ArrayList<>();
        landkoder.add("USA");
        landkoder.add("CAN");
        Region region = Region.finnHøyestRangertRegion(landkoder);
        assertThat(region).isEqualByComparingTo(Region.TREDJELANDS_BORGER);
    }
}
