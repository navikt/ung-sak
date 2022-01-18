package no.nav.k9.sak.kontrakt.hendelser;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.hendelser.HendelseType;
import no.nav.k9.sak.kontrakt.uttak.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(Hendelse.FOEDSEL)
public class FødselHendelse implements Hendelse {

    private static final HendelseType HENDELSETYPE_FØDSEL = HendelseType.PDL_FØDSEL;

    @JsonProperty(value = "hendelseInfo", required = true)
    @NotNull
    @Valid
    private HendelseInfo hendelseInfo;

    @JsonProperty(value = "fødselsdato", required = true)
    @NotNull
    @Valid
    private LocalDate fødselsdato;

    private FødselHendelse() {
    }

    @JsonCreator
    public FødselHendelse(@JsonProperty("hendelseInfo") @Valid @NotNull HendelseInfo hendelseInfo,
                          @JsonProperty("fødselsdato") @Valid LocalDate fødselsdato) {
        this.hendelseInfo = hendelseInfo;
        this.fødselsdato = fødselsdato;
    }

    @Override
    public HendelseInfo getHendelseInfo() {
        return this.hendelseInfo;
    }

    @Override
    public HendelseType getHendelseType() {
        return HENDELSETYPE_FØDSEL;
    }

    @Override
    public Periode getHendelsePeriode() {
        return new Periode(fødselsdato, fødselsdato);
    }

    public static class Builder {
        private FødselHendelse mal;

        public Builder() {
            this.mal = new FødselHendelse();
        }

        public Builder medHendelseInfo(HendelseInfo hendelseInfo) {
            mal.hendelseInfo = hendelseInfo;
            return this;
        }

        public Builder medFødselsdato(LocalDate fødselsdato) {
            mal.fødselsdato = fødselsdato;
            return this;
        }

        public FødselHendelse build() {
            return mal;
        }
    }

}
