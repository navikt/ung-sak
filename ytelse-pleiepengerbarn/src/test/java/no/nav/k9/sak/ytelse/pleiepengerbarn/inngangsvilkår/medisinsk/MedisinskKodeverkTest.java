package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.Pleiegrad;

public class MedisinskKodeverkTest {
    @Test
    public void skal_mappe_alle_pleiegrad_verdier() {
        Arrays.stream(Pleiegrad.values())
            .forEach(value -> assertThat(no.nav.k9.kodeverk.medisinsk.Pleiegrad.fraKode(value.name())).isNotNull());
    }
}
