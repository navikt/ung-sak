package no.nav.k9.sak.kontrakt.uttak;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class UttaksplanOmsorgspenger {

    @JsonProperty(value = "saksnummer", required = true)
    @Valid
    @NotNull
    private String saksnummer;

    @JsonProperty(value = "behandlingId", required = true)
    @Valid
    @NotNull
    private String behandlingId;

    @JsonProperty(value = "innsendingstidspunkt", required = true)
    @Valid
    @NotNull
    private LocalDateTime innsendingstidspunkt;


    @JsonProperty(value = "aktiviteter", required = true)
    @Valid
    @NotNull
    private List<UttaksPlanOmsorgspengerAktivitet> aktiviteter;


    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(String behandlingId) {
        this.behandlingId = behandlingId;
    }

    public LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    public void setInnsendingstidspunkt(LocalDateTime innsendingstidspunkt) {
        this.innsendingstidspunkt = innsendingstidspunkt;
    }

    public List<UttaksPlanOmsorgspengerAktivitet> getAktiviteter() {
        return aktiviteter;
    }

    public void setAktiviteter(List<UttaksPlanOmsorgspengerAktivitet> aktiviteter) {
        this.aktiviteter = aktiviteter;
    }
}
