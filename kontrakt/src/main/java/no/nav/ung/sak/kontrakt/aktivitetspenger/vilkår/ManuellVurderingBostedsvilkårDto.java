package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import java.util.List;

/**
 * DTO for aksjonspunkt 5144 – manuell vurdering av bostedsvilkåret der saksbehandler har valgt årsak ANNET.
 * Saksbehandler oppgir en fritekstvurdering for IKKE_OPPFYLT-delen av perioden.
 */
@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_BOSTEDSVILKÅR_KODE)
public class ManuellVurderingBostedsvilkårDto extends BekreftetAksjonspunktDto {

    @JsonProperty("perioder")
    @NotNull
    @Size(min = 1, max = 100)
    private List<@Valid ManuellBostedPeriodeDto> perioder;

    public ManuellVurderingBostedsvilkårDto() {
        // for Jackson
    }

    @JsonCreator
    public ManuellVurderingBostedsvilkårDto(@JsonProperty("perioder") List<ManuellBostedPeriodeDto> perioder,
                                             @JsonProperty("begrunnelse") String begrunnelse) {
        super(begrunnelse);
        this.perioder = perioder;
    }

    public List<ManuellBostedPeriodeDto> getPerioder() {
        return perioder;
    }
}
