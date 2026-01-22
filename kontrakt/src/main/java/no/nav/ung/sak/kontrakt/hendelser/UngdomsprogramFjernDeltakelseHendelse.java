package no.nav.ung.sak.kontrakt.hendelser;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.hendelser.HendelseType;
import no.nav.ung.sak.felles.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(Hendelse.UNGDOMSPROGRAM_FJERN_PERIODE)
public class UngdomsprogramFjernDeltakelseHendelse implements Hendelse {

    private static final HendelseType HENDELSETYPE_FJERN = HendelseType.UNGDOMSPROGRAM_FJERN_PERIODE;

    @JsonProperty(value = "hendelseInfo", required = true)
    @NotNull
    @Valid
    private HendelseInfo hendelseInfo;

    @JsonProperty(value = "periode")
    @NotNull
    @Valid
    private Periode periode;

    private UngdomsprogramFjernDeltakelseHendelse() {
    }

    @JsonCreator
    public UngdomsprogramFjernDeltakelseHendelse(@JsonProperty("hendelseInfo") @Valid @NotNull HendelseInfo hendelseInfo,
                                                 @JsonProperty("periode") @Valid Periode periode) {
        this.hendelseInfo = hendelseInfo;
        this.periode = periode;
    }

    @Override
    public HendelseInfo getHendelseInfo() {
        return hendelseInfo;
    }

    @Override
    public HendelseType getHendelseType() {
        return HENDELSETYPE_FJERN;
    }

    @Override
    public Periode getHendelsePeriode() {
        return periode;
    }

}
