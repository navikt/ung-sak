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

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_BOSTEDVILKÅR_KODE)
public class VurderBostedDto extends BekreftetAksjonspunktDto {

    /**
     * Fakta-avklaringer om brukers bosted per periode.
     * Saksbehandler fyller inn om bruker er bosatt i Trondheim for hvert skjæringstidspunkt.
     */
    @JsonProperty("avklaringer")
    @NotNull
    @Size(min = 1, max = 100)
    private List<@Valid BostedAvklaringPeriodeDto> avklaringer;

    @Valid
    @Size(max = 1000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String brevtekst;

    public VurderBostedDto() {
        //for jackson
    }

    @JsonCreator
    public VurderBostedDto(@JsonProperty("avklaringer") List<BostedAvklaringPeriodeDto> avklaringer,
                           @JsonProperty("brevtekst") String brevtekst,
                           @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.avklaringer = avklaringer;
        this.brevtekst = brevtekst;
    }

    public List<BostedAvklaringPeriodeDto> getAvklaringer() {
        return avklaringer;
    }

    public String getBrevtekst() {
        return brevtekst;
    }
}
