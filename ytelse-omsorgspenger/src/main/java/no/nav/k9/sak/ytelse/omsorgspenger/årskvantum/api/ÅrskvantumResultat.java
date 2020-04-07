package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.kontrakt.uttak.OmsorgspengerUtfall;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.kontrakt.uttak.UttaksperiodeOmsorgspenger;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Comparator;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ÅrskvantumResultat {

    @JsonProperty(value = "behandlingUUID", required = true)
    @Valid
    @NotNull
    private String behandlingUUID;

    @JsonProperty(value = "utfall", required = true)
    @Valid
    @NotNull
    private OmsorgspengerUtfall samletUtfall;

    @JsonProperty(value = "uttaksperioder")
    @Valid
    @Size(max = 1000)
    private List<UttaksperiodeOmsorgspenger> uttaksperioder;

    public String getBehandlingUUID() {
        return behandlingUUID;
    }

    public void setBehandlingUUID(String behandlingUUID) {
        this.behandlingUUID = behandlingUUID;
    }

    public OmsorgspengerUtfall getSamletUtfall() {
        return samletUtfall;
    }

    public void setSamletUtfall(OmsorgspengerUtfall samletUtfall) {
        this.samletUtfall = samletUtfall;
    }

    public List<UttaksperiodeOmsorgspenger> getUttaksperioder() {
        return uttaksperioder;
    }

    public void setUttaksperioder(List<UttaksperiodeOmsorgspenger> uttaksperioder) {
        this.uttaksperioder = uttaksperioder;
    }

    public Periode getMaksPeriode() {
        var fom = uttaksperioder.stream().map(UttaksperiodeOmsorgspenger::getFom).min(Comparator.nullsFirst(Comparator.naturalOrder())).orElse(null);
        var tom = uttaksperioder.stream().map(UttaksperiodeOmsorgspenger::getTom).max(Comparator.nullsLast(Comparator.naturalOrder())).orElse(null);
        return fom != null && tom != null ? new Periode(fom, tom) : null;
    }
}
