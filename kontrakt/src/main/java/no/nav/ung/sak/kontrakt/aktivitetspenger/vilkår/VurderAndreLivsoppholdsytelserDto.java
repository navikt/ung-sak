package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.validering.InputValideringRegex;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.kontrakt.vilkår.VilkårPeriodeVurderingDto;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ANDRE_LIVSOPPHOLDSYTELSER_KODE)
public class VurderAndreLivsoppholdsytelserDto extends BekreftetAksjonspunktDto {

    @JsonProperty("vurdertePerioder")
    @NotNull
    @Size(min = 0, max = 100)
    private List<@Valid VilkårPeriodeVurderingDto> vurdertePerioder;

    @Valid
    @Size(max = 1000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String brevtekst;

    public VurderAndreLivsoppholdsytelserDto() {
        //for jackson
    }

    @JsonCreator
    public VurderAndreLivsoppholdsytelserDto(
        @JsonProperty("vurdertePerioder") List<VilkårPeriodeVurderingDto> vurderinger,
        @JsonProperty("brevtekst") String brevtekst,
        @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.vurdertePerioder = vurderinger;
        this.brevtekst = brevtekst;
    }

    public List<VilkårPeriodeVurderingDto> getVurdertePerioder() {
        return vurdertePerioder;
    }

    public String getBrevtekst() {
        return brevtekst;
    }
}
