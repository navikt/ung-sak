package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VARSEL_REVURDERING_MANUELL_KODE)
public class VarselRevurderingManuellDto extends VarselRevurderingDto {

    protected VarselRevurderingManuellDto() {
        //
    }

    public VarselRevurderingManuellDto(String begrunnelse,
                                       boolean sendVarsel,
                                       String fritekst,
                                       LocalDate frist,
                                       Venteårsak ventearsak) {
        super(begrunnelse, sendVarsel, fritekst, frist, ventearsak);
    }

}
