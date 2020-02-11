package no.nav.k9.sak.kontrakt.medlem;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE_KODE)
public class BekreftErMedlemVurderingDto extends BekreftedePerioderMalDto {

    BekreftErMedlemVurderingDto() { // NOSONAR
        // For Jackson
    }

    public BekreftErMedlemVurderingDto(String begrunnelse, List<BekreftedePerioderDto> bekreftedePerioder) { // NOSONAR
        super(begrunnelse, bekreftedePerioder);
    }

}
