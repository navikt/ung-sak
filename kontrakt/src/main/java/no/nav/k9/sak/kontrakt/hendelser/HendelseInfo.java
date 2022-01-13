package no.nav.k9.sak.kontrakt.hendelser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class HendelseInfo {

    @JsonProperty(value = "hendelseId")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String hendelseId;

    @JsonProperty(value = "aktørIder")
    @Size(max = 100)
    @Valid
    @NotNull
    private List<AktørId> aktørIder = new ArrayList<>();

    @JsonProperty(value = "opprettet", required = true)
    @NotNull
    private LocalDateTime opprettet;

    public LocalDateTime getOpprettet() {
        return opprettet;
    }

    public List<AktørId> getAktørIder() {
        return aktørIder;
    }

    public String getHendelseId() {
        return hendelseId;
    }

    public static class Builder {
        private HendelseInfo mal;

        public Builder() {
            this.mal = new HendelseInfo();
        }

        public Builder medHendelseId(String hendelseId) {
            mal.hendelseId = hendelseId;
            return this;
        }

        public Builder leggTilAktør(AktørId aktørId) {
            mal.aktørIder.add(aktørId);
            return this;
        }

        public Builder medOpprettet(LocalDateTime opprettet) {
            mal.opprettet = opprettet;
            return this;
        }

        public HendelseInfo build() {
            return mal;
        }
    }

}
