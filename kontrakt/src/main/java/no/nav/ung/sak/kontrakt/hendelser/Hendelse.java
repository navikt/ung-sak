package no.nav.ung.sak.kontrakt.hendelser;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.validation.Valid;
import no.nav.ung.kodeverk.hendelser.HendelseType;
import no.nav.ung.sak.typer.Periode;

@Valid
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = Hendelse.FOEDSEL, value = FødselHendelse.class),
    @JsonSubTypes.Type(name = Hendelse.DOEDSFALL, value = DødsfallHendelse.class),
    @JsonSubTypes.Type(name = Hendelse.UNGDOMSPROGRAM_OPPHOER, value = UngdomsprogramOpphørHendelse.class),
})
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public interface Hendelse {

    String FOEDSEL = "FOEDSEL_V1";
    String DOEDSFALL = "DOEDSFALL_V1";
    String UNGDOMSPROGRAM_OPPHOER = "UNGDOMSPROGRAM_OPPHOER";

    HendelseType getHendelseType();

    HendelseInfo getHendelseInfo();

    Periode getHendelsePeriode();

}
