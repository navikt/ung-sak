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

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class RisikovurderingRequest {

    public static class Builder {
        private RisikovurderingRequest mal;

        public Builder() {
            this.mal = new RisikovurderingRequest();
        }

        public RisikovurderingRequest build() {
            verifyStateForBuild();
            return mal;
        }

        public Builder medAnnenPart(AnnenPart annenPart) {
            mal.annenPart = annenPart;
            return this;
        }

        public Builder medYtelseType(FagsakYtelseType ytelseType) {
            mal.ytelseType = ytelseType.getKode();
            return this;
        }

        public Builder medKonsumentId(UUID konsumentId) {
            mal.konsumentId = konsumentId;
            return this;
        }

        public Builder medOpplysningsperiode(Opplysningsperiode opplysningsperiode) {
            mal.opplysningsperiode = opplysningsperiode;
            return this;
        }

        public Builder medSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
            mal.skjæringstidspunkt = skjæringstidspunkt;
            return this;
        }

        public Builder medSoekerAktoerId(AktoerId soekerAktoerId) {
            mal.soekerAktoerId = soekerAktoerId;
            return this;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(mal.soekerAktoerId, "soekerAktoerId");
            Objects.requireNonNull(mal.konsumentId, "konsumentid");
            Objects.requireNonNull(mal.skjæringstidspunkt, "skjæringstidspunkt");
            Objects.requireNonNull(mal.opplysningsperiode, "opplysningsperiode");
            Objects.requireNonNull(mal.ytelseType, "ytelseType");
        }

    }

    @JsonProperty(value = "annenPart")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AnnenPart annenPart;

    @JsonProperty(value = "ytelseType")
    @Valid
    @NotNull
    private String ytelseType;

    @JsonProperty(value = "konsumentId", required = true)
    @Valid
    @NotNull
    private UUID konsumentId;

    @JsonProperty(value = "opplysningsperiode", required = true)
    @Valid
    @NotNull
    private Opplysningsperiode opplysningsperiode;

    @JsonProperty(value = "skjæringstidspunkt", required = true)
    @Valid
    @NotNull
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "soekerAktoerId", required = true)
    @Valid
    @NotNull
    private AktoerId soekerAktoerId;

    public RisikovurderingRequest() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public AnnenPart getAnnenPart() {
        return annenPart;
    }

    public String getYtelseType() {
        return ytelseType;
    }

    public UUID getKonsumentId() {
        return konsumentId;
    }

    public Opplysningsperiode getOpplysningsperiode() {
        return opplysningsperiode;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public AktoerId getSoekerAktoerId() {
        return soekerAktoerId;
    }

    public void setAnnenPart(AnnenPart annenPart) {
        this.annenPart = annenPart;
    }

    public void setBehandlingstema(String behandlingstema) {
        this.ytelseType = behandlingstema;
    }

    public void setKonsumentId(UUID konsumentId) {
        this.konsumentId = konsumentId;
    }

    public void setOpplysningsperiode(Opplysningsperiode opplysningsperiode) {
        this.opplysningsperiode = opplysningsperiode;
    }

    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

    public void setSoekerAktoerId(AktoerId soekerAktoerId) {
        this.soekerAktoerId = soekerAktoerId;
    }

}
