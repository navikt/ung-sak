package no.nav.k9.sak.mottak.dokumentmottak;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.mottak.inntektsmelding.InntektsmeldingParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@Dependent
public class MottatteDokumentTjeneste {

    private Period fristForInnsendingAvDokumentasjon;

    private final InntektsmeldingParser inntektsmeldingParser = new InntektsmeldingParser();

    private MottatteDokumentRepository mottatteDokumentRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private UttakRepository uttakRepository;

    private BehandlingRepository behandlingRepository;

    private BehandlingVedtakRepository behandlingVedtakRepository;

    private ProsessTaskRepository prosessTaskRepository;

    protected MottatteDokumentTjeneste() {
        // for CDI proxy
    }

    /**
     * 
     * @param fristForInnsendingAvDokumentasjon - Frist i uker fom siste vedtaksdato
     */
    @Inject
    public MottatteDokumentTjeneste(@KonfigVerdi(value = "sak.frist.innsending.dok", defaultVerdi = "P6W") Period fristForInnsendingAvDokumentasjon,
                                    MottatteDokumentRepository mottatteDokumentRepository,
                                    VilkårResultatRepository vilkårResultatRepository,
                                    UttakRepository uttakRepository,
                                    ProsessTaskRepository prosessTaskRepository,
                                    BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.fristForInnsendingAvDokumentasjon = fristForInnsendingAvDokumentasjon;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.uttakRepository = uttakRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = behandlingRepositoryProvider.getBehandlingVedtakRepository();
    }

    public void persisterInntektsmeldingForBehandling(Behandling behandling, Collection<MottattDokument> dokumenter) {
        boolean harPayload = dokumenter.stream().anyMatch(d -> d.harPayload());
        if (!harPayload) {
            return; // quick return
        }
        Long behandlingId = behandling.getId();

        var inntektsmeldinger = inntektsmeldingParser.parseInntektsmeldinger(dokumenter);
        for (var dokument : dokumenter) {
            // sendte bare ett dokument her, så forventer kun et svar:
            InntektsmeldingBuilder im = inntektsmeldinger.get(0);
            var arbeidsgiver = im.getArbeidsgiver(); // NOSONAR
            dokument.setArbeidsgiver(arbeidsgiver.getIdentifikator());
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(im.getInnsendingstidspunkt());
            dokument.setKildesystem(im.getKildesystem());
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.MOTTATT);// setter status MOTTATT; oppdatres senere til GYLDIG når er lagret i Abakus
        }

        var journalpostder = dokumenter.stream().map(MottattDokument::getJournalpostId).collect(Collectors.toCollection(LinkedHashSet::new));

        lagreInntektsmeldinger(behandlingId, journalpostder);
    }

    /** Lagrer inntektsmeldinger til abakus fra mottatt dokument. */
    private void lagreInntektsmeldinger(Long behandlingId, Collection<JournalpostId> mottatteDokumenter) {

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        AktørId aktørId = behandling.getAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();

        var enkeltTask = new ProsessTaskData(LagreMottattInntektsmeldingerTask.TASKTYPE);
        enkeltTask.setBehandling(behandling.getFagsakId(), behandlingId, aktørId.getId());
        enkeltTask.setSaksnummer(saksnummer.getVerdi());
        enkeltTask.setCallIdFraEksisterende();
        List<String> journalpostIder = mottatteDokumenter.stream().map(j -> j.getVerdi()).collect(Collectors.toList());
        enkeltTask.setProperty(LagreMottattInntektsmeldingerTask.MOTTATT_DOKUMENT, String.join(",", journalpostIder));
        prosessTaskRepository.lagre(enkeltTask);
    }

    Long lagreMottattDokumentPåFagsak(MottattDokument dokument) {
        MottattDokument mottattDokument = mottatteDokumentRepository.lagre(dokument, DokumentStatus.MOTTATT);
        return mottattDokument.getId();
    }

    Optional<MottattDokument> hentMottattDokument(Long mottattDokumentId) {
        return mottatteDokumentRepository.hentMottattDokument(mottattDokumentId);
    }

    List<MottattDokument> hentMottatteDokumentPåFagsak(long fagsakId, boolean taSkriveLås, DokumentStatus... statuser) {
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId, taSkriveLås, statuser);
    }

    boolean erSisteYtelsesbehandlingAvslåttPgaManglendeDokumentasjon(Fagsak sak) {
        Objects.requireNonNull(sak, "Fagsak");
        Optional<Behandling> behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(sak.getId());
        return behandling.map(this::erAvsluttetPgaManglendeDokumentasjon).orElse(Boolean.FALSE);
    }

    /**
     * Beregnes fra vedtaksdato
     */
    boolean harFristForInnsendingAvDokGåttUt(Fagsak sak) {
        Objects.requireNonNull(sak, "Fagsak");
        Optional<Behandling> behandlingOptional = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(sak.getId());
        return behandlingOptional.flatMap(b -> behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(b.getId()))
            .map(BehandlingVedtak::getVedtaksdato)
            .map(dato -> dato.isBefore(LocalDate.now().minus(fristForInnsendingAvDokumentasjon))).orElse(Boolean.FALSE);
    }

    private boolean erAvsluttetPgaManglendeDokumentasjon(Behandling behandling) {
        Objects.requireNonNull(behandling, "Behandling");
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandling.getId());
        var vilkår = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());
        if (søknadsperioder.isPresent() && vilkår.isPresent()) {
            var v = vilkår.get();
            var maksPeriode = søknadsperioder.get().getMaksPeriode();
            var vt = v.getVilkårTimeline(VilkårType.SØKERSOPPLYSNINGSPLIKT, maksPeriode.getFomDato(), maksPeriode.getTomDato());
            return !vt.filterValue(p -> Objects.equals(p.getAvslagsårsak(), Avslagsårsak.MANGLENDE_DOKUMENTASJON)).isEmpty();
        } else {
            return false;
        }
    }

    void oppdaterStatus(List<MottattDokument> mottatteDokumenter, DokumentStatus nyStatus) {
        mottatteDokumentRepository.oppdaterStatus(mottatteDokumenter, nyStatus);
    }

}
