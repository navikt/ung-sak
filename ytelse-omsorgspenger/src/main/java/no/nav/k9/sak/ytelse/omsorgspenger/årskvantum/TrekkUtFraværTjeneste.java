package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist.InntektsmeldingSøktePerioderMapper;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist.SøknadPerioderTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.KravDokumentFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@Dependent
public class TrekkUtFraværTjeneste {
    private static final Logger log = LoggerFactory.getLogger(TrekkUtFraværTjeneste.class);

    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VurderSøknadsfristTjeneste<OppgittFraværPeriode> søknadsfristTjeneste;
    private SøknadPerioderTjeneste søknadPerioderTjeneste;

    private InntektsmeldingSøktePerioderMapper inntektsmeldingMapper;

    @Inject
    public TrekkUtFraværTjeneste(OmsorgspengerGrunnlagRepository grunnlagRepository,
                                 BehandlingRepository behandlingRepository,
                                 MottatteDokumentRepository mottatteDokumentRepository,
                                 InntektArbeidYtelseTjeneste iayTjeneste,
                                 @FagsakYtelseTypeRef("OMP") VurderSøknadsfristTjeneste<OppgittFraværPeriode> søknadsfristTjeneste,
                                 SøknadPerioderTjeneste søknadPerioderTjeneste,
                                 InntektsmeldingSøktePerioderMapper inntektsmeldingMapper) {
        this.grunnlagRepository = grunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.iayTjeneste = iayTjeneste;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.søknadPerioderTjeneste = søknadPerioderTjeneste;
        this.inntektsmeldingMapper = inntektsmeldingMapper;
    }

    OppgittFravær samleSammenOppgittFravær(Long behandlingId) {

        var behandling = behandlingRepository.hentBehandling(behandlingId);

        List<OppgittFraværPeriode> fravær; // Tar med eventuelle perioder som tilkommer en åpen manuelt opprettet behandling
        if (behandling.erManueltOpprettet()) {
            fravær = alleFraværsperioderPåFagsak(behandling);
        } else {
            var fraværFraKravDokument = fraværPåBehandling(behandling);
            log.info("Legger til totalt {} perioder fra inntektsmeldinger og søknader", fraværFraKravDokument.size());
            if (fraværFraKravDokument.isEmpty()) {
                // Kan inntreffe dersom IM er av variant ikkeFravaer eller ikke refusjon. Da brukes fraværsperioder kopiert fra forrige behandling
                // TODO: Logg heller dokumenter tilknyttet behandling
                log.warn("Kun kravdokument uten fraværsperioder er knyttet til behandling. Fraværsperioder fra tidligere behandlinger brukes, forventer noop for ytelse.");
                var oppgittOpt = grunnlagRepository.hentSammenslåttOppgittFraværHvisEksisterer(behandling.getId());
                fravær = new ArrayList<>(oppgittOpt.orElseThrow().getPerioder());
            } else {
                fravær = fraværFraKravDokument;
            }
        }
        log.info("Fravær har totalt {} perioder: {}",
            fravær.size(),
            fravær.stream()
                .map(OppgittFraværPeriode::getPeriode)
                .collect(Collectors.toList()));
        if (fravær.isEmpty()) {
            throw new IllegalStateException("Utvikler feil, forventer fraværsperioder til behandlingen");
        }
        return new OppgittFravær(fravær);
    }

    private List<OppgittFraværPeriode> fraværPåBehandling(Behandling behandling) {
        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søkteFraværsperioder = new LinkedHashMap<>();
        søkteFraværsperioder.putAll(søktFraværFraImPåBehandling(behandling));
        søkteFraværsperioder.putAll(fraværFraSøknaderPåBehandling(behandling));

        var vurdertePerioder = søknadsfristTjeneste.vurderSøknadsfrist(behandling.getId(), søkteFraværsperioder);
        log.info("Fant {} inntektsmeldinger og {} søknader knyttet til behandlingen:", countIm(vurdertePerioder), countSøknad(vurdertePerioder));

        return trekkUtFravær(vurdertePerioder).stream().map(WrappedOppgittFraværPeriode::getPeriode).collect(Collectors.toList());
    }

    public List<OppgittFraværPeriode> fraværFraInntektsmeldingerPåFagsak(Behandling behandling) {
        var fraværPåFagsak = søktFraværFraImPåFagsak(behandling.getFagsak());

        var vurdertePerioder = søknadsfristTjeneste.vurderSøknadsfrist(behandling.getId(), fraværPåFagsak);
        log.info("Fant {} inntektsmeldinger knyttet med fræværsperioder for behandlingen:", countIm(vurdertePerioder));

        return trekkUtFravær(vurdertePerioder).stream().map(WrappedOppgittFraværPeriode::getPeriode).collect(Collectors.toList());
    }

