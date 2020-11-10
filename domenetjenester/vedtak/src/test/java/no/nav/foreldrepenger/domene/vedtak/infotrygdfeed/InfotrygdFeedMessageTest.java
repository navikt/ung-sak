package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class InfotrygdFeedMessageTest {
    private final String json = "{\"uuid\":\"uuid\",\"ytelse\":\"ytelse\",\"saksnummer\":\"saksnummer\",\"aktoerId\":\"aktørId\",\"aktoerIdPleietrengende\":\"aktørIdPleietrengende\",\"foersteStoenadsdag\":\"2020-01-01\",\"sisteStoenadsdag\":\"2020-01-01\"}";
    private final LocalDate date = LocalDate.of(2020, 1, 1);

    @Test
    public void toJson() {
        InfotrygdFeedMessage msg = InfotrygdFeedMessage.builder()
            .uuid("uuid")
            .ytelse("ytelse")
            .aktoerId("aktørId")
            .aktoerIdPleietrengende("aktørIdPleietrengende")
            .saksnummer("saksnummer")
            .foersteStoenadsdag(date)
            .sisteStoenadsdag(date)
            .build();

        assertThat(msg.toJson()).isEqualTo(json);
    }

    @Test
    public void fromJson() {
        InfotrygdFeedMessage msg = InfotrygdFeedMessage.fromJson(json);
        assertThat(msg.getUuid()).isEqualTo("uuid");
        assertThat(msg.getYtelse()).isEqualTo("ytelse");
        assertThat(msg.getAktoerId()).isEqualTo("aktørId");
        assertThat(msg.getAktoerIdPleietrengende()).isEqualTo("aktørIdPleietrengende");
        assertThat(msg.getSaksnummer()).isEqualTo("saksnummer");
        assertThat(msg.getFoersteStoenadsdag()).isEqualTo(date);
        assertThat(msg.getSisteStoenadsdag()).isEqualTo(date);
    }
}
