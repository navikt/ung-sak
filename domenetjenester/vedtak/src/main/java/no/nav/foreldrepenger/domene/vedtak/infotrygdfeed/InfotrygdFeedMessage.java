package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InfotrygdFeedMessage {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }


    @JsonProperty("uuid")
    @NotNull
    private final String uuid;

    @JsonProperty("ytelse")
    @NotNull
    private final String ytelse;

    @JsonProperty("saksnummer")
    @NotNull
    private final String saksnummer;

    @JsonProperty("aktoerId")
    @NotNull
    private final String aktoerId;

    @JsonProperty("foersteStoenadsdag")
    private final LocalDate foersteStoenadsdag;

    @JsonProperty("sisteStoenadsdag")
    private final LocalDate sisteStoenadsdag;

    public static Builder builder() {
        return new Builder();
    }

    @JsonCreator
    private InfotrygdFeedMessage(
                @JsonProperty("uuid") String uuid,
                @JsonProperty("ytelse") String ytelse,
                @JsonProperty("saksnummer") String saksnummer,
                @JsonProperty("aktoerId") String aktoerId,
                @JsonProperty("foersteStoenadsdag") LocalDate foersteStoenadsdag,
                @JsonProperty("sisteStoenadsdag") LocalDate sisteStoenadsdag) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.ytelse = Objects.requireNonNull(ytelse, "ytelse");
        this.saksnummer = Objects.requireNonNull(saksnummer, "saksnummer");
        this.aktoerId = Objects.requireNonNull(aktoerId, "aktoerId");
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

    public String getYtelse() {
        return ytelse;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public String getAktoerId() {
        return aktoerId;
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
            Objects.equals(ytelse, that.ytelse) &&
            Objects.equals(saksnummer, that.saksnummer) &&
            Objects.equals(aktoerId, that.aktoerId) &&
            Objects.equals(foersteStoenadsdag, that.foersteStoenadsdag) &&
            Objects.equals(sisteStoenadsdag, that.sisteStoenadsdag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, ytelse, saksnummer, aktoerId, foersteStoenadsdag, sisteStoenadsdag);
    }

    @Override
    public String toString() {
        return "InfotrygdFeedMessage{" +
            "uuid='" + uuid + '\'' +
            ", ytelse='" + ytelse + '\'' +
            ", saksnummer='" + saksnummer + '\'' +
            ", aktoerId='" + aktoerId + '\'' +
            ", foersteStoenadsdag=" + foersteStoenadsdag +
            ", sisteStoenadsdag=" + sisteStoenadsdag +
            '}';
    }

    public static class Builder {
        private String uuid; // oblig
        private String ytelse; // oblig
        private String saksnummer; // oblig
        private String aktoerId; // oblig

        private LocalDate foersteStoenadsdag;
        private LocalDate sisteStoenadsdag;

        public InfotrygdFeedMessage build() {
            return new InfotrygdFeedMessage(uuid, ytelse, saksnummer, aktoerId, foersteStoenadsdag, sisteStoenadsdag);
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder ytelse(String ytelse) {
            this.ytelse = ytelse;
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
