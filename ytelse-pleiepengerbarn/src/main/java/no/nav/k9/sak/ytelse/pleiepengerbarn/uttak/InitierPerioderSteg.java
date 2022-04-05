package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.INIT_PERIODER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.MapTilBrevkode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PSBVurdererSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperioderHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

@ApplicationScoped
@BehandlingStegRef(value = INIT_PERIODER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class InitierPerioderSteg implements BehandlingSteg {

    private final Logger log = LoggerFactory.getLogger(InitierPerioderSteg.class);

    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SøknadsperiodeRepository søknadsperiodeRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste;
    private Instance<MapTilBrevkode> brevkodeMappere;

    InitierPerioderSteg() {
        // for proxy
    }

    @Inject
    public InitierPerioderSteg(BehandlingRepository behandlingRepository,
                               MottatteDokumentRepository mottatteDokumentRepository,
                               SøknadsperiodeRepository søknadsperiodeRepository,
                               UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                               @Any Instance<MapTilBrevkode> brevkodeMappere,
                               @Any PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.brevkodeMappere = brevkodeMappere;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var referanse = BehandlingReferanse.fra(behandling);
        var søknadsperiodeGrunnlag = søknadsperiodeRepository.hentGrunnlag(behandlingId).orElseThrow();
        var uttaksPerioderGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(behandlingId).orElseThrow();

        if (behandling.erManueltOpprettet()) {
            søknadsperiodeRepository.lagreRelevanteSøknadsperioder(behandlingId, søknadsperiodeGrunnlag.getOppgitteSøknadsperioder());
            uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandlingId, uttaksPerioderGrunnlag.getOppgitteSøknadsperioder());
        } else {
            var kravDokumenterMedPerioder = søknadsfristTjeneste.hentPerioderTilVurdering(referanse);
            var brevkode = MapTilBrevkode.finnBrevkodeMapper(brevkodeMappere, referanse.getFagsakYtelseType()).getBrevkode();
            var mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId())
                .stream()
                .filter(it -> it.getBehandlingId().equals(behandlingId))
                .filter(it -> brevkode.equals(it.getType()))
                .map(MottattDokument::getJournalpostId)
                .collect(Collectors.toSet());

            log.info("Fant {} dokumenter knyttet til behandlingen", mottatteDokumenter);

            var søknadsperioderHolder = mapSøknadsperioderRelevantForBehandlingen(kravDokumenterMedPerioder, mottatteDokumenter, søknadsperiodeGrunnlag);
            søknadsperiodeRepository.lagreRelevanteSøknadsperioder(behandlingId, søknadsperioderHolder);

            var uttakPerioderHolder = mapUttaksPerioderRelevantForBehandlingen(uttaksPerioderGrunnlag, mottatteDokumenter);
            uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandlingId, uttakPerioderHolder);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private UttakPerioderHolder mapUttaksPerioderRelevantForBehandlingen(UttaksPerioderGrunnlag grunnlag, Set<JournalpostId> mottatteDokumenter) {
        var perioderFraSøknader = grunnlag.getOppgitteSøknadsperioder()
            .getPerioderFraSøknadene()
            .stream()
            .filter(it -> mottatteDokumenter.contains(it.getJournalpostId()))
            .collect(Collectors.toSet());

        log.info("Fant {} dokumenter med perioder knyttet til behandlingen", perioderFraSøknader);

        if (perioderFraSøknader.size() != mottatteDokumenter.size()) {
            throw new IllegalStateException("Fant ikke alle forventede dokumenter.. " + perioderFraSøknader.size() + " != " + mottatteDokumenter.size());
        }

        return new UttakPerioderHolder(perioderFraSøknader);
    }

    private SøknadsperioderHolder mapSøknadsperioderRelevantForBehandlingen(Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravDokumenterMedPerioder,
                                                                            Set<JournalpostId> mottatteDokumenter, SøknadsperiodeGrunnlag grunnlag) {
        var entries = kravDokumenterMedPerioder.entrySet()
            .stream()
            .filter(it -> mottatteDokumenter.contains(it.getKey().getJournalpostId()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var relevanteDokumenter = grunnlag.getOppgitteSøknadsperioder()
            .getPerioder()
            .stream()
            .filter(it -> entries.keySet().stream().map(KravDokument::getJournalpostId).anyMatch(at -> at.getJournalpostId().equals(it.getJournalpostId())))
            .collect(Collectors.toSet());

        return new SøknadsperioderHolder(relevanteDokumenter);
    }
}
