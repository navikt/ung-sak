package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperioderHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

@ApplicationScoped
@BehandlingStegRef(kode = "INIT_PERIODER")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class InitierPerioderSteg implements BehandlingSteg {

    private final Logger log = LoggerFactory.getLogger(InitierPerioderSteg.class);

    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SøknadsperiodeRepository søknadsperiodeRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;

    InitierPerioderSteg() {
        // for proxy
    }

    @Inject
    public InitierPerioderSteg(BehandlingRepository behandlingRepository,
                               MottatteDokumentRepository mottatteDokumentRepository,
                               SøknadsperiodeRepository søknadsperiodeRepository,
                               UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                               @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var referanse = BehandlingReferanse.fra(behandling);
        var uttaksPerioderGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(behandlingId).orElseThrow();

        if (behandling.erManueltOpprettet()) {
            var kravDokumenterMedPerioder = søknadsfristTjeneste.vurderSøknadsfrist(referanse);

            log.info("Fant {} dokumenter knyttet til fagsaken", kravDokumenterMedPerioder.size());

            var søknadsperioderHolder = mapRelevanteSøknadsperioder(kravDokumenterMedPerioder);
            søknadsperiodeRepository.lagreRelevanteSøknadsperioder(behandlingId, søknadsperioderHolder);

            uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandlingId, uttaksPerioderGrunnlag.getOppgitteSøknadsperioder());
        } else {
            var kravDokumenterMedPerioder = søknadsfristTjeneste.relevanteVurderteKravdokumentMedPeriodeForBehandling(referanse);

            log.info("Fant {} dokumenter knyttet til behandlingen", kravDokumenterMedPerioder.size());

            var søknadsperioderHolder = mapRelevanteSøknadsperioder(kravDokumenterMedPerioder);
            søknadsperiodeRepository.lagreRelevanteSøknadsperioder(behandlingId, søknadsperioderHolder);

            var uttakPerioderHolder = mapUttaksPerioderRelevantForBehandlingen(uttaksPerioderGrunnlag, kravDokumenterMedPerioder.keySet());
            uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandlingId, uttakPerioderHolder);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private UttakPerioderHolder mapUttaksPerioderRelevantForBehandlingen(UttaksPerioderGrunnlag grunnlag, Set<KravDokument> kravdokumenter) {
        var mottatteDokumenter = kravdokumenter.stream()
            .map(KravDokument::getJournalpostId)
            .collect(Collectors.toSet());

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

    private SøknadsperioderHolder mapRelevanteSøknadsperioder(Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravDokumenterMedPerioder) {

        var relevanteDokumenter = kravDokumenterMedPerioder.entrySet().stream()
            .map(this::mapTilSøknadsPerioder)
            .collect(Collectors.toSet());

        log.info("Fant {} dokumenter knyttet til fagsaken", kravDokumenterMedPerioder.size());

        return new SøknadsperioderHolder(relevanteDokumenter);
    }

    private Søknadsperioder mapTilSøknadsPerioder(Map.Entry<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> entry) {
        var perioder = entry.getValue()
            .stream()
            .filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()))
            .map(VurdertSøktPeriode::getRaw)
            .collect(Collectors.toSet());
        return new Søknadsperioder(entry.getKey().getJournalpostId(), perioder);
    }
}
