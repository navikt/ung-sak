package no.nav.foreldrepenger.inngangsvilkaar.medisinsk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Test;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk.Pleiegrad;

public class MedisinskKodeverkTest {
    @Test
    public void skal_mappe_alle_pleiegrad_verdier() {
        Arrays.stream(Pleiegrad.values())
            .forEach(value -> assertThat(no.nav.k9.kodeverk.medisinsk.Pleiegrad.fraKode(value.name())).isNotNull());
    }
}
