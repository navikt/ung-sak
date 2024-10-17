package no.nav.k9.sak.kontrakt.uttak.søskensaker;

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
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakPeriodeDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakSlettPeriodeDto;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_OVERLAPPENDE_SØSKENSAK_KODE)
public class VurderSøskensakerDto extends BekreftetAksjonspunktDto {


    @JsonProperty(value = "lagreEllerOppdater")
    @Valid
    @NotNull
    @Size(max = 100)
    private List<OverstyrUttakPeriodeDto> lagreEllerOppdater;


    public VurderSøskensakerDto() {
        //
    }

    public VurderSøskensakerDto(String begrunnelse, List<OverstyrUttakPeriodeDto> lagreEllerOppdater) {
        super(begrunnelse);
        this.lagreEllerOppdater = lagreEllerOppdater;
    }

    public List<OverstyrUttakPeriodeDto> getLagreEllerOppdater() {
        return lagreEllerOppdater;
    }

    @AssertTrue(message = "Det er duplikat id i slett- og/eller lagreEllerOppdater-feltene")
    public boolean getHarIngenDuplikatId() {
        List<Long> alleIder = new ArrayList<>();
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
