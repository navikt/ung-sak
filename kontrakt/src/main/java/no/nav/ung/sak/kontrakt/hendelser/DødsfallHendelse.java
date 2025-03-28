package no.nav.ung.sak.kontrakt.hendelser;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.hendelser.HendelseType;
import no.nav.ung.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(Hendelse.DOEDSFALL)
public class DødsfallHendelse implements Hendelse {

    private static final HendelseType HENDELSETYPE_DØDSFALL = HendelseType.PDL_DØDSFALL;

    @JsonProperty(value = "hendelseInfo", required = true)
    @NotNull
    @Valid
    private HendelseInfo hendelseInfo;

    @JsonProperty(value = "dødsdato")
    @NotNull
    @Valid
    private LocalDate dødsdato;

    private DødsfallHendelse() {
    }

    @JsonCreator
    public DødsfallHendelse(@JsonProperty("hendelseInfo") @Valid @NotNull HendelseInfo hendelseInfo,
                            @JsonProperty("dødsdato") @Valid LocalDate dødsdato) {
        this.hendelseInfo = hendelseInfo;
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
        return HENDELSETYPE_DØDSFALL;
    }

    @Override
    public Periode getHendelsePeriode() {
        return new Periode(dødsdato, dødsdato);
    }

}
