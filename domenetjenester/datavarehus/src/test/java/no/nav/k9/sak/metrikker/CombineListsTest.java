package no.nav.k9.sak.metrikker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class CombineListsTest {

    private static final String AVSLAG_ARSAK = "avslag_arsak";
    private static final String FAGSAK_STATUS = "fagsak_status";
    private static final String YTELSE_TYPE = "ytelse_type";

    @Test
    public void skal_kombinere_angitte_vektorer() throws Exception {

        var res = new CombineLists<>(Map.of(
            YTELSE_TYPE, StatistikkRepository.YTELSER,
            FAGSAK_STATUS, StatistikkRepository.FAGSAK_STATUS,
            AVSLAG_ARSAK, StatistikkRepository.AVSLAGSÅRSAKER)).toMap();

        assertThat(res).isNotEmpty();
        var firstRow = res.get(0);
        assertThat(firstRow).hasSize(3);
        assertThat(firstRow.keySet()).containsOnly(YTELSE_TYPE, FAGSAK_STATUS, AVSLAG_ARSAK);

        for (var yt : StatistikkRepository.YTELSER) {
            assertThat(res).anyMatch(m -> m.get(YTELSE_TYPE).equals(yt));
        }

        for (var av : StatistikkRepository.AVSLAGSÅRSAKER) {
            assertThat(res).anyMatch(m -> m.get(AVSLAG_ARSAK).equals(av));
        }

        for (var av : StatistikkRepository.FAGSAK_STATUS) {
            assertThat(res).anyMatch(m -> m.get(FAGSAK_STATUS).equals(av));
        }
    }
}
