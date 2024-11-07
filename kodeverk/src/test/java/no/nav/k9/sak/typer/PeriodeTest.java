package no.nav.k9.sak.typer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PeriodeTest {

    @Test
    void test_periode_overlapper() throws Exception {

        assertThat(new Periode("../2020-10-10").overlaps(new Periode("2020-10-10/.."))).isTrue();
        assertThat(new Periode("2020-01-01/2020-10-10").overlaps(new Periode("2020-10-10/.."))).isTrue();
        assertThat(new Periode("../2020-10-10").overlaps(new Periode("2020-09-10/2020-10-10"))).isTrue();
        assertThat(new Periode("2020-01-01/..").overlaps(new Periode("2020-10-10/.."))).isTrue();
        assertThat(new Periode("2020-01-01/2020-01-01").overlaps(new Periode("2020-01-01/2020-01-01"))).isTrue();
    }

    @Test
    void test_overlapper_ikke() throws Exception {
        assertThat(new Periode("../2020-10-10").overlaps(new Periode("2020-10-11/.."))).isFalse();
        assertThat(new Periode("2020-01-01/2020-10-10").overlaps(new Periode("2020-10-11/.."))).isFalse();
        assertThat(new Periode("../2020-10-10").overlaps(new Periode("2020-10-11/.."))).isFalse();
        assertThat(new Periode("2020-10-01/..").overlaps(new Periode("../2020-09-10"))).isFalse();
        assertThat(new Periode("2020-01-01/2020-01-01").overlaps(new Periode("2020-01-02/2020-01-02"))).isFalse();
    }
}
