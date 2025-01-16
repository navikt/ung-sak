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
@JsonTypeName(Hendelse.HAR_FÅTT_BARN)
public class HarFåttBarnHendelse implements Hendelse {

    private static final HendelseType HENDELSETYPE_HAR_FÅTT_BARN = HendelseType.PDL_FORELDER_BARN_RELASJON;

    @JsonProperty(value = "hendelseInfo", required = true)
    @NotNull
    @Valid
    private HendelseInfo hendelseInfo;

    @JsonProperty(value = "barnIdent", required = true)
    @NotNull
    @Valid
    private PersonIdent barnIdent;

    private HarFåttBarnHendelse() {
    }

    @JsonCreator
    public HarFåttBarnHendelse(@JsonProperty("hendelseInfo") @Valid @NotNull HendelseInfo hendelseInfo,
                               @JsonProperty("barnIdent") @Valid @NotNull PersonIdent barnIdent) {
        this.hendelseInfo = hendelseInfo;
        this.barnIdent = barnIdent;
    }

    @Override
    public HendelseInfo getHendelseInfo() {
        return this.hendelseInfo;
    }

    @Override
    public HendelseType getHendelseType() {
        return HENDELSETYPE_HAR_FÅTT_BARN;
    }

    @Override
    public Periode getHendelsePeriode() {
        throw new UnsupportedOperationException("Fødselsdato på barnet må hentes fra PDL");
    }

    public PersonIdent getBarnIdent() {
        return barnIdent;
    }

    public static class Builder {
        private HarFåttBarnHendelse mal;

        public Builder() {
            this.mal = new HarFåttBarnHendelse();
        }

        public Builder medHendelseInfo(HendelseInfo hendelseInfo) {
            mal.hendelseInfo = hendelseInfo;
            return this;
        }

        public Builder medBarnIdent(PersonIdent barnIdent) {
            mal.barnIdent = barnIdent;
            return this;
        }

        public HarFåttBarnHendelse build() {
            return mal;
        }
    }

}
