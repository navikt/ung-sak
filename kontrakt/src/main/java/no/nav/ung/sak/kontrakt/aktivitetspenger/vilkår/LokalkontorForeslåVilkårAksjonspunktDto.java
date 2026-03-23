package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.LOKALKONTOR_FORESLÅR_VILKÅR_KODE)
public class LokalkontorForeslåVilkårAksjonspunktDto extends BekreftetAksjonspunktDto {

    public LokalkontorForeslåVilkårAksjonspunktDto() {
        //
    }

    public LokalkontorForeslåVilkårAksjonspunktDto(String begrunnelse) {
        super(begrunnelse);
    }

}
