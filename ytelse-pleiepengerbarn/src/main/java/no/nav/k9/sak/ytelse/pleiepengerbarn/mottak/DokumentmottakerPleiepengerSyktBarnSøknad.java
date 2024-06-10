package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

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
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.JsonObjectMapper;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
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
import no.nav.k9.sak.domene.behandling.steg.omsorgenfor.BrukerdialoginnsynTjeneste;
import no.nav.k9.sak.mottak.dokumentmottak.AsyncAbakusLagreOpptjeningTask;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.mottak.dokumentmottak.OppgittOpptjeningMapper;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet;
import no.nav.k9.søknad.felles.type.Journalpost;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarnSøknadValidator;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@DokumentGruppeRef(Brevkode.PLEIEPENGER_BARN_SOKNAD_KODE)
class DokumentmottakerPleiepengerSyktBarnSøknad implements Dokumentmottaker {

    private Logger logger = LoggerFactory.getLogger(DokumentmottakerPleiepengerSyktBarnSøknad.class);
    private SøknadOversetter pleiepengerBarnSoknadOversetter;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SøknadParser søknadParser;
    private SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer;
    private ProsessTaskTjeneste taskTjeneste;
    private OppgittOpptjeningMapper oppgittOpptjeningMapperTjeneste;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private Instance<BrukerdialoginnsynTjeneste> brukerdialoginnsynServicer;
    private DokumentmottakerFelles dokumentMottakerFelles;

    DokumentmottakerPleiepengerSyktBarnSøknad() {
        // for CDI proxy
    }

    @Inject
    DokumentmottakerPleiepengerSyktBarnSøknad(MottatteDokumentRepository mottatteDokumentRepository,
                                              SøknadParser søknadParser,
                                              SøknadOversetter pleiepengerBarnSoknadOversetter,
                                              SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer,
                                              ProsessTaskTjeneste taskTjeneste,
                                              OppgittOpptjeningMapper oppgittOpptjeningMapperTjeneste,
                                              SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                              @Any Instance<BrukerdialoginnsynTjeneste> brukerdialoginnsynServicer,
                                              DokumentmottakerFelles dokumentMottakerFelles) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadParser = søknadParser;
        this.sykdomsDokumentVedleggHåndterer = sykdomsDokumentVedleggHåndterer;
        this.pleiepengerBarnSoknadOversetter = pleiepengerBarnSoknadOversetter;
        this.taskTjeneste = taskTjeneste;
        this.oppgittOpptjeningMapperTjeneste = oppgittOpptjeningMapperTjeneste;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.brukerdialoginnsynServicer = brukerdialoginnsynServicer;
        this.dokumentMottakerFelles = dokumentMottakerFelles;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> dokumenter, Behandling behandling) {
        var behandlingId = behandling.getId();

        var brukerdialoginnsynService = BrukerdialoginnsynTjeneste.finnTjeneste(brukerdialoginnsynServicer, behandling.getFagsakYtelseType());

        var sorterteDokumenter = sorterSøknadsdokumenter(dokumenter);
        for (MottattDokument dokument : sorterteDokumenter) {
            Søknad søknad = søknadParser.parseSøknad(dokument);
            brukerdialoginnsynService.publiserDokumentHendelse(behandling, dokument);
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(søknad.getMottattDato().toLocalDateTime());
            if (søknad.getKildesystem().isPresent()) {
                dokument.setKildesystem(søknad.getKildesystem().get().getKode());
            }
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);
            // Søknadsinnhold som persisteres "lokalt" i k9-sak
            persister(søknad, behandling, dokument.getJournalpostId());
            dokumentMottakerFelles.opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), dokument.getJournalpostId(), dokument.getType());
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
            OpptjeningAktivitet opptjeningAktiviteter = ((PleiepengerSyktBarn) søknad.getYtelse()).getOpptjeningAktivitet();
            var request = oppgittOpptjeningMapperTjeneste.mapRequest(behandling, dokument, opptjeningAktiviteter);
            if (request.map(OppgittOpptjeningMottattRequest::getOppgittOpptjening).isEmpty()) {
                // Ingenting mer som skal lagres - dokument settes som ferdig
                mottatteDokumentRepository.oppdaterStatus(List.of(dokument), DokumentStatus.GYLDIG);
                return;
            }
            var enkeltTask = ProsessTaskData.forProsessTask(AsyncAbakusLagreOpptjeningTask.class);
            var payload = JsonObjectMapper.getMapper().writeValueAsString(request.get());
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
        final List<Periode> tidligereSøknadsperioder = søknadsperiodeTjeneste.utledFullstendigPeriode(behandling.getId())
            .stream()
            .map(d -> new Periode(d.getFomDato(), d.getTomDato()))
            .toList();
        new PleiepengerSyktBarnSøknadValidator().forsikreValidert(søknad, tidligereSøknadsperioder);

        pleiepengerBarnSoknadOversetter.persister(søknad, journalpostId, behandling);

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
                sykdomsDokumentVedleggHåndterer.leggTilDokumenterSomSkalHåndteresVedlagtSkjema(
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
            sykdomsDokumentVedleggHåndterer.leggTilDokumenterSomSkalHåndteresVedlagtSkjema(behandling,
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
