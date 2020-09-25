package no.nav.k9.sak.kontrakt.aksjonspunkt;

import no.nav.k9.sak.kontrakt.medlem.OverstyringMedlemskapsvilkåretDto;
import no.nav.k9.sak.typer.Periode;
import org.junit.Test;

import javax.validation.Validation;

import java.time.LocalDate;

import static org.junit.Assert.assertTrue;

public class OverstyringAksjonspunktDtoTest {

    @Test
    public void begrunnelse_skal_kunne_ha_paragraftegn() {
        var p = new Periode(LocalDate.now(), LocalDate.now());
        var object = new OverstyringMedlemskapsvilkåretDto(p,false, "Overstyrt ihht §123", "LN42");
        var result = Validation.buildDefaultValidatorFactory().getValidator().validate(object);
        assertTrue(result.isEmpty());
    }

}
