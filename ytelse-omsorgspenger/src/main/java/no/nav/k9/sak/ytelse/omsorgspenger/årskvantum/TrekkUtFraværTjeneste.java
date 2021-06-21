package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import java.util.ArrayList;
import java.util.Collection;
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
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist.SøknadPerioderTjeneste;
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

    private TrekkUtOppgittFraværPeriode mapOppgittFravær;
    private AvklartSøknadsfristRepository avklartSøknadsfristRepository;

    @Inject
    public TrekkUtFraværTjeneste(OmsorgspengerGrunnlagRepository grunnlagRepository,
                                 BehandlingRepository behandlingRepository,
                                 MottatteDokumentRepository mottatteDokumentRepository,
                                 InntektArbeidYtelseTjeneste iayTjeneste,
                                 @FagsakYtelseTypeRef("OMP") VurderSøknadsfristTjeneste<OppgittFraværPeriode> søknadsfristTjeneste,
                                 SøknadPerioderTjeneste søknadPerioderTjeneste,
                                 TrekkUtOppgittFraværPeriode mapOppgittFravær,
                                 AvklartSøknadsfristRepository avklartSøknadsfristRepository) {
        this.grunnlagRepository = grunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.iayTjeneste = iayTjeneste;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.søknadPerioderTjeneste = søknadPerioderTjeneste;
        this.mapOppgittFravær = mapOppgittFravær;
        this.avklartSøknadsfristRepository = avklartSøknadsfristRepository;
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
                log.warn("Forventer ny periode til behandling fra IM eller søknad, siden dette ikke er manuell revurdering.");
                var oppgittOpt = annetOppgittFravær(behandling);
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

    Optional<OppgittFravær> annetOppgittFravær(Behandling behandling) {
        return grunnlagRepository.hentOppittFraværHvisEksisterer(behandling.getUuid());
    }

    private List<OppgittFraværPeriode> fraværPåBehandling(Behandling behandling) {
        LinkedHashSet<Inntektsmelding> inntektsmeldingerPåBehandling = inntektsmeldingerPåBehandling(behandling);
        var avklartSøknadsfristResultat = avklartSøknadsfristRepository.hentHvisEksisterer(behandling.getId());
        var vurderteKravOgPerioder = mapOppgittFravær.mapFra(inntektsmeldingerPåBehandling, fraværMedInnsendingstidspunktFraSøknaderPåBehandling(behandling), avklartSøknadsfristResultat);
        return trekkUtFravær(vurderteKravOgPerioder).stream().map(WrappedOppgittFraværPeriode::getPeriode).collect(Collectors.toList());
    }

    public List<OppgittFraværPeriode> fraværFraInntektsmeldingerPåFagsak(Behandling behandling) {
        var inntektsmeldingerPåFagsak = inntektsmeldingerPåFagsak(behandling.getFagsak());
        var avklartSøknadsfristResultat = avklartSøknadsfristRepository.hentHvisEksisterer(behandling.getId());
        var vurderteKravOgPerioder = mapOppgittFravær.mapFra(inntektsmeldingerPåFagsak, Map.of(), avklartSøknadsfristResultat);
        return trekkUtFravær(vurderteKravOgPerioder).stream().map(WrappedOppgittFraværPeriode::getPeriode).collect(Collectors.toList());
    }

    public List<OppgittFraværPeriode> fraværPåFagsak(Behandling behandling) {
        var inntektsmeldingerPåFagsak = inntektsmeldingerPåFagsak(behandling.getFagsak());
        var avklartSøknadsfristResultat = avklartSøknadsfristRepository.hentHvisEksisterer(behandling.getId());
        var vurdertePerioder = mapOppgittFravær.mapFra(inntektsmeldingerPåFagsak, fraværMedInnsendingstidspunktFraSøknaderPåFagsak(behandling), avklartSøknadsfristResultat);

        // TBD: hvofor bruker ikke denne #trekkUtFravær som de andre over?
        return vurdertePerioder.values().stream()
            .flatMap(Collection::stream)
            .map(VurdertSøktPeriode::getRaw)
            .collect(Collectors.toList());
    }

    private LinkedHashSet<Inntektsmelding> inntektsmeldingerPåBehandling(Behandling behandling) {
        var inntektsmeldingerJournalposter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> Brevkode.INNTEKTSMELDING.equals(it.getType()))
            .filter(it -> DokumentStatus.GYLDIG.equals(it.getStatus()))
            .filter(it -> it.getBehandlingId() != null && it.getBehandlingId().equals(behandling.getId()))
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        log.info("Fant inntektsmeldinger knyttet til fagsaken: {}", inntektsmeldingerJournalposter);

        return hentInntektsmeldinger(behandling.getFagsak(), inntektsmeldingerJournalposter);
    }

    private LinkedHashSet<Inntektsmelding> inntektsmeldingerPåFagsak(Fagsak fagsak) {
        var inntektsmeldingerJournalposter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsak.getId())
            .stream()
            .filter(it -> Brevkode.INNTEKTSMELDING.equals(it.getType()))
            .filter(it -> DokumentStatus.GYLDIG.equals(it.getStatus()))
            .filter(it -> it.getBehandlingId() != null)
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        log.info("Fant inntektsmeldinger knyttet til fagsaken: {}", inntektsmeldingerJournalposter);

        return hentInntektsmeldinger(fagsak, inntektsmeldingerJournalposter);
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> fraværMedInnsendingstidspunktFraSøknaderPåFagsak(Behandling behandling) {
        return søknadPerioderTjeneste.hentSøktePerioderMedKravdokumentPåFagsak(BehandlingReferanse.fra(behandling));
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> fraværMedInnsendingstidspunktFraSøknaderPåBehandling(Behandling behandling) {
        return søknadPerioderTjeneste.hentSøktePerioderMedKravdokumentPåBehandling(BehandlingReferanse.fra(behandling));
    }

    public List<WrappedOppgittFraværPeriode> fraværFraKravDokumenterPåFagsakMedSøknadsfristVurdering(Behandling behandling) {
        return trekkUtFravær(søknadsfristTjeneste.vurderSøknadsfrist(BehandlingReferanse.fra(behandling)));
    }

    private LinkedHashSet<Inntektsmelding> hentInntektsmeldinger(Fagsak fagsak, Set<JournalpostId> inntektsmeldingerJournalposter) {
        if (inntektsmeldingerJournalposter.isEmpty()) {
            return new LinkedHashSet<>();
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
        return inntektsmeldinger;
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

    public List<WrappedOppgittFraværPeriode> trekkUtFravær(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværPerKravdokument) {
        return mapOppgittFravær.trekkUtFravær(fraværPerKravdokument);
    }
}
