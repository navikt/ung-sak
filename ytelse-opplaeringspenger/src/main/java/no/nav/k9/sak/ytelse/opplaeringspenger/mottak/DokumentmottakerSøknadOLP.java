package no.nav.k9.sak.ytelse.opplaeringspenger.mottak;

import java.io.IOException;
import java.time.LocalDateTime;
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
import no.nav.abakus.iaygrunnlag.JsonObjectMapper;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.domene.abakus.AbakusInntektArbeidYtelseTjenesteFeil;
import no.nav.k9.sak.mottak.dokumentmottak.AsyncAbakusLagreOpptjeningTask;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.mottak.dokumentmottak.OppgittOpptjeningMapper;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.mottak.SykdomsDokumentVedleggHåndterer;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet;
import no.nav.k9.søknad.felles.type.Journalpost;
import no.nav.k9.søknad.ytelse.olp.v1.Opplæringspenger;
import no.nav.k9.søknad.ytelse.olp.v1.OpplæringspengerSøknadValidator;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
@DokumentGruppeRef(Brevkode.OPPLÆRINGSPENGER_SOKNAD_KODE)
class DokumentmottakerSøknadOLP implements Dokumentmottaker {

    private Logger logger = LoggerFactory.getLogger(DokumentmottakerSøknadOLP.class);
    private SøknadOversetter søknadOversetter;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SøknadParser søknadParser;
    private SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer;
    private OpplæringDokumentVedleggHåndterer opplæringDokumentVedleggHåndterer;
    private ProsessTaskRepository prosessTaskRepository;
    private OppgittOpptjeningMapper oppgittOpptjeningMapperTjeneste;
    private DokumentmottakerFelles dokumentmottakerFelles;

    DokumentmottakerSøknadOLP() {
        // for CDI proxy
    }

    @Inject
    DokumentmottakerSøknadOLP(MottatteDokumentRepository mottatteDokumentRepository,
                              SøknadParser søknadParser,
                              SøknadOversetter søknadOversetter,
                              SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer,
                              OpplæringDokumentVedleggHåndterer opplæringDokumentVedleggHåndterer,
                              ProsessTaskRepository prosessTaskRepository,
                              OppgittOpptjeningMapper oppgittOpptjeningMapperTjeneste,
                              DokumentmottakerFelles dokumentmottakerFelles) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadParser = søknadParser;
        this.sykdomsDokumentVedleggHåndterer = sykdomsDokumentVedleggHåndterer;
        this.opplæringDokumentVedleggHåndterer = opplæringDokumentVedleggHåndterer;
        this.søknadOversetter = søknadOversetter;
        this.prosessTaskRepository = prosessTaskRepository;
        this.oppgittOpptjeningMapperTjeneste = oppgittOpptjeningMapperTjeneste;
        this.dokumentmottakerFelles = dokumentmottakerFelles;
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
            dokumentmottakerFelles.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), dokument.getJournalpostId(), dokument.getType());
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
            OpptjeningAktivitet opptjeningAktiviteter = ((Opplæringspenger) søknad.getYtelse()).getOpptjeningAktivitet();
            var request = oppgittOpptjeningMapperTjeneste.mapRequest(behandling, dokument, opptjeningAktiviteter);
            if (request.map(OppgittOpptjeningMottattRequest::getOppgittOpptjening).isEmpty()) {
                // Ingenting mer som skal lagres - dokument settes som ferdig
                mottatteDokumentRepository.oppdaterStatus(List.of(dokument), DokumentStatus.GYLDIG);
                return;
            }
            var enkeltTask = ProsessTaskData.forProsessTask(AsyncAbakusLagreOpptjeningTask.class);
            var payload = JsonObjectMapper.getMapper().writeValueAsString(request);
            enkeltTask.setPayload(payload);

            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.JOURNALPOST_ID, dokument.getJournalpostId().getVerdi());
            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.BREVKODER, dokument.getType().getKode());

            enkeltTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getAktørId());
            enkeltTask.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
            enkeltTask.setCallIdFraEksisterende();

            prosessTaskRepository.lagre(enkeltTask);
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Opprettelse av task for lagring av oppgitt opptjening i abakus feiler.", e).toException();
        }
    }

    private void persister(Søknad søknad, Behandling behandling, JournalpostId journalpostId) {
        new OpplæringspengerSøknadValidator().forsikreValidert(søknad);
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
                håndterVedlegg(behandling, new JournalpostId(journalpost.getJournalpostId()), søknad.getMottattDato().toLocalDateTime(),
                    journalpostHarInformasjonSomIkkeKanPunsjes, journalpostHarMedisinskeOpplysninger);
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
            håndterVedlegg(behandling, journalpostId, søknad.getMottattDato().toLocalDateTime(), false, true);
        }
    }

    private void håndterVedlegg(Behandling behandling, JournalpostId journalpostId, LocalDateTime mottattidspunkt,
                                boolean journalpostHarInformasjonSomIkkeKanPunsjes, boolean journalpostHarMedisinskeOpplysninger) {
        sykdomsDokumentVedleggHåndterer.leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(behandling, journalpostId,
            behandling.getFagsak().getPleietrengendeAktørId(),
            mottattidspunkt, journalpostHarInformasjonSomIkkeKanPunsjes, journalpostHarMedisinskeOpplysninger);
        opplæringDokumentVedleggHåndterer.leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(behandling, journalpostId, mottattidspunkt);
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode) {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }
}
