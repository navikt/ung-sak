package no.nav.ung.sak.kontrakt.hendelser;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.hendelser.HendelseType;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;

@JsonTypeName(Hendelse.UNGDOMSPROGRAM_ENDRET_STARTDATO)
public class UngdomsprogramEndretStartdatoHendelse implements Hendelse {

    @JsonProperty(value = "hendelseInfo", required = true)
    @NotNull
    @Valid
    private HendelseInfo hendelseInfo;

    @JsonProperty(value = "startdato")
    @NotNull
    @Valid
    private LocalDate startdato;

    private UngdomsprogramEndretStartdatoHendelse() {
    }

    @JsonCreator
    public UngdomsprogramEndretStartdatoHendelse(@JsonProperty("hendelseInfo") @Valid @NotNull HendelseInfo hendelseInfo,
                                                 @JsonProperty("startdato") @Valid LocalDate startdato) {
        this.hendelseInfo = hendelseInfo;
        this.startdato = startdato;
    }

    @Override
    public HendelseInfo getHendelseInfo() {
        return hendelseInfo;
    }

    @Override
    public HendelseType getHendelseType() {
        return HendelseType.UNGDOMSPROGRAM_ENDRET_STARTDATO;
    }

    @Override
    public Periode getHendelsePeriode() {
        return new Periode(startdato, startdato);
    }

}
