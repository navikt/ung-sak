package no.nav.ung.sak.kontrakt.hendelser;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.hendelser.HendelseType;
import no.nav.ung.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(Hendelse.UNGDOMSPROGRAM_FORLENGET_PERIODE)
public class UngdomsprogramForlengetPeriodeHendelse implements Hendelse {

    private static final HendelseType HENDELSETYPE_FORLENGET_PERIODE = HendelseType.UNGDOMSPROGRAM_FORLENGET_PERIODE;

    @JsonProperty(value = "hendelseInfo", required = true)
    @NotNull
    @Valid
    private HendelseInfo hendelseInfo;

    @JsonProperty(value = "periode")
    @NotNull
    @Valid
    private Periode periode;

    private UngdomsprogramForlengetPeriodeHendelse() {
    }

    @JsonCreator
    public UngdomsprogramForlengetPeriodeHendelse(@JsonProperty("hendelseInfo") @Valid @NotNull HendelseInfo hendelseInfo,
                                              @JsonProperty("periode") @Valid @NotNull Periode periode) {
        this.hendelseInfo = hendelseInfo;
        this.periode = periode;
    }

    @Override
    public HendelseInfo getHendelseInfo() {
        return hendelseInfo;
    }

    @Override
    public Periode getHendelsePeriode() {
        return this.periode;
    }

    @Override
    public HendelseType getHendelseType() {
        return HENDELSETYPE_FORLENGET_PERIODE;
    }

}
