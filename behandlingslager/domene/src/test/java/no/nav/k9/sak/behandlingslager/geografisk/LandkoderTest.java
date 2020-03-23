package no.nav.k9.sak.behandlingslager.geografisk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import no.nav.k9.kodeverk.geografisk.Landkoder;

public class LandkoderTest {

    @Test
    public void skal_sjekke_for_norge() {
        boolean swe = Landkoder.erNorge("SWE");
        boolean nor = Landkoder.erNorge("NOR");

        assertThat(swe).isFalse();
        assertThat(nor).isTrue();
    }
}
