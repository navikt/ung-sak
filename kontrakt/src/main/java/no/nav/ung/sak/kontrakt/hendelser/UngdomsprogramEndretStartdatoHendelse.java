package no.nav.ung.sak.kontrakt.hendelser;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.hendelser.HendelseType;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(Hendelse.UNGDOMSPROGRAM_OPPHOER)
public class UngdomsprogramEndretStartdatoHendelse implements Hendelse {

    private static final HendelseType HENDELSETYPE_ENDRET_STARTDATO = HendelseType.UNGDOMSPROGRAM_ENDRET_STARTDATO;

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
