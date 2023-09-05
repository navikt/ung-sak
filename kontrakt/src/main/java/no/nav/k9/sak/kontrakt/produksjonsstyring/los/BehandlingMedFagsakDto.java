package no.nav.k9.sak.kontrakt.produksjonsstyring.los;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BehandlingMedFagsakDto {

    @JsonProperty(value = "sakstype", required = true)
    @Valid
    @NotNull
    private FagsakYtelseType sakstype;

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "behandlingResultatType")
    @Valid
    private BehandlingResultatType behandlingResultatType;

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @Valid
    @JsonProperty(value = "eldsteDatoMedEndringFraSøker", required = true)
    private LocalDateTime eldsteDatoMedEndringFraSøker;

    public FagsakYtelseType getSakstype() {
        return sakstype;
    }

    public void setSakstype(FagsakYtelseType sakstype) {
        this.sakstype = sakstype;
    }

    public BehandlingResultatType getBehandlingResultatType() {
        return behandlingResultatType;
    }

    public void setBehandlingResultatType(BehandlingResultatType behandlingResultatType) {
        this.behandlingResultatType = behandlingResultatType;
    }

    public LocalDateTime getEldsteDatoMedEndringFraSøker() {
        return eldsteDatoMedEndringFraSøker;
    }

    public void setEldsteDatoMedEndringFraSøker(LocalDateTime eldsteDatoMedEndringFraSøker) {
        this.eldsteDatoMedEndringFraSøker = eldsteDatoMedEndringFraSøker;
    }
}
