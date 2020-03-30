package no.nav.k9.sak.mottak.dokumentmottak;

import java.time.LocalDate;

import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.test.util.fagsak.FagsakBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

public class DokumentmottakTestUtil {

    public static MottattDokument byggMottattDokument(Long fagsakId, String xml, LocalDate mottattDato, String journalpostId) {
        MottattDokument.Builder builder = new MottattDokument.Builder();
        builder.medMottattDato(mottattDato);
        builder.medPayload(xml);
        builder.medFagsakId(fagsakId);
        if (journalpostId != null) {
            builder.medJournalPostId(new JournalpostId(journalpostId));
        }
        return builder.build();
    }

    public static Fagsak byggFagsak(AktørId aktørId, Saksnummer saksnummer, FagsakRepository fagsakRepository) {
        Fagsak fagsak = FagsakBuilder.nyForeldrepengesak()
            .medSaksnummer(saksnummer)
            .medBruker(aktørId).build();
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    public static BehandlingVedtak oppdaterVedtaksresultat(Behandling origBehandling, VedtakResultatType vedtakResultatType) {
        return BehandlingVedtak.builder(origBehandling.getId())
            .medVedtakResultatType(vedtakResultatType)
            .medAnsvarligSaksbehandler("Severin Saksbehandler")
            .build();
    }
}
