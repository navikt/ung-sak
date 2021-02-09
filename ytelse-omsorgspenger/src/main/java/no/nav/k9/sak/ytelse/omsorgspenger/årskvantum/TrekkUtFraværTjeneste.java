package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
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
            fravær = fraværPåFagsak(behandling);
        } else {
            var fraværFraKravDokument = fraværPåBehandling(behandling);
            log.info("Legger til totalt {} perioder fra inntektsmeldinger og søknader", fraværFraKravDokument.size());
            if (fraværFraKravDokument.isEmpty()) {
                // Dette bør da være manuelle "revurderinger" hvor vi behandler samme periode som forrige behandling på nytt
                var oppgittOpt = annetOppgittFravær(behandlingId);
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

    Optional<OppgittFravær> annetOppgittFravær(Long behandlingId) {
        return grunnlagRepository.hentOppgittFraværHvisEksisterer(behandlingId);
    }

    private List<OppgittFraværPeriode> fraværPåBehandling(Behandling behandling) {
        var vurdertePerioder = søknadsfristTjeneste.vurderSøknadsfrist(BehandlingReferanse.fra(behandling));

        var antallIM = vurdertePerioder.keySet().stream().filter(type -> KravDokumentType.INNTEKTSMELDING.equals(type.getType())).count();
        var antallSøknader = vurdertePerioder.keySet().stream().filter(type -> KravDokumentType.SØKNAD.equals(type.getType())).count();
        log.info("Fant {} inntektsmeldinger og {} søknader knyttet til behandlingen:", antallIM, antallSøknader);

        return vurdertePerioder.values().stream()
            .flatMap(Collection::stream)
            .map(VurdertSøktPeriode::getRaw)
            .collect(Collectors.toList());
    }

    public List<OppgittFraværPeriode> fraværFraInntektsmeldingerPåFagsak(Behandling behandling) {
        var søkteFraværsperioderIm = fraværMedInnsendingstidspunktFraInntektsmeldingerPåFagsak(behandling);
        var vurdertePerioder = søknadsfristTjeneste.vurderSøknadsfrist(søkteFraværsperioderIm);

        return vurdertePerioder.values()
            .stream()
            .flatMap(Collection::stream)
            .map(VurdertSøktPeriode::getRaw)
            .collect(Collectors.toList());
    }

    public List<OppgittFraværPeriode> fraværPåFagsak(Behandling behandling) {
        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søkteFraværsperioder = new HashMap<>();
        søkteFraværsperioder.putAll(fraværMedInnsendingstidspunktFraInntektsmeldingerPåFagsak(behandling));
        søkteFraværsperioder.putAll(fraværMedInnsendingstidspunktFraSøknaderPåFagsak(behandling));

        var vurdertePerioder = søknadsfristTjeneste.vurderSøknadsfrist(søkteFraværsperioder);

        return vurdertePerioder.values().stream()
            .flatMap(Collection::stream)
            .map(VurdertSøktPeriode::getRaw)
            .collect(Collectors.toList());
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> fraværMedInnsendingstidspunktFraInntektsmeldingerPåFagsak(Behandling behandling) {
        var inntektsmeldingerJournalposter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> Brevkode.INNTEKTSMELDING.equals(it.getType()))
            .filter(it -> it.getBehandlingId() != null)
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        log.info("Fant inntektsmeldinger knyttet til fagsaken: {}", inntektsmeldingerJournalposter);

        return trekkUtOppgittFraværFraInntektsmeldinger(behandling, inntektsmeldingerJournalposter);
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> fraværMedInnsendingstidspunktFraSøknaderPåFagsak(Behandling behandling) {
        return søknadPerioderTjeneste.hentSøktePerioderMedKravdokument(behandling.getFagsak());
    }

    public List<WrappedOppgittFraværPeriode> fraværFraKravDokumenterPåFagsakMedSøknadsfristVurdering(Behandling behandling) {
        return trekkUtFravær(søknadsfristTjeneste.vurderSøknadsfrist(BehandlingReferanse.fra(behandling)));
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> trekkUtOppgittFraværFraInntektsmeldinger(Behandling behandling, Set<JournalpostId> inntektsmeldingerJournalposter) {
        if (inntektsmeldingerJournalposter.isEmpty()) {
            return Map.of();
        }

        var fagsak = behandling.getFagsak();
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

    public List<WrappedOppgittFraværPeriode> trekkUtFravær(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraInntektsmelding) {
        return new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(fraværFraInntektsmelding);
    }
}
