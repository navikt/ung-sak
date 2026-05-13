package no.nav.ung.ytelse.ungdomsprogramytelsen.initperioder;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class InitierPerioderStegTest {

    private static final LocalDate STARTDATO = LocalDate.of(2025, 1, 6);

    @Inject
    private EntityManager em;

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private UngdomsytelseStartdatoRepository startdatoRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private InitierPerioderSteg steg;
    private Fagsak fagsak;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(em);
        behandlingVedtakRepository = new BehandlingVedtakRepository(em);
        startdatoRepository = new UngdomsytelseStartdatoRepository(em);
        mottatteDokumentRepository = new MottatteDokumentRepository(em);
        steg = new InitierPerioderSteg(behandlingRepository, startdatoRepository, mottatteDokumentRepository);

        fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), STARTDATO, STARTDATO.plusWeeks(64));
        em.persist(fagsak);
    }

    @Test
    void skal_markere_alle_startdatoer_som_relevante_for_førstegangsbehandling() {
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var journalpostId = new JournalpostId("100");
        lagreMottattDokument(behandling, journalpostId);
        startdatoRepository.lagre(behandling.getId(), List.of(new UngdomsytelseSøktStartdato(STARTDATO, journalpostId)));

        steg.utførSteg(kontekstFor(behandling));

        var resultat = hentRelevanteStartdatoer(behandling);
        assertThat(resultat).containsExactly(STARTDATO);
    }

    @Test
    void skal_filtrere_bort_startdato_som_allerede_er_kjent_fra_forrige_avsluttede_behandling() {
        // Førstegangsbehandling med startdato S1 - avsluttes
        var førstegangsbehandling = lagreFørstegangsbehandlingMedRelevantStartdato(STARTDATO, new JournalpostId("100"));

        // Revurdering med papirsøknad som har samme startdato S1
        var revurdering = Behandling.fraTidligereBehandling(førstegangsbehandling, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));

        var papirsøknadJournalpostId = new JournalpostId("200");
        lagreMottattDokument(revurdering, papirsøknadJournalpostId);
        startdatoRepository.lagre(revurdering.getId(), List.of(new UngdomsytelseSøktStartdato(STARTDATO, papirsøknadJournalpostId)));

        steg.utførSteg(kontekstFor(revurdering));

        var resultat = hentRelevanteStartdatoer(revurdering);
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_kun_markere_nye_startdatoer_som_relevante_ved_revurdering() {
        // Førstegangsbehandling med startdato S1 - avsluttes
        var førstegangsbehandling = lagreFørstegangsbehandlingMedRelevantStartdato(STARTDATO, new JournalpostId("100"));

        // Revurdering med to søknader: en med eksisterende startdato S1 og en med ny startdato S2
        var revurdering = Behandling.fraTidligereBehandling(førstegangsbehandling, BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));

        var nyStartdato = STARTDATO.plusMonths(2);
        var eksisterendeJournalpost = new JournalpostId("200");
        var nyJournalpost = new JournalpostId("201");
        lagreMottattDokument(revurdering, eksisterendeJournalpost);
        lagreMottattDokument(revurdering, nyJournalpost);
        startdatoRepository.lagre(revurdering.getId(), List.of(
            new UngdomsytelseSøktStartdato(STARTDATO, eksisterendeJournalpost),
            new UngdomsytelseSøktStartdato(nyStartdato, nyJournalpost)
        ));

        steg.utførSteg(kontekstFor(revurdering));

        var resultat = hentRelevanteStartdatoer(revurdering);
        assertThat(resultat).containsExactly(nyStartdato);
    }

    private void lagreMottattDokument(Behandling behandling, JournalpostId journalpostId) {
        var dokument = new MottattDokument.Builder()
            .medJournalPostId(journalpostId)
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medType(Brevkode.UNGDOMSYTELSE_SOKNAD)
            .medMottattDato(LocalDate.now())
            .build();
        mottatteDokumentRepository.lagre(dokument, DokumentStatus.GYLDIG);
    }

    private Behandling lagreFørstegangsbehandlingMedRelevantStartdato(LocalDate startdato, JournalpostId journalpostId) {
        var førstegangsbehandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(førstegangsbehandling, behandlingRepository.taSkriveLås(førstegangsbehandling));
        var søktStartdato = new UngdomsytelseSøktStartdato(startdato, journalpostId);
        startdatoRepository.lagre(førstegangsbehandling.getId(), List.of(søktStartdato));
        startdatoRepository.lagreRelevanteSøknader(førstegangsbehandling.getId(),
            new UngdomsytelseStartdatoer(Set.of(søktStartdato)));
        // BehandlingVedtak kreves for at finnSisteAvsluttedeIkkeHenlagteYtelsebehandling skal returnere behandlingen.
        var vedtak = BehandlingVedtak.builder(førstegangsbehandling.getId())
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(1))
            .medAnsvarligSaksbehandler("test")
            .build();
        behandlingVedtakRepository.lagre(vedtak, behandlingRepository.taSkriveLås(førstegangsbehandling));
        førstegangsbehandling.avsluttBehandling();
        behandlingRepository.lagre(førstegangsbehandling, behandlingRepository.taSkriveLås(førstegangsbehandling));
        return førstegangsbehandling;
    }

    private BehandlingskontrollKontekst kontekstFor(Behandling behandling) {
        return new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling));
    }

    private Set<LocalDate> hentRelevanteStartdatoer(Behandling behandling) {
        return startdatoRepository.hentGrunnlag(behandling.getId())
            .map(g -> g.getRelevanteStartdatoer().getStartdatoer())
            .orElseThrow()
            .stream()
            .map(UngdomsytelseSøktStartdato::getStartdato)
            .collect(Collectors.toSet());
    }
}
