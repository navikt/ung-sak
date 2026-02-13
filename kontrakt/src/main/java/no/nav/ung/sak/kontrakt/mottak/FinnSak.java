package no.nav.ung.sak.kontrakt.mottak;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.abac.AppAbacAttributt;
import no.nav.ung.sak.abac.AppAbacAttributtType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FinnSak {

    @JsonProperty(value = "ytelseType", required = true)
    @NotNull
    private FagsakYtelseType ytelseType;

    @JsonProperty(value = "aktørId", required = true)
    @NotNull
    @Valid
    private AktørId aktørId;

    @JsonProperty(value = "periode", required = false)
    @Valid
    private Periode periode;

    @JsonCreator
    public FinnSak(@JsonProperty(value = "ytelseType", required = true) FagsakYtelseType ytelseType,
                   @JsonProperty(value = "aktørId", required = true) @NotNull AktørId aktørId,
                   @JsonProperty(value = "periode", required = false) Periode periode) {
        this.ytelseType = Objects.requireNonNull(ytelseType, "ytelseType");
        this.aktørId = Objects.requireNonNull(aktørId, "aktørId");
        this.periode = periode;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    @AppAbacAttributt(AppAbacAttributtType.YTELSETYPE)
    public String getYtelseTypeKode() {
        return ytelseType.getKode();
    }

    public Periode getPeriode() {
        return periode;
    }

    @AppAbacAttributt(AppAbacAttributtType.SAKER_MED_AKTØR_ID)
    public String getAktorId() {
        return aktørId.getId();
    }

    public AktørId getAktørId() {
        return aktørId;
    }

}
