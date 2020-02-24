package no.nav.k9.sak.kontrakt.produksjonsstyring;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OppgaveIdDto {

    @JsonProperty(value = "verdi")
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    @Size(min = 1, max = 50)
    private String verdi;

    public OppgaveIdDto() {
        //
    }

    @JsonCreator
    public OppgaveIdDto(@JsonProperty(value = "verdi") @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$") @Size(min = 1, max = 50) String verdi) {
        this.verdi = verdi;
    }

    @AbacAttributt("oppgaveId")
    public String getVerdi() {
        return verdi;
    }

    public void setVerdi(String verdi) {
        this.verdi = verdi;
    }
}
