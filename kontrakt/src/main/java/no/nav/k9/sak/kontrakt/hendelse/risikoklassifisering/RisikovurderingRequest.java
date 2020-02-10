package no.nav.k9.sak.kontrakt.hendelse.risikoklassifisering;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class RisikovurderingRequest {

    @JsonProperty(value = "soekerAktoerId", required = true)
    @Valid
    @NotNull
    private AktoerId soekerAktoerId;

    @JsonProperty(value = "skjæringstidspunkt", required = true)
    @Valid
    @NotNull
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "opplysningsperiode", required = true)
    @Valid
    @NotNull
    private Opplysningsperiode opplysningsperiode;

    @JsonProperty(value = "behandlingstema", required = true)
    @Valid
    @NotNull
    private String behandlingstema;

    @JsonProperty(value = "annenPart")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AnnenPart annenPart;

    @JsonProperty(value="konsumentId", required = true)
    @Valid
    @NotNull
    private UUID konsumentId;

    public RisikovurderingRequest() {
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public AktoerId getSoekerAktoerId() {
        return soekerAktoerId;
    }

    public Opplysningsperiode getOpplysningsperiode() {
        return opplysningsperiode;
    }

    public String getBehandlingstema() {
        return behandlingstema;
    }

    public AnnenPart getAnnenPart() {
        return annenPart;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getKonsumentId() {
        return konsumentId;
    }

    public static class Builder {
        private RisikovurderingRequest mal;

        public Builder() {
            this.mal = new RisikovurderingRequest();
        }

        public Builder medSoekerAktoerId(AktoerId soekerAktoerId) {
            mal.soekerAktoerId = soekerAktoerId;
            return this;
        }

        public Builder medSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
            mal.skjæringstidspunkt = skjæringstidspunkt;
            return this;
        }

        public Builder medOpplysningsperiode(Opplysningsperiode opplysningsperiode) {
            mal.opplysningsperiode = opplysningsperiode;
            return this;
        }

        public Builder medBehandlingstema(String behandlingstema) {
            mal.behandlingstema = behandlingstema;
            return this;
        }

        public Builder medAnnenPart(AnnenPart annenPart) {
            mal.annenPart = annenPart;
            return this;
        }

        public Builder medKonsumentId(UUID konsumentId) {
            mal.konsumentId = konsumentId;
            return this;
        }

        public RisikovurderingRequest build() {
            verifyStateForBuild();
            return mal;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(mal.soekerAktoerId, "soekerAktoerId");
            Objects.requireNonNull(mal.konsumentId, "konsumentid");
            Objects.requireNonNull(mal.skjæringstidspunkt, "skjæringstidspunkt");
            Objects.requireNonNull(mal.opplysningsperiode, "opplysningsperiode");
            Objects.requireNonNull(mal.behandlingstema, "behandlingstema");
        }

    }

}
