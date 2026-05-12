package no.nav.ung.sak.kontrakt.hendelser;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.hendelser.HendelseType;
import no.nav.ung.sak.typer.Periode;

/**
 * @deprecated Bruk {@link UngdomsprogramForlengetPeriodeHendelse} med typenavn {@code UNGDOMSPROGRAM_FORLENGET_PERIODE}.
 *             Denne klassen beholdes for bakoverkompatibel deserialisering av hendelser som bruker det gamle
 *             typenavnet {@code UNGDOMSPROGRAM_UTVIDET_KVOTE}. Fjernes etter at alle konsumenter
 *             har migrert til {@code UNGDOMSPROGRAM_FORLENGET_PERIODE}.
 */
@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(Hendelse.UNGDOMSPROGRAM_UTVIDET_KVOTE)
public class UngdomsprogramUtvidetKvoteHendelse implements Hendelse {

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
        return HendelseType.UNGDOMSPROGRAM_UTVIDET_KVOTE;
    }

}



