package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.time.Duration;
import java.time.LocalDateTime;

import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.kodeverk.vilkår.Utfall;

public record OppgittFraværVerdi(
    LocalDateTime innsendingstidspunkt,
    Duration fraværPerDag,
    FraværÅrsak fraværÅrsak,
    SøknadÅrsak søknadÅrsak,
    Utfall søknadsfristUtfall) {

    public boolean erTrektPeriode() {
        return Duration.ZERO.equals(fraværPerDag);
    }
}