    public List<OppgittFraværPeriode> alleFraværsperioderPåFagsak(Behandling behandling) {
        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søkteFraværsperioder = new LinkedHashMap<>();
        søkteFraværsperioder.putAll(søktFraværFraImPåFagsak(behandling.getFagsak()));
        søkteFraværsperioder.putAll(fraværFraSøknaderPåFagsak(behandling));

        var vurdertePerioder = søknadsfristTjeneste.vurderSøknadsfrist(behandling.getId(), søkteFraværsperioder);
        log.info("Fant {} inntektsmeldinger med fraværsperioder og {} søknader knyttet til fagsaken", countIm(vurdertePerioder), countSøknad(vurdertePerioder));

        return trekkUtFravær(vurdertePerioder).stream().map(WrappedOppgittFraværPeriode::getPeriode).collect(Collectors.toList());
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søktFraværFraImPåBehandling(Behandling behandling) {
        var inntektsmeldingerJournalposter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> Brevkode.INNTEKTSMELDING.equals(it.getType()))
            .filter(it -> it.getBehandlingId() != null && it.getBehandlingId().equals(behandling.getId()))
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        log.info("Fant {} inntektsmeldinger knyttet til behandlingen", inntektsmeldingerJournalposter);

        return hentFraværFraInntektsmeldinger(behandling.getFagsak(), inntektsmeldingerJournalposter);
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søktFraværFraImPåFagsak(Fagsak fagsak) {
        var inntektsmeldingerJournalposter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(fagsak.getId())
            .stream()
            .filter(it -> Brevkode.INNTEKTSMELDING.equals(it.getType()))
            .filter(it -> it.getBehandlingId() != null)
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        log.info("Fant {} inntektsmeldinger knyttet til fagsaken", inntektsmeldingerJournalposter);

        return hentFraværFraInntektsmeldinger(fagsak, inntektsmeldingerJournalposter);
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> fraværFraSøknaderPåFagsak(Behandling behandling) {
        return søknadPerioderTjeneste.hentSøktePerioderMedKravdokumentPåFagsak(BehandlingReferanse.fra(behandling));
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> fraværFraSøknaderPåBehandling(Behandling behandling) {
        return søknadPerioderTjeneste.hentSøktePerioderMedKravdokumentPåBehandling(BehandlingReferanse.fra(behandling));
    }

    public List<WrappedOppgittFraværPeriode> fraværFraKravDokumenterPåFagsakMedSøknadsfristVurdering(Behandling behandling) {
        var kravdokumenterMedFraværsperioder = søknadsfristTjeneste.vurderSøknadsfrist(BehandlingReferanse.fra(behandling));
        return trekkUtFravær(kravdokumenterMedFraværsperioder);
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> hentFraværFraInntektsmeldinger(Fagsak fagsak, Set<JournalpostId> inntektsmeldingerJournalposter) {
        if (inntektsmeldingerJournalposter.isEmpty()) {
            return Map.of();
        }
        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(fagsak.getSaksnummer(), fagsak.getAktørId(), fagsak.getYtelseType());
        if (sakInntektsmeldinger.isEmpty()) {
            // Abakus setter ikke ytelsetype på "koblingen" før registerinnhenting så vil bare være feil før første registerinnhenting..
            sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(fagsak.getSaksnummer(), fagsak.getAktørId(), FagsakYtelseType.UDEFINERT);
        }
        var inntektsmeldinger = sakInntektsmeldinger.stream()
            .filter(it -> inntektsmeldingerJournalposter.contains(it.getJournalpostId()))
            .sorted(Inntektsmelding.COMP_REKKEFØLGE)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        sjekkOmInntektsmeldingerMatcher(inntektsmeldingerJournalposter, inntektsmeldinger);
        return inntektsmeldingMapper.mapTilSøktePerioder(inntektsmeldinger);
    }

    private void sjekkOmInntektsmeldingerMatcher(Set<JournalpostId> inntektsmeldingerJournalposter, LinkedHashSet<Inntektsmelding> inntektsmeldinger) {
        var abakusJournalposter = inntektsmeldinger.stream().map(Inntektsmelding::getJournalpostId).collect(Collectors.toCollection(LinkedHashSet::new));
        var abakusJournalposterSomMangler = new LinkedHashSet<>(abakusJournalposter);
        abakusJournalposterSomMangler.removeAll(inntektsmeldingerJournalposter);

        if (!abakusJournalposterSomMangler.isEmpty()) {
            throw new IllegalStateException(
                "Har inntektsmeldinger i abakus " + abakusJournalposterSomMangler
                    + " som er knyttet til behandlingen, men har ikke knyttet disse i til behandling (har følgende knyttet til behandling: "
                    + inntektsmeldingerJournalposter + ", mangler: " + abakusJournalposterSomMangler + ")");
        }

        var journalposterSomMangler = new LinkedHashSet<>(inntektsmeldingerJournalposter);
        journalposterSomMangler.removeAll(abakusJournalposter);

        if (!journalposterSomMangler.isEmpty()) {
            throw new IllegalStateException(
                "Har inntektsmeldinger " + inntektsmeldingerJournalposter
                    + " som er knyttet til behandlingen, men finner ikke disse i abakus (har følgende i abakus: "
                    + abakusJournalposter + ", mangler " + journalposterSomMangler + ")");
        }
    }

    // Slår sammen overlappende perioder fra kravdokumenter (IM-er, søknader)
    public List<WrappedOppgittFraværPeriode> trekkUtFravær(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværPerKravdokument) {
        return new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(fraværPerKravdokument);
    }

    private long countIm(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> vurdertePerioder) {
        return vurdertePerioder.keySet().stream().filter(type -> List.of(KravDokumentType.INNTEKTSMELDING, KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV).contains(type.getType())).count();
    }

    private long countSøknad(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> vurdertePerioder) {
        return vurdertePerioder.keySet().stream().filter(type -> KravDokumentType.SØKNAD.equals(type.getType())).count();
    }
}
