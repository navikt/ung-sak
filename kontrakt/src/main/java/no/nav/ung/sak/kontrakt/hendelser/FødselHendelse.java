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
import no.nav.ung.sak.typer.PersonIdent;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(Hendelse.FOEDSEL)
public class FødselHendelse implements Hendelse {

    private static final HendelseType HENDELSETYPE_FØDSEL = HendelseType.PDL_FORELDER_BARN_RELASJON;

    @JsonProperty(value = "hendelseInfo", required = true)
    @NotNull
    @Valid
    private HendelseInfo hendelseInfo;

    @JsonProperty(value = "fødselsdato", required = true)
    @NotNull
    @Valid
    private LocalDate fødselsdato;

    @JsonProperty(value = "barnIdent", required = true)
    @NotNull
    @Valid
    private PersonIdent barnIdent;

    private FødselHendelse() {
    }

    @JsonCreator
    public FødselHendelse(@JsonProperty("hendelseInfo") @Valid @NotNull HendelseInfo hendelseInfo,
                          @JsonProperty("fødselsdato") @Valid @NotNull LocalDate fødselsdato,
                          @JsonProperty("barnIdent") @Valid @NotNull PersonIdent barnIdent) {
        this.hendelseInfo = hendelseInfo;
        this.barnIdent = barnIdent;
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

    public PersonIdent getBarnIdent() {
        return barnIdent;
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

        public Builder medBarnIdent(PersonIdent barnIdent) {
            mal.barnIdent = barnIdent;
            return this;
        }

        public FødselHendelse build() {
            return mal;
        }
    }

}
