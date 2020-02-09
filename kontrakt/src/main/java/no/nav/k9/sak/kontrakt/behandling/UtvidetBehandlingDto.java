package no.nav.k9.sak.kontrakt.behandling;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.AsyncPollingStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UtvidetBehandlingDto extends BehandlingDto {

    @JsonProperty("ansvarligBeslutter")
    @Size(max = 100000)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String ansvarligBeslutter;

    @JsonProperty(value = "behandlingHenlagt")
    private boolean behandlingHenlagt;

    /** Eventuelt async status på tasks. */
    @JsonProperty("taskStatus")
    @Valid
    private AsyncPollingStatus taskStatus;

    public String getAnsvarligBeslutter() {
        return ansvarligBeslutter;
    }

    public AsyncPollingStatus getTaskStatus() {
        return taskStatus;
    }

    public boolean isBehandlingHenlagt() {
        return behandlingHenlagt;
    }

    public void setAnsvarligBeslutter(String ansvarligBeslutter) {
        this.ansvarligBeslutter = ansvarligBeslutter;
    }

    public void setBehandlingHenlagt(boolean behandlingHenlagt) {
        this.behandlingHenlagt = behandlingHenlagt;
    }

    public void setAsyncStatus(AsyncPollingStatus asyncStatus) {
        this.taskStatus = asyncStatus;
    }
}
