package no.nav.k9.sak.domene.person.pdl;

import static java.util.stream.Stream.of;
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
    void adressebeskyttelseGraderingSkalIkkeUnderNoenOmstendigheterOversettesTilUdefinert_sidenOmkringliggendeSystemerIkkeStøtterDiskresjonskodenUdefinert() {
        of(AdressebeskyttelseGradering.values())
            .forEach(adressebeskyttelseGradering ->
                assertDiskresjonskodeFor(adressebeskyttelseGradering)
                    .isNotEqualTo(Diskresjonskode.UDEFINERT));
    }

    @Test
    void dersomIngenAdressebeskyttelseErAngitt_såSkalDiskresjonskodeVæreNull_sidenOmkringliggendeSystemerIkkeStøtterDiskresjonskodenUdefinert() {
        assertDiskresjonskodeFor(
            ingenAdressebeskyttelseAngitt()
        )
            .isNull();
    }

    @Test
    void dersomAdressebeskyttelseErStrengtFortrolig_såSkalDiskresjonskodeVæreKode6() {
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
    void dersomAdressebeskyttelseErFortrolig_såSkalDiskresjonskodeVæreKode7() {
        assertDiskresjonskodeFor(
            FORTROLIG
        )
            .isEqualTo(KODE7);
    }

    @Disabled("Inntil vi er helt sikre på hva pdl kan finne på å sende så er implementasjon foreløpig tilsvarende med fp-sak sin")
    @Test
    void dersomAdressebeskyttelseKommerMedFlereForskjelligeKoder_såBørDiskresjonskodeVæreDenMestStrenge() {
        assertDiskresjonskodeFor(
            UGRADERT,
            FORTROLIG,
            STRENGT_FORTROLIG,
            STRENGT_FORTROLIG_UTLAND
        )
            .isEqualTo(KODE6);
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

    private AdressebeskyttelseGradering[] ingenAdressebeskyttelseAngitt() {
        //noinspection SuspiciousToArrayCall
        return of().toArray(AdressebeskyttelseGradering[]::new);
    }
}
