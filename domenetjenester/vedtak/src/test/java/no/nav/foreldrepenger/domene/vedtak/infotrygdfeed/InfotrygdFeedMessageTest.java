package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import org.junit.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class InfotrygdFeedMessageTest {

    @Test
    public void toJson() {
        LocalDate date = LocalDate.of(2020, 1, 1);

        InfotrygdFeedMessage msg = InfotrygdFeedMessage.builder()
            .uuid("uuid")
            .aktoerId("aktørId")
            .aktoerIdPleietrengende("aktørIdPleietrengende")
            .saksnummer("saksnummer")
            .foersteStoenadsdag(date)
            .sisteStoenadsdag(date)
            .build();

        assertThat(msg.toJson()).isEqualTo(
            "{\"uuid\":\"uuid\",\"saksnummer\":\"saksnummer\",\"aktoerId\":\"aktørId\",\"aktoerIdPleietrengende\":\"aktørIdPleietrengende\",\"foersteStoenadsdag\":\"2020-01-01\",\"sisteStoenadsdag\":\"2020-01-01\"}");
    }
}
