package no.nav.k9.sak.kontrakt.hendelser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import no.nav.k9.sak.typer.AktørId;

@Valid
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class HendelseInfo {

    @Valid
    @NotNull
    @JsonProperty(value = "hendelseId", required = true)
    private String hendelseId;

    @JsonProperty(value = "aktørIder")
    @Size(max = 100)
    @Valid
    @NotNull
    private List<AktørId> aktørIder = new ArrayList<>();

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "opprettet")
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
