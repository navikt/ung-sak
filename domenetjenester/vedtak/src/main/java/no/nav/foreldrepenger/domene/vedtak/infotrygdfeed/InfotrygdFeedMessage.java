package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

/*
data class VedtakMessage(
        val uuid: String,
        val saksnummer: String,
        val aktoerId: String,
        val aktoerIdPleietrengende: String?,
        val foersteStoenadsdag: LocalDate?,
        val sisteStoenadsdag: LocalDate?
)
 */


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.util.Objects;

@JsonDeserialize(builder = InfotrygdFeedMessage.Builder.class)
public class InfotrygdFeedMessage {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }


    private final String uuid; // oblig
    private final String saksnummer; // oblig
    private final String aktoerId; // oblig

    private final String aktoerIdPleietrengende;
    private final LocalDate foersteStoenadsdag;
    private final LocalDate sisteStoenadsdag;

    public static Builder builder() {
        return new Builder();
    }

    private InfotrygdFeedMessage(String uuid, String saksnummer, String aktoerId, String aktoerIdPleietrengende, LocalDate foersteStoenadsdag, LocalDate sisteStoenadsdag) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.saksnummer = Objects.requireNonNull(saksnummer, "saksnummer");
        this.aktoerId = Objects.requireNonNull(aktoerId, "aktoerId");
        this.aktoerIdPleietrengende = aktoerIdPleietrengende;
        this.foersteStoenadsdag = foersteStoenadsdag;
        this.sisteStoenadsdag = sisteStoenadsdag;
    }

    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static InfotrygdFeedMessage fromJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, InfotrygdFeedMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getUuid() {
        return uuid;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public String getAktoerId() {
        return aktoerId;
    }

    public String getAktoerIdPleietrengende() {
        return aktoerIdPleietrengende;
    }

    public LocalDate getFoersteStoenadsdag() {
        return foersteStoenadsdag;
    }

    public LocalDate getSisteStoenadsdag() {
        return sisteStoenadsdag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InfotrygdFeedMessage that = (InfotrygdFeedMessage) o;
        return Objects.equals(uuid, that.uuid) &&
            Objects.equals(saksnummer, that.saksnummer) &&
            Objects.equals(aktoerId, that.aktoerId) &&
            Objects.equals(aktoerIdPleietrengende, that.aktoerIdPleietrengende) &&
            Objects.equals(foersteStoenadsdag, that.foersteStoenadsdag) &&
            Objects.equals(sisteStoenadsdag, that.sisteStoenadsdag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, saksnummer, aktoerId, aktoerIdPleietrengende, foersteStoenadsdag, sisteStoenadsdag);
    }

    @Override
    public String toString() {
        return "InfotrygdFeedMessage{" +
            "uuid='" + uuid + '\'' +
            ", saksnummer='" + saksnummer + '\'' +
            ", aktoerId='" + aktoerId + '\'' +
            ", aktoerIdPleietrengende='" + aktoerIdPleietrengende + '\'' +
            ", foersteStoenadsdag=" + foersteStoenadsdag +
            ", sisteStoenadsdag=" + sisteStoenadsdag +
            '}';
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String uuid; // oblig
        private String saksnummer; // oblig
        private String aktoerId; // oblig

        private String aktoerIdPleietrengende;
        private LocalDate foersteStoenadsdag;
        private LocalDate sisteStoenadsdag;

        public InfotrygdFeedMessage build() {
            return new InfotrygdFeedMessage(uuid, saksnummer, aktoerId, aktoerIdPleietrengende, foersteStoenadsdag, sisteStoenadsdag);
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder saksnummer(String saksnummer) {
            this.saksnummer = saksnummer;
            return this;
        }

        public Builder aktoerId(String aktoerId) {
            this.aktoerId = aktoerId;
            return this;
        }

        public Builder aktoerIdPleietrengende(String aktoerIdPleietrengende) {
            this.aktoerIdPleietrengende = aktoerIdPleietrengende;
            return this;
        }

        public Builder foersteStoenadsdag(LocalDate foersteStoenadsdag) {
            this.foersteStoenadsdag = foersteStoenadsdag;
            return this;
        }

        public Builder sisteStoenadsdag(LocalDate sisteStoenadsdag) {
            this.sisteStoenadsdag = sisteStoenadsdag;
            return this;
        }
    }
}
