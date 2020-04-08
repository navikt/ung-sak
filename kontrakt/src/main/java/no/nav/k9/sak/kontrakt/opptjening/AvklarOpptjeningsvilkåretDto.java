package no.nav.k9.sak.kontrakt.opptjening;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_OPPTJENINGSVILKÅRET_KODE)
public class AvklarOpptjeningsvilkåretDto extends BekreftetAksjonspunktDto {

    @Valid
    @NotNull
    @JsonProperty(value = "opptjeningFom", required = true)
    private LocalDate opptjeningFom;

    @Valid
    @NotNull
    @JsonProperty(value = "opptjeningTom", required = true)
    private LocalDate opptjeningTom;

    @JsonProperty(value = "avslagskode")
    @Size(min = 4, max = 5)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String avslagskode;

    @JsonProperty(value = "erVilkarOk")
    private boolean erVilkarOk;

    public AvklarOpptjeningsvilkåretDto() {
    }

    public AvklarOpptjeningsvilkåretDto(String begrunnelse, @Size(min = 4, max = 4) @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$") String avslagskode,
                                        boolean erVilkarOk) {
        super(begrunnelse);
        this.avslagskode = avslagskode;
        this.erVilkarOk = erVilkarOk;
    }

    public String getAvslagskode() {
        return avslagskode;
    }

    public void setAvslagskode(String avslagskode) {
        this.avslagskode = avslagskode;
    }

    public boolean getErVilkarOk() {
        return erVilkarOk;
    }

    public void setErVilkarOk(boolean erVilkarOk) {
        this.erVilkarOk = erVilkarOk;
    }

    public LocalDate getOpptjeningFom() {
        return opptjeningFom;
    }

    public void setOpptjeningFom(LocalDate opptjeningFom) {
        this.opptjeningFom = opptjeningFom;
    }

    public LocalDate getOpptjeningTom() {
        return opptjeningTom;
    }

    public void setOpptjeningTom(LocalDate opptjeningTom) {
        this.opptjeningTom = opptjeningTom;
    }
}
