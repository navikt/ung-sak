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
