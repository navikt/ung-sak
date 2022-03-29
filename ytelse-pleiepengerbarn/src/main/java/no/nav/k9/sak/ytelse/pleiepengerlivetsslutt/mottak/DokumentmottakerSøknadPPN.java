package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.mottak;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.domene.abakus.AbakusInntektArbeidYtelseTjenesteFeil;
import no.nav.k9.sak.mottak.dokumentmottak.AsyncAbakusLagreOpptjeningTask;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.OppgittOpptjeningMapper;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.mottak.SykdomsDokumentVedleggHåndterer;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet;
import no.nav.k9.søknad.felles.type.Journalpost;
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase;

@ApplicationScoped
@FagsakYtelseTypeRef("PPN")
@DokumentGruppeRef(Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE_KODE)
class DokumentmottakerSøknadPPN implements Dokumentmottaker {

    private static final Logger logger = LoggerFactory.getLogger(DokumentmottakerSøknadPPN.class);

    private SøknadOversetter søknadOversetter;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SøknadParser søknadParser;
    private SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer;
    private ProsessTaskTjeneste taskTjeneste;
    private OppgittOpptjeningMapper oppgittOpptjeningMapperTjeneste;

    DokumentmottakerSøknadPPN() {
        // for CDI proxy
    }

    @Inject
    DokumentmottakerSøknadPPN(MottatteDokumentRepository mottatteDokumentRepository,
                              SøknadParser søknadParser,
                              SøknadOversetter søknadOversetter,
                              SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer,
                              ProsessTaskTjeneste taskTjeneste,
                              OppgittOpptjeningMapper oppgittOpptjeningMapperTjeneste) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadParser = søknadParser;
        this.sykdomsDokumentVedleggHåndterer = sykdomsDokumentVedleggHåndterer;
        this.søknadOversetter = søknadOversetter;
        this.taskTjeneste = taskTjeneste;
        this.oppgittOpptjeningMapperTjeneste = oppgittOpptjeningMapperTjeneste;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> dokumenter, Behandling behandling) {
        var behandlingId = behandling.getId();

        var sorterteDokumenter = sorterSøknadsdokumenter(dokumenter);
        for (MottattDokument dokument : sorterteDokumenter) {
            Søknad søknad = søknadParser.parseSøknad(dokument);
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(søknad.getMottattDato().toLocalDateTime());
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);
            // Søknadsinnhold som persisteres "lokalt" i k9-sak
            persister(søknad, behandling, dokument.getJournalpostId());
            // Søknadsinnhold som persisteres eksternt (abakus)
            lagreOppgittOpptjeningFraSøknad(søknad, behandling, dokument);
        }
    }

    private LinkedHashSet<MottattDokument> sorterSøknadsdokumenter(Collection<MottattDokument> dokumenter) {
        return dokumenter
            .stream()
            .sorted(Comparator.comparing(MottattDokument::getMottattTidspunkt))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Lagrer oppgitt opptjening til abakus fra mottatt dokument.
     */
    private void lagreOppgittOpptjeningFraSøknad(Søknad søknad, Behandling behandling, MottattDokument dokument) {
        try {
            OpptjeningAktivitet opptjeningAktiviteter = ((PleipengerLivetsSluttfase) søknad.getYtelse()).getOpptjeningAktivitet();
            var request = oppgittOpptjeningMapperTjeneste.mapRequest(behandling, dokument, opptjeningAktiviteter);
            if (request.getOppgittOpptjening() == null) {
                // Ingenting mer som skal lagres - dokument settes som ferdig
                mottatteDokumentRepository.oppdaterStatus(List.of(dokument), DokumentStatus.GYLDIG);
                return;
            }
            var enkeltTask = ProsessTaskData.forProsessTask(AsyncAbakusLagreOpptjeningTask.class);
            var payload = IayGrunnlagJsonMapper.getMapper().writeValueAsString(request);
            enkeltTask.setPayload(payload);

            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.JOURNALPOST_ID, dokument.getJournalpostId().getVerdi());
            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.BREVKODER, dokument.getType().getKode());

            enkeltTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getAktørId());
            enkeltTask.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
            enkeltTask.setCallIdFraEksisterende();

            taskTjeneste.lagre(enkeltTask);
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Opprettelse av task for lagring av oppgitt opptjening i abakus feiler.", e).toException();
        }
    }

    private void persister(Søknad søknad, Behandling behandling, JournalpostId journalpostId) {
        søknadOversetter.persister(søknad, journalpostId, behandling);

        for (Journalpost journalpost : søknad.getJournalposter()) {
            boolean journalpostHarInformasjonSomIkkeKanPunsjes = false;
            boolean journalpostHarMedisinskeOpplysninger = true;
            if (journalpost.getInneholderInformasjonSomIkkeKanPunsjes() != null) {
                journalpostHarInformasjonSomIkkeKanPunsjes = journalpost.getInneholderInformasjonSomIkkeKanPunsjes();
            }
            if (journalpost.getInneholderMedisinskeOpplysninger() != null) {
                journalpostHarMedisinskeOpplysninger = journalpost.getInneholderMedisinskeOpplysninger();
            }

            try {
                sykdomsDokumentVedleggHåndterer.leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(
                    behandling,
                    new JournalpostId(journalpost.getJournalpostId()),
                    behandling.getFagsak().getPleietrengendeAktørId(),
                    søknad.getMottattDato().toLocalDateTime(),
                    journalpostHarInformasjonSomIkkeKanPunsjes,
                    journalpostHarMedisinskeOpplysninger);
            } catch (RuntimeException e) {
                logger.warn("Feil ved håndtering av forsendelse " + journalpostId.getVerdi() + " med tilknyttet journalpost " + journalpost.getJournalpostId());
                throw e;
            }
        }

        Optional<Journalpost> journalpost = søknad.getJournalposter()
            .stream()
            .filter(j -> j.getJournalpostId().equals(journalpostId.getVerdi()))
            .findFirst();

        if (journalpost.isEmpty()) {
            sykdomsDokumentVedleggHåndterer.leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(behandling,
                journalpostId,
                behandling.getFagsak().getPleietrengendeAktørId(),
                søknad.getMottattDato().toLocalDateTime(),
                false,
                true);
        }
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode) {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }
}
