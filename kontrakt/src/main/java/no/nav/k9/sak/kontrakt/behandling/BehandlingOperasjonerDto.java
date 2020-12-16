package no.nav.k9.sak.kontrakt.behandling;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BehandlingOperasjonerDto {

    @JsonProperty(value = "uuid", required = true)
    @Valid
    @NotNull
    private UUID uuid;

    @JsonProperty(value = "behandlingKanBytteEnhet")
    private boolean behandlingKanBytteEnhet;

    @JsonProperty(value = "behandlingKanHenlegges")
    private boolean behandlingKanHenlegges;

    @JsonProperty(value = "behandlingKanGjenopptas")
    private boolean behandlingKanGjenopptas;

    @JsonProperty(value = "behandlingKanOpnesForEndringer")
    private boolean behandlingKanOpnesForEndringer;

    @JsonProperty(value = "behandlingKanSettesPaVent")
    private boolean behandlingKanSettesPaVent;

    @JsonProperty(value = "behandlingKanSendeMelding")
    private boolean behandlingKanSendeMelding;

    @JsonProperty(value = "behandlingFraBeslutter")
    private boolean behandlingFraBeslutter;

    @JsonProperty(value = "behandlingTilGodkjenning")
    private boolean behandlingTilGodkjenning;

    public UUID getUuid() {
        return uuid;
    }

    public boolean isBehandlingKanBytteEnhet() {
        return behandlingKanBytteEnhet;
    }

    public boolean isBehandlingKanHenlegges() {
        return behandlingKanHenlegges;
    }

    public boolean isBehandlingKanGjenopptas() {
        return behandlingKanGjenopptas;
    }

    public boolean isBehandlingKanOpnesForEndringer() {
        return behandlingKanOpnesForEndringer;
    }

    public boolean isBehandlingKanSettesPaVent() {
        return behandlingKanSettesPaVent;
    }

    public boolean isBehandlingKanSendeMelding() {
        return behandlingKanSendeMelding;
    }

    public boolean isBehandlingFraBeslutter() {
        return behandlingFraBeslutter;
    }

    public boolean isBehandlingTilGodkjenning() {
        return behandlingTilGodkjenning;
    }


    public static Builder builder(UUID uuid) {
        return new Builder(uuid);
    }

    public static class Builder {
        private BehandlingOperasjonerDto kladd;

        private Builder(UUID uuid) {
            kladd = new BehandlingOperasjonerDto();
            kladd.uuid = uuid;
        }

        public Builder medKanBytteEnhet(boolean bytteEnhet) {
            this.kladd.behandlingKanBytteEnhet = bytteEnhet;
            return this;
        }

        public Builder medKanHenlegges(boolean henlegges) {
            this.kladd.behandlingKanHenlegges = henlegges;
            return this;
        }

        public Builder medKanGjenopptas(boolean gjenopptas) {
            this.kladd.behandlingKanGjenopptas = gjenopptas;
            return this;
        }

        public Builder medKanSettesPaVent(boolean settVent) {
            this.kladd.behandlingKanSettesPaVent = settVent;
            return this;
        }

        public Builder medKanOpnesForEndringer(boolean opnes) {
            this.kladd.behandlingKanOpnesForEndringer = opnes;
            return this;
        }

        public Builder medKanSendeMelding(boolean sendeMelding) {
            this.kladd.behandlingKanSendeMelding = sendeMelding;
            return this;
        }

        public Builder medFraBeslutter(boolean fraBeslutter) {
            this.kladd.behandlingFraBeslutter = fraBeslutter;
            return this;
        }

        public Builder medTilGodkjenning(boolean tilGodkjenning) {
            this.kladd.behandlingTilGodkjenning = tilGodkjenning;
            return this;
        }

        public BehandlingOperasjonerDto build() {
            return kladd;
        }
    }
}
