package no.nav.k9.sak.web.app.tjenester.forvaltning;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MigrerOpptjeningbegrunnelserRequest {

    @JsonProperty("periode")
    @NotNull
    @Valid
    private Periode periode;

    @JsonProperty(value = "dryrun")
    @NotNull
    @Valid
    private boolean dryrun;

    protected MigrerOpptjeningbegrunnelserRequest() {
    }

    public MigrerOpptjeningbegrunnelserRequest(Periode periode, boolean dryrun) {
        this.periode = periode;
        this.dryrun = dryrun;
    }

    public Periode getPeriode() {
        return periode;
    }

    public boolean dryrun() {
        return dryrun;
    }
}
