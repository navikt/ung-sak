package no.nav.ung.sak.kontrakt.mottak;

import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;

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

    public Periode getPeriode() {
        return periode;
    }

    @AbacAttributt(value = "aktorId", masker = true)
    public String getAktorId() {
        return aktørId.getId();
    }

    public AktørId getAktørId() {
        return aktørId;
    }

}
