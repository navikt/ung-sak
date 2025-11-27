package no.nav.ung.sak.behandling.prosessering.task;

import no.nav.ung.sak.domene.registerinnhenting.InntektAbonnentTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InntektHendelseTilstandTest {

    @Test
    void skal_returnere_false_når_sekvensnummer_er_null() {
        var inntektHendelseTilstand = new HentInntektHendelserTask.InntektHendelseTilstand(null);

        assertThat(inntektHendelseTilstand.kanHenteHendelser()).isFalse();
    }

    @Test
    void skal_returnere_true_når_sekvensnummer_har_verdi() {
        var inntektHendelseTilstand = new HentInntektHendelserTask.InntektHendelseTilstand(1000L);

        assertThat(inntektHendelseTilstand.kanHenteHendelser()).isTrue();
    }

    @Test
    void skal_returnere_true_når_sekvensnummer_er_0() {
        var inntektHendelseTilstand = new HentInntektHendelserTask.InntektHendelseTilstand(0L);

        assertThat(inntektHendelseTilstand.kanHenteHendelser()).isTrue();
    }

    @Test
    void skal_returnere_høyeste_sekvensnummer_pluss_1_når_liste_har_flere_hendelser() {
        var inntektHendelseTilstand = new HentInntektHendelserTask.InntektHendelseTilstand(1000L);
        var InntektHendelse = List.of(
            new InntektAbonnentTjeneste.InntektHendelse(1001L, new AktørId("123"), periode()),
            new InntektAbonnentTjeneste.InntektHendelse(1002L, new AktørId("456"), periode()),
            new InntektAbonnentTjeneste.InntektHendelse(1003L, new AktørId("789"), periode())
        );

        var nyTilstand = inntektHendelseTilstand.oppdaterTilstand(InntektHendelse);

        assertThat(nyTilstand.fraSekvensnummer()).isEqualTo(1004L);
    }

    @Test
    void skal_returnere_sekvensnummer_pluss_1_når_liste_har_enkelt_element() {
        var inntektHendelseTilstand = new HentInntektHendelserTask.InntektHendelseTilstand(1000L);
        var InntektHendelse = List.of(
            new InntektAbonnentTjeneste.InntektHendelse(1001L, new AktørId("123"), periode())
        );

        var nyTilstand = inntektHendelseTilstand.oppdaterTilstand(InntektHendelse);

        assertThat(nyTilstand.fraSekvensnummer()).isEqualTo(1002L);
    }

    @Test
    void skal_returnere_høyeste_sekvensnummer_pluss_1_når_liste_er_usortert() {
        var inntektHendelseTilstand = new HentInntektHendelserTask.InntektHendelseTilstand(1000L);
        var InntektHendelse = List.of(
            new InntektAbonnentTjeneste.InntektHendelse(1005L, new AktørId("123"), periode()),
            new InntektAbonnentTjeneste.InntektHendelse(1002L, new AktørId("456"), periode()),
            new InntektAbonnentTjeneste.InntektHendelse(1008L, new AktørId("789"), periode()),
            new InntektAbonnentTjeneste.InntektHendelse(1003L, new AktørId("012"), periode())
        );

        var nyTilstand = inntektHendelseTilstand.oppdaterTilstand(InntektHendelse);

        assertThat(nyTilstand.fraSekvensnummer()).isEqualTo(1009L);
    }

    @Test
    void skal_returnere_korrekt_sekvensnummer_når_verdier_er_store() {
        var inntektHendelseTilstand = new HentInntektHendelserTask.InntektHendelseTilstand(999999999L);
        var hendelser = List.of(
            new InntektAbonnentTjeneste.InntektHendelse(1000000000L, new AktørId("123"), periode())
        );

        var nyTilstand = inntektHendelseTilstand.oppdaterTilstand(hendelser);

        assertThat(nyTilstand.fraSekvensnummer()).isEqualTo(1000000001L);
    }

    @Test
    void skal_kaste_exception_når_liste_er_tom() {
        var inntektHendelseTilstand = new HentInntektHendelserTask.InntektHendelseTilstand(1000L);
        List<InntektAbonnentTjeneste.InntektHendelse> tomListe = List.of();

        assertThatThrownBy(() -> inntektHendelseTilstand.oppdaterTilstand(tomListe))
            .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void skal_returnere_ny_tilstand_når_oppdatert() {
        var opprinneligTilstand = new HentInntektHendelserTask.InntektHendelseTilstand(1000L);
        var InntektHendelse = List.of(
            new InntektAbonnentTjeneste.InntektHendelse(1001L, new AktørId("123"), periode())
        );

        var nyTilstand = opprinneligTilstand.oppdaterTilstand(InntektHendelse);

        assertThat(opprinneligTilstand.fraSekvensnummer()).isEqualTo(1000L);
        assertThat(nyTilstand.fraSekvensnummer()).isEqualTo(1002L);
        assertThat(nyTilstand).isNotSameAs(opprinneligTilstand);
    }

    @Test
    void skal_returnere_gyldig_tilstand_når_opprinnelig_sekvensnummer_er_null() {
        var inntektHendelseTilstand = new HentInntektHendelserTask.InntektHendelseTilstand(null);
        var InntektHendelse = List.of(
            new InntektAbonnentTjeneste.InntektHendelse(1L, new AktørId("123"), periode())
        );

        assertThat(inntektHendelseTilstand.kanHenteHendelser()).isFalse();

        var nyTilstand = inntektHendelseTilstand.oppdaterTilstand(InntektHendelse);

        assertThat(nyTilstand.fraSekvensnummer()).isEqualTo(2L);
        assertThat(nyTilstand.kanHenteHendelser()).isTrue();
    }

    @Test
    void skal_være_lik_når_sekvensnummer_er_samme() {
        var tilstand1 = new HentInntektHendelserTask.InntektHendelseTilstand(1000L);
        var tilstand2 = new HentInntektHendelserTask.InntektHendelseTilstand(1000L);
        var tilstand3 = new HentInntektHendelserTask.InntektHendelseTilstand(2000L);

        assertThat(tilstand1)
            .isEqualTo(tilstand2)
            .isNotEqualTo(tilstand3)
            .hasSameHashCodeAs(tilstand2);
    }

    @Test
    void skal_være_lik_når_begge_sekvensnummer_er_null() {
        var tilstand1 = new HentInntektHendelserTask.InntektHendelseTilstand(null);
        var tilstand2 = new HentInntektHendelserTask.InntektHendelseTilstand(null);

        assertThat(tilstand1)
            .isEqualTo(tilstand2)
            .hasSameHashCodeAs(tilstand2);
    }

    private Periode periode() {
        return new Periode(LocalDate.now(), LocalDate.now().plusMonths(1));
    }
}

