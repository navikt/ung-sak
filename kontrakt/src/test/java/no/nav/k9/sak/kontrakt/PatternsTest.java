package no.nav.k9.sak.kontrakt;


import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

public class PatternsTest {

    @Test
    public void fritekstfelter_skal_tillate_paragraftegn() {
        String tekst = "Beskrivelse med paragrafreferanse §123";
        Assert.assertTrue(Pattern.matches(Patterns.FRITEKST, tekst));
    }

}
