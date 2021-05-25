package no.nav.k9.sak.kontrakt.hendelser;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import no.nav.k9.kodeverk.hendelser.HendelseType;

@Valid
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
        @JsonSubTypes.Type(name = Hendelse.DOEDFOEDT_BARN, value = DødfødtBarnHendelse.class),
        @JsonSubTypes.Type(name = Hendelse.FOEDSEL, value = FødselHendelse.class),
        @JsonSubTypes.Type(name = Hendelse.DOEDSFALL, value = DødsfallHendelse.class),
})
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public interface Hendelse {

    String DOEDFOEDT_BARN = "DOEDFOEDT_BARN_V1";
    String FOEDSEL = "FOEDSEL_V1";
    String DOEDSFALL = "DOEDSFALL_V1";


    HendelseType getHendelseType();

    HendelseInfo getHendelseInfo();

}
