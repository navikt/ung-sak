package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.typer.JournalpostId;

class PSBBeregningEndringPåForlengelsePeriodeVurdererTest {

    private PSBBeregningEndringPåForlengelsePeriodeVurderer vurderer = new PSBBeregningEndringPåForlengelsePeriodeVurderer();

    @Test
    void skal_vurdere_set_som_like_hvis_inneholder_de_samme() {
        var imSetForrige = Set.of(new JournalpostId("1"), new JournalpostId("12"), new JournalpostId("123"), new JournalpostId("1234"));
        var imSetNå = Set.of(new JournalpostId("1"), new JournalpostId("12"), new JournalpostId("123"), new JournalpostId("1234"));

        var resultat = vurderer.harEndretSeg(imSetForrige, imSetNå);

        assertThat(resultat).isFalse();
    }

    @Test
    void skal_vurdere_set_som_like_hvis_inneholder_de_samme_i_forskjellig_rekkefølge() {
        var imSetForrige = Set.of(new JournalpostId("1"), new JournalpostId("12"), new JournalpostId("123"), new JournalpostId("1234"));
        var imSetNå = Set.of(new JournalpostId("123"), new JournalpostId("1234"), new JournalpostId("1"), new JournalpostId("12"));

        var resultat = vurderer.harEndretSeg(imSetForrige, imSetNå);

        assertThat(resultat).isFalse();
    }

    @Test
    void skal_vurdere_set_som_ulike_hvis_forskjellig_størrelse() {
        var imSetForrige = Set.of(new JournalpostId("1"));
        var imSetNå = Set.of(new JournalpostId("1"), new JournalpostId("12"));

        var resultat = vurderer.harEndretSeg(imSetForrige, imSetNå);

        assertThat(resultat).isTrue();
    }

    @Test
    void skal_vurdere_set_som_ulike_hvis_lik_størrelse_forskjellig_innhold() {
        var imSetForrige = Set.of(new JournalpostId("1"));
        var imSetNå = Set.of(new JournalpostId("12"));

        var resultat = vurderer.harEndretSeg(imSetForrige, imSetNå);

        assertThat(resultat).isTrue();
    }
}
