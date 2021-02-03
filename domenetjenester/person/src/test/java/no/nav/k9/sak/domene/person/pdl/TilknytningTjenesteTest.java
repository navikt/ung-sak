package no.nav.k9.sak.domene.person.pdl;

import static no.nav.k9.kodeverk.person.Diskresjonskode.KODE6;
import static no.nav.k9.kodeverk.person.Diskresjonskode.KODE7;
import static no.nav.pdl.AdressebeskyttelseGradering.FORTROLIG;
import static no.nav.pdl.AdressebeskyttelseGradering.STRENGT_FORTROLIG;
import static no.nav.pdl.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND;
import static no.nav.pdl.AdressebeskyttelseGradering.UGRADERT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractComparableAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.person.Diskresjonskode;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseGradering;

class TilknytningTjenesteTest {

    @Test
    void dersom_adressebeskyttelse_er_strengt_fortrolig_så_skal_diskresjonskode_være_kode6() {
        assertDiskresjonskodeFor(
            STRENGT_FORTROLIG_UTLAND
        )
            .isEqualTo(KODE6);

        assertDiskresjonskodeFor(
            STRENGT_FORTROLIG
        )
            .isEqualTo(KODE6);
    }

    @Test
    void dersom_adressebeskyttelse_er_fortrolig_så_skal_diskresjonskode_være_kode7() {
        assertDiskresjonskodeFor(
            FORTROLIG
        )
            .isEqualTo(KODE7);
    }

    @Disabled("Inntil vi er helt sikre på hva pdl kan finne på å sende så er implementasjon foreløpig tilsvarende med fp-sak sin")
    @Test
    void dersom_adressebeskyttelse_kommer_med_flere_forskjellige_koder_så_bør_diskresjonskode_være_den_mest_strenge() {
        assertDiskresjonskodeFor(
            UGRADERT,
            FORTROLIG,
            STRENGT_FORTROLIG,
            STRENGT_FORTROLIG_UTLAND
        )
            .isEqualTo(KODE6);
    }

    @Test
    void dersom_adressebeskyttelse_er_ugradert_så_skal_diskresjonskode_være_null_siden_omkringliggende_systemer_ikke_støtter_diskresjonskode_udefinert() {
        assertDiskresjonskodeFor(
            UGRADERT
        )
            .isNull();
    }

    @Test
    void dersom_adressebeskyttelse_er_tom_liste_så_skal_diskresjonskode_være_null_siden_omkringliggende_systemer_ikke_støtter_diskresjonskode_udefinert() {
        assertDiskresjonskodeFor(
        )
            .isNull();
    }

    private AbstractComparableAssert<?, Diskresjonskode> assertDiskresjonskodeFor(AdressebeskyttelseGradering... strengtFortroligUtland) {
        return assertThat(
            TilknytningTjeneste.diskresjonskodeFor(
                adressebeskyttelser(strengtFortroligUtland)
            )
        );
    }

    private Stream<Adressebeskyttelse> adressebeskyttelser(AdressebeskyttelseGradering... adressebeskyttelseGradering) {
        return Arrays.stream(adressebeskyttelseGradering)
            .map(this::enAdressebeskyttelseMed);
    }

    private Adressebeskyttelse enAdressebeskyttelseMed(AdressebeskyttelseGradering gradering) {
        Adressebeskyttelse adressebeskyttelse = new Adressebeskyttelse();
        adressebeskyttelse.setGradering(gradering);
        return adressebeskyttelse;
    }
}
