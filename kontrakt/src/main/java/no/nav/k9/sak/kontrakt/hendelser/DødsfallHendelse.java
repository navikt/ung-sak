package no.nav.k9.sak.kontrakt.hendelser;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.hendelser.HendelseType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(Hendelse.DOEDSFALL)
public class DødsfallHendelse implements Hendelse {

    @JsonProperty(value = "hendelseInfo", required = true)
    @NotNull
    @Valid
    private HendelseInfo hendelseInfo;

    @JsonProperty(value = "dødsdato", required = true)
    @NotNull
    @Valid
    private LocalDate dødsdato;

    private DødsfallHendelse() {
    }

    @JsonCreator
    public DødsfallHendelse(@JsonProperty("dødsdato") @Valid LocalDate dødsdato) {
        this.dødsdato = dødsdato;
    }

    public static class Builder {
        private DødsfallHendelse mal;

        public Builder() {
            this.mal = new DødsfallHendelse();
        }

        public Builder medHendelseInfo(HendelseInfo hendelseInfo) {
            mal.hendelseInfo = hendelseInfo;
            return this;
        }

        public Builder medDødsdato(LocalDate dødsdato) {
            mal.dødsdato = dødsdato;
            return this;
        }

        public DødsfallHendelse build() {
            return mal;
        }
    }

    @Override
    public HendelseInfo getHendelseInfo() {
        return hendelseInfo;
    }

    @Override
    public HendelseType getHendelseType() {
        return HendelseType.PDL_DØDSFALL;
    }

}
