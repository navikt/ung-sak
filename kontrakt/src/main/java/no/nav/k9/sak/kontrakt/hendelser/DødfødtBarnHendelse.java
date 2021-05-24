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
@JsonTypeName(Hendelse.DOEDFOEDT_BARN)
public class DødfødtBarnHendelse implements Hendelse {

    @JsonProperty(value = "hendelseInfo", required = true)
    @NotNull
    @Valid
    private HendelseInfo hendelseInfo;

    @JsonProperty(value = "dødfødselsdato", required = true)
    @NotNull
    @Valid
    private LocalDate dødfødselsdato;

    private DødfødtBarnHendelse() {
    }

    @JsonCreator
    public DødfødtBarnHendelse(@JsonProperty("dødfødselsdato") @Valid LocalDate dødfødselsdato) {
        this.dødfødselsdato = dødfødselsdato;
    }

    public static class Builder {
        private DødfødtBarnHendelse mal;

        public Builder() {
            this.mal = new DødfødtBarnHendelse();
        }

        public Builder medHendelseInfo(HendelseInfo hendelseInfo) {
            mal.hendelseInfo = hendelseInfo;
            return this;
        }

        public Builder medDødfødselsdato(LocalDate dødfødselsdato) {
            mal.dødfødselsdato = dødfødselsdato;
            return this;
        }

        public DødfødtBarnHendelse build() {
            return mal;
        }
    }

    @Override
    public HendelseInfo getHendelseInfo() {
        return hendelseInfo;
    }

    @Override
    public HendelseType getHendelseType() {
        return HendelseType.PDL_DØDFØDSEL;
    }

}
