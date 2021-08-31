package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;

class OpptjeningsaktiviteterPerYtelseTest {

    private OpptjeningsaktiviteterPerYtelse tjeneste = new OpptjeningsaktiviteterPerYtelse(Set.of());

    @Test
    void skal_få_med_frilans_hvis_oppgitt_næring_men_ikke_frilans() {
        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilEgneNæringer(List.of(OppgittOpptjeningBuilder.EgenNæringBuilder.ny().medVirksomhet("000000000").medVirksomhetType(VirksomhetType.FISKE)));
        var oppgittOpptjening = oppgitt.build();

        var relevantAktivitet = tjeneste.erRelevantAktivitet(OpptjeningAktivitetType.FRILANS);

        assertThat(oppgittOpptjening.getFrilans()).isEmpty();
        assertThat(relevantAktivitet).isTrue();
    }

    @Test
    void skal_få_med_frilans_hvis_oppgitt_frilans() {
        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilFrilansOpplysninger(oppgitt.getFrilansBuilder().build());
        var oppgittOpptjening = oppgitt.build();

        var relevantAktivitet = tjeneste.erRelevantAktivitet(OpptjeningAktivitetType.FRILANS);

        assertThat(oppgittOpptjening.getFrilans()).isPresent();
        assertThat(relevantAktivitet).isTrue();
    }

    @Test
    void skal_få_med_frilans_hvis_ingen_oppgitt_opptjening() {
        var relevantAktivitet = tjeneste.erRelevantAktivitet(OpptjeningAktivitetType.FRILANS);
        assertThat(relevantAktivitet).isTrue();
    }
}
