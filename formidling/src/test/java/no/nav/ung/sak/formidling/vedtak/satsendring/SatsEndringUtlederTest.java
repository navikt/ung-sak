package no.nav.ung.sak.formidling.vedtak.satsendring;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SatsEndringUtlederTest {

    private static final LocalDate D1 = LocalDate.of(2024, 1, 1);
    private static final LocalDate D2 = LocalDate.of(2024, 2, 1);
    private static final LocalDate D3 = LocalDate.of(2024, 3, 1);

    @Test
    void fødsel_flereNyeFødteBarn_og_dødsfall() {
        var input = List.of(
            new SatsEndringUtlederInput(0, false, 500, 0, D1),
            new SatsEndringUtlederInput(3, false, 500, 100, D2),   // +3 barn: fødselBarn + fikkFlereBarn
            new SatsEndringUtlederInput(2, false, 500, 80, D3)     // -1 barn: dødsfallBarn
        );

        var resultat = new SatsEndringUtleder(input).lagSatsEndringHendelser();
        assertThat(resultat).hasSize(2);

        var fødselsHendelse = resultat.get(0);
        assertThat(fødselsHendelse.fødselBarn()).isTrue();
        assertThat(fødselsHendelse.fikkFlereBarn()).isTrue();
        assertThat(fødselsHendelse.dødsfallBarn()).isFalse();
        assertThat(fødselsHendelse.fom()).isEqualTo(D2);
        assertThat(fødselsHendelse.barnetilleggSats()).isEqualTo(100);

        var dødsfallHendelse = resultat.get(1);
        assertThat(dødsfallHendelse.dødsfallBarn()).isTrue();
        assertThat(dødsfallHendelse.fødselBarn()).isFalse();
        assertThat(dødsfallHendelse.fom()).isEqualTo(D3);
        assertThat(dødsfallHendelse.barnetilleggSats()).isEqualTo(100); // bruker previous ved dødsfall
    }

    @Test
    void overgang_fra_lav_til_høy_sats() {
        var input = List.of(
            new SatsEndringUtlederInput(0, false, 500, 0, D1),
            new SatsEndringUtlederInput(0, true, 700, 0, D2)
        );

        var hendelse = new SatsEndringUtleder(input).lagSatsEndringHendelser().get(0);
        assertThat(hendelse.overgangTilHøySats()).isTrue();
        assertThat(hendelse.fom()).isEqualTo(D2);
        assertThat(hendelse.dagsats()).isEqualTo(700);
    }

    @Test
    void overgang_fra_høy_til_lav_sats_er_ikke_tillatt() {
        var input = List.of(
            new SatsEndringUtlederInput(0, true, 700, 0, D1),
            new SatsEndringUtlederInput(0, false, 500, 0, D2)
        );

        assertThatThrownBy(() -> new SatsEndringUtleder(input).lagSatsEndringHendelser())
            .isInstanceOf(IllegalStateException.class);
    }
}

