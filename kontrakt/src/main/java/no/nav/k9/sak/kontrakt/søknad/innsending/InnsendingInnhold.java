package no.nav.k9.sak.kontrakt.søknad.innsending;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.PlainYtelseDeserializer;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.PlainYtelseSerializer;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "ytelseType", defaultImpl = Void.class)
public abstract class InnsendingInnhold {

    /** bruker custom serializer her da default wrapper ytelsetype i et objekt. */
    @JsonDeserialize(using = PlainYtelseDeserializer.class)
    @JsonSerialize(using = PlainYtelseSerializer.class)
    @JsonProperty(value = "ytelseType", required = true)
    @NotNull
    @Valid
    private FagsakYtelseType ytelseType;

    public InnsendingInnhold(FagsakYtelseType ytelseType) {
        this.ytelseType = Objects.requireNonNull(ytelseType, "ytelseType");
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

}
