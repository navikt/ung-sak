package no.nav.k9.sak.kontrakt.ungdomsytelse.hendelser;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.kodeverk.hendelser.HendelseType;
import no.nav.k9.sak.kontrakt.hendelser.Hendelse;
import no.nav.k9.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.k9.sak.kontrakt.uttak.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(Hendelse.UNGDOMSPROGRAM_OPPHOER)
public class UngdomsprogramOpphørHendelse implements Hendelse {

    private static final HendelseType HENDELSETYPE_OPPHØR = HendelseType.UNGDOMSPROGRAM_OPPHØR;

    @JsonProperty(value = "hendelseInfo", required = true)
    @NotNull
    @Valid
    private HendelseInfo hendelseInfo;

    @JsonProperty(value = "opphørsdato")
    @NotNull
    @Valid
    private LocalDate opphørsdato;

    private UngdomsprogramOpphørHendelse() {
    }

    @JsonCreator
    public UngdomsprogramOpphørHendelse(@JsonProperty("hendelseInfo") @Valid @NotNull HendelseInfo hendelseInfo,
                                        @JsonProperty("opphørsdato") @Valid LocalDate opphørsdato) {
        this.hendelseInfo = hendelseInfo;
        this.opphørsdato = opphørsdato;
    }

    public static class Builder {
        private UngdomsprogramOpphørHendelse mal;

        public Builder() {
            this.mal = new UngdomsprogramOpphørHendelse();
        }

        public Builder medHendelseInfo(HendelseInfo hendelseInfo) {
            mal.hendelseInfo = hendelseInfo;
            return this;
        }

        public Builder medOpphørsdato(LocalDate opphørsdato) {
            mal.opphørsdato = opphørsdato;
            return this;
        }

        public UngdomsprogramOpphørHendelse build() {
            return mal;
        }
    }

    @Override
    public HendelseInfo getHendelseInfo() {
        return hendelseInfo;
    }

    @Override
    public HendelseType getHendelseType() {
        return HENDELSETYPE_OPPHØR;
    }

    @Override
    public Periode getHendelsePeriode() {
        return new Periode(opphørsdato, opphørsdato);
    }

}
