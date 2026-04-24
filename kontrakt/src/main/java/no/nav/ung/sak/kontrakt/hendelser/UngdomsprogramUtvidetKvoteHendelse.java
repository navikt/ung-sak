package no.nav.ung.sak.kontrakt.hendelser;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.hendelser.HendelseType;
import no.nav.ung.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(Hendelse.UNGDOMSPROGRAM_UTVIDET_KVOTE)
public class UngdomsprogramUtvidetKvoteHendelse implements Hendelse {

    private static final HendelseType HENDELSETYPE_UTVIDET_KVOTE = HendelseType.UNGDOMSPROGRAM_UTVIDET_KVOTE;

    @JsonProperty(value = "hendelseInfo", required = true)
    @NotNull
    @Valid
    private HendelseInfo hendelseInfo;

    @JsonProperty(value = "periode")
    @NotNull
    @Valid
    private Periode periode;

    private UngdomsprogramUtvidetKvoteHendelse() {
    }

    @JsonCreator
    public UngdomsprogramUtvidetKvoteHendelse(@JsonProperty("hendelseInfo") @Valid @NotNull HendelseInfo hendelseInfo,
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
        return HENDELSETYPE_UTVIDET_KVOTE;
    }

}
