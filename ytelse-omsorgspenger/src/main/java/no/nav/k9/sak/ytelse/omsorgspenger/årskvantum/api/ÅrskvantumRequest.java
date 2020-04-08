package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.kontrakt.uttak.UttaksperiodeOmsorgspenger;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ÅrskvantumRequest {
    @JsonProperty(value = "behandlingUUID", required = true)
    @Valid
    @NotNull
    private String behandlingUUID;

    @JsonProperty(value = "saksnummer", required = true)
    @Valid
    @NotNull
    private String saksnummer;

    @JsonProperty(value = "aktørId", required = true)
    @Valid
    @NotNull
    private String aktørId;

    @JsonProperty(value = "uttakperioder")
    @Valid
    @Size(max = 1000)
    private List<UttaksperiodeOmsorgspenger> uttaksperioder = new ArrayList<>();

    @JsonProperty(value = "barna")
    @Valid
    @Size(max = 20)
    private List<Barn> barna = new ArrayList<>();



    public List<UttaksperiodeOmsorgspenger> getUttaksperioder() {
        return uttaksperioder;
    }

    public void setUttaksperioder(List<UttaksperiodeOmsorgspenger> uttaksperioder) {
        this.uttaksperioder = uttaksperioder;
    }

    public String getAktørId() {
        return aktørId;
    }

    public void setAktørId(String aktørId) {
        this.aktørId = aktørId;
    }

    public List<Barn> getBarna() {
        return barna;
    }

    public void setBarna(List<Barn> barna) {
        this.barna = barna;
    }

    public String getBehandlingUUID() {
        return behandlingUUID;
    }

    public void setBehandlingUUID(String behandlingUUID) {
        this.behandlingUUID = behandlingUUID;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }
}
