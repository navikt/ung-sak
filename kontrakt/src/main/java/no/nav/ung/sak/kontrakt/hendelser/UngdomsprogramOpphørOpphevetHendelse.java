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

/**
 * Hendelse som opphever en tidligere sendt {@link UngdomsprogramOpphørHendelse}, f.eks. når opphørsdato/sluttdato
 * har blitt satt feil og bruker skal fortsette i ungdomsprogrammet (f.eks. medhold i klage på opphør).
 * <p>
 * Sluttdatoen som tidligere ble satt for opphøret fjernes, slik at deltakelsen igjen blir løpende/åpen.
 * Maksdato for programperioden er uendret av denne hendelsen, siden den kun forholder seg til startdato.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeName(Hendelse.UNGDOMSPROGRAM_OPPHOER_OPPHEVET)
public class UngdomsprogramOpphørOpphevetHendelse implements Hendelse {

    private static final HendelseType HENDELSETYPE_OPPHØR_OPPHEVET = HendelseType.UNGDOMSPROGRAM_OPPHØR_OPPHEVET;

    @JsonProperty(value = "hendelseInfo", required = true)
    @NotNull
    @Valid
    private HendelseInfo hendelseInfo;

    @JsonProperty(value = "tidligereOpphørsdato")
    @NotNull
    @Valid
    private LocalDate tidligereOpphørsdato;

    private UngdomsprogramOpphørOpphevetHendelse() {
    }

    @JsonCreator
    public UngdomsprogramOpphørOpphevetHendelse(@JsonProperty("hendelseInfo") @Valid @NotNull HendelseInfo hendelseInfo,
                                                @JsonProperty("tidligereOpphørsdato") @Valid LocalDate tidligereOpphørsdato) {
        this.hendelseInfo = hendelseInfo;
        this.tidligereOpphørsdato = tidligereOpphørsdato;
    }

    @Override
    public HendelseInfo getHendelseInfo() {
        return hendelseInfo;
    }

    @Override
    public HendelseType getHendelseType() {
        return HENDELSETYPE_OPPHØR_OPPHEVET;
    }

    /**
     * Datoen som tidligere var satt som opphørsdato/sluttdato, og som nå oppheves.
     */
    public LocalDate getTidligereOpphørsdato() {
        return tidligereOpphørsdato;
    }

    @Override
    public Periode getHendelsePeriode() {
        return new Periode(tidligereOpphørsdato, tidligereOpphørsdato);
    }

}
