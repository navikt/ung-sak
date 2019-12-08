package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class DokumentmottakTestUtil {

    public static BehandlingskontrollTjeneste lagBehandlingskontrollTjenesteMock(BehandlingskontrollServiceProvider serviceProvider) {
        BehandlingskontrollTjeneste behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider, null) {
            @Override
            protected void fireEventBehandlingStegOvergang(BehandlingskontrollKontekst kontekst, Behandling behandling,
                                                           BehandlingStegTilstandSnapshot forrigeTilstand, BehandlingStegTilstandSnapshot nyTilstand) {
                // NOOP
            }

            @Override
            public void prosesserBehandling(BehandlingskontrollKontekst kontekst) {
                // NOOP
            }
        };
        return behandlingskontrollTjeneste;
    }

    static MottattDokument byggMottattDokument(DokumentTypeId dokumentTypeId, Long fagsakId, String xml, LocalDate mottattDato, boolean elektroniskRegistrert,
                                               String journalpostId) {
        MottattDokument.Builder builder = new MottattDokument.Builder();
        builder.medDokumentType(dokumentTypeId);
        builder.medMottattDato(mottattDato);
        builder.medXmlPayload(xml);
        builder.medElektroniskRegistrert(elektroniskRegistrert);
        builder.medFagsakId(fagsakId);
        if (journalpostId != null) {
            builder.medJournalPostId(new JournalpostId(journalpostId));
        }
        return builder.build();
    }

    static MottattDokument byggMottattPapirsøknad(DokumentTypeId dokumentTypeId, Long fagsakId, String xml, LocalDate mottattDato,
                                                  boolean elektroniskRegistrert, String journalpostId) {
        MottattDokument.Builder builder = new MottattDokument.Builder();
        builder.medDokumentType(dokumentTypeId);
        builder.medDokumentKategori(DokumentKategori.SØKNAD);
        builder.medMottattDato(mottattDato);
        builder.medXmlPayload(xml);
        builder.medElektroniskRegistrert(elektroniskRegistrert);
        builder.medFagsakId(fagsakId);
        if (journalpostId != null) {
            builder.medJournalPostId(new JournalpostId(journalpostId));
        }
        return builder.build();
    }

    static Fagsak byggFagsak(AktørId aktørId, Saksnummer saksnummer, FagsakRepository fagsakRepository) {
        NavBruker navBruker = new NavBrukerBuilder()
            .medAktørId(aktørId)
            .build();
        Fagsak fagsak = FagsakBuilder.nyForeldrepengesak()
            .medSaksnummer(saksnummer)
            .medBruker(navBruker).build();
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    public static BehandlingVedtak oppdaterVedtaksresultat(Behandling origBehandling, VedtakResultatType vedtakResultatType) {
        BehandlingVedtak vedtak = BehandlingVedtak.builder()
            .medVedtakResultatType(vedtakResultatType)
            .medVedtakstidspunkt(LocalDateTime.now())
            .medBehandlingsresultat(origBehandling.getBehandlingsresultat())
            .medAnsvarligSaksbehandler("Severin Saksbehandler")
            .build();

        return vedtak;
    }
}
