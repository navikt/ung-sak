package no.nav.ung.sak.kontrakt.formidling.informasjonsbrev;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "informasjonsbrevMalType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = GenereltFritekstBrevDto.class, name = "GENERELT_FRITEKSTBREV")
})
public sealed interface InformasjonsbrevInnholdDto permits GenereltFritekstBrevDto {
}
