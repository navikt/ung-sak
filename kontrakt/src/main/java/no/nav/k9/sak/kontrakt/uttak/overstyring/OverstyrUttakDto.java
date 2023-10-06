package no.nav.k9.sak.kontrakt.uttak.overstyring;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_UTTAK_KODE)
public class OverstyrUttakDto extends OverstyringAksjonspunktDto {


    @JsonProperty(value = "lagreEllerOppdater")
    @Valid
    @NotNull
    @Size(max = 100)
    private List<OverstyrUttakPeriodeDto> lagreEllerOppdater;

    @JsonProperty(value = "slett")
    @Valid
    @NotNull
    @Size(max = 100)
    private List<OverstyrUttakSlettPeriodeDto> slett;

    @JsonProperty(value = "gåVidere")
    @Valid
    @NotNull
    private boolean gåVidere;

    public OverstyrUttakDto() {
        //
    }

    public OverstyrUttakDto(String begrunnelse, List<OverstyrUttakPeriodeDto> lagreEllerOppdater, List<OverstyrUttakSlettPeriodeDto> slett, boolean gåVidere) {
        super(begrunnelse);
        this.lagreEllerOppdater = lagreEllerOppdater;
        this.slett = slett;
        this.gåVidere = gåVidere;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkarOk() {
        return true;
    }

    public List<OverstyrUttakPeriodeDto> getLagreEllerOppdater() {
        return lagreEllerOppdater;
    }

    public List<OverstyrUttakSlettPeriodeDto> getSlett() {
        return slett;
    }

    public boolean gåVidere() {
        return gåVidere;
    }

    @AssertFalse(message = "Har ingen slett- og/eller lagreEllerOppdater-operasjon")
    public boolean getHarIngenOperasjon() {
        return !gåVidere && lagreEllerOppdater.isEmpty() && slett.isEmpty();
    }

    @AssertTrue(message = "Det er duplikat id i slett- og/eller lagreEllerOppdater-feltene")
    public boolean getHarIngenDuplikatId() {
        List<Long> alleIder = new ArrayList<>();
        for (OverstyrUttakSlettPeriodeDto dto : slett) {
            alleIder.add(dto.getId());
        }
        for (OverstyrUttakPeriodeDto periode : lagreEllerOppdater) {
            if (periode.getId() != null) {
                alleIder.add(periode.getId());
            }
        }
        Set<Long> unike = new HashSet<>(alleIder);
        return alleIder.size() == unike.size();
    }

    @AssertFalse(message = "Det er overlappende perioder i lagreEllerOppdater-feltet")
    public boolean getHarOverlappendePerioder() {
        List<Periode> sortertePerioder = new ArrayList<>(lagreEllerOppdater.stream()
            .map(OverstyrUttakPeriodeDto::getPeriode)
            .sorted(Comparator.naturalOrder())
            .toList());

        for (int i = 1; i < sortertePerioder.size(); i++) {
            if (sortertePerioder.get(i - 1).overlaps(sortertePerioder.get(i))) {
                return true;
            }
        }
        return false;
    }
}
