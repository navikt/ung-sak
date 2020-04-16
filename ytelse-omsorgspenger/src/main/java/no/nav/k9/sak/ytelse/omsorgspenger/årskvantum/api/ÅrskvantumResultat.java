package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.kontrakt.uttak.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Comparator;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ÅrskvantumResultat {

    @JsonProperty(value = "behandlingUUID", required = true)
    @Valid
    @NotNull
    private String behandlingUUID;

    @JsonProperty(value = "årskvantum")
    @Valid
    @NotNull
    private Årskvantum årskvantum;

    @JsonProperty(value = "uttaksplan")
    @Valid
    @NotNull
    private UttaksplanOmsorgspenger uttaksplan;

    public String getBehandlingUUID() {
        return behandlingUUID;
    }

    public void setBehandlingUUID(String behandlingUUID) {
        this.behandlingUUID = behandlingUUID;
    }

    public Årskvantum getÅrskvantum() {
        return årskvantum;
    }

    public void setÅrskvantum(Årskvantum årskvantum) {
        this.årskvantum = årskvantum;
    }

    public UttaksplanOmsorgspenger getUttaksplan() {
        return uttaksplan;
    }

    public void setUttaksplan(UttaksplanOmsorgspenger uttaksplan) {
        this.uttaksplan = uttaksplan;
    }
    public Periode getMaksPeriode() {
        var fom = uttaksplan.getAktiviteter().stream().flatMap(aktivitet -> aktivitet.getUttaksperioder().stream()).map(UttaksperiodeOmsorgspenger::getFom).min(Comparator.nullsFirst(Comparator.naturalOrder())).orElse(null);
        var tom = uttaksplan.getAktiviteter().stream().flatMap(aktivitet -> aktivitet.getUttaksperioder().stream()).map(UttaksperiodeOmsorgspenger::getFom).max(Comparator.nullsLast(Comparator.naturalOrder())).orElse(null);
        return fom != null && tom != null ? new Periode(fom, tom) : null;
    }

    public OmsorgspengerUtfall hentSamletUtfall() {
        for(UttaksPlanOmsorgspengerAktivitet uttaksPlanOmsorgspengerAktivitet : getUttaksplan().getAktiviteter()) {
            for (UttaksperiodeOmsorgspenger uttaksperiodeOmsorgspenger : uttaksPlanOmsorgspengerAktivitet.getUttaksperioder()) {
                if (OmsorgspengerUtfall.INNVILGET.equals(uttaksperiodeOmsorgspenger.getUtfall())) {
                    return OmsorgspengerUtfall.INNVILGET;
                }
            }
        }

        return OmsorgspengerUtfall.AVSLÅTT;
    }
}
