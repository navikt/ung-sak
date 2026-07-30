package no.nav.ung.domenetjenester.arkiv.journalpostvurderer;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.domenetjenester.arkiv.HentDataFraJoarkTask;
import no.nav.ung.domenetjenester.arkiv.JournalpostInfo;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.typer.JournalpostId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StrukturertJournalpostTest {

    @Test
    void skal_godta_aktivitetspenger_varsel_uttalelse_når_aktivitetspenger_enabled() {
        var strukturertJournalpost = new StrukturertJournalpost(false, true);
        var vurderingsgrunnlag = lagVurderingsgrunnlag(Brevkode.AKTIVITETSPENGER_VARSEL_UTTALELSE.getOffisiellKode());

        assertThat(strukturertJournalpost.skalVurdere(vurderingsgrunnlag)).isTrue();
    }

    @Test
    void skal_ikke_godta_aktivitetspenger_varsel_uttalelse_når_aktivitetspenger_disabled() {
        var strukturertJournalpost = new StrukturertJournalpost(false, false);
        var vurderingsgrunnlag = lagVurderingsgrunnlag(Brevkode.AKTIVITETSPENGER_VARSEL_UTTALELSE.getOffisiellKode());

        assertThat(strukturertJournalpost.skalVurdere(vurderingsgrunnlag)).isFalse();
    }

    @Test
    void skal_godta_ungdomsytelse_varsel_uttalelse_uavhengig_av_aktivitetspenger_flag() {
        var strukturertJournalpostDisabled = new StrukturertJournalpost(false, false);
        var strukturertJournalpostEnabled = new StrukturertJournalpost(false, true);
        var vurderingsgrunnlag = lagVurderingsgrunnlag(Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE.getOffisiellKode());

        assertThat(strukturertJournalpostDisabled.skalVurdere(vurderingsgrunnlag)).isTrue();
        assertThat(strukturertJournalpostEnabled.skalVurdere(vurderingsgrunnlag)).isTrue();
    }

    private static Vurderingsgrunnlag lagVurderingsgrunnlag(String brevkode) {
        var journalpostInfo = new JournalpostInfo();
        journalpostInfo.setBrevkode(brevkode);
        var melding = new MottattMelding(new ProsessTaskData(HentDataFraJoarkTask.class).medSekvens("1"));
        melding.setJournalPostId(new JournalpostId("test-123"));
        return new Vurderingsgrunnlag(melding, journalpostInfo);
    }
}
