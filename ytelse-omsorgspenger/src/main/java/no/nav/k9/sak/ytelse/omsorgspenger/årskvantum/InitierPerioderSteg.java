package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.InntektsmeldingFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

/**
 * Samle sammen fakta for fravær.
 */
@ApplicationScoped
@BehandlingStegRef(kode = "INIT_PERIODER")
@BehandlingTypeRef
@FagsakYtelseTypeRef("OMP")
public class InitierPerioderSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(InitierPerioderSteg.class);

    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    protected InitierPerioderSteg() {
        // for proxy
    }

    @Inject
    public InitierPerioderSteg(OmsorgspengerGrunnlagRepository grunnlagRepository,
                               BehandlingRepository behandlingRepository,
                               MottatteDokumentRepository mottatteDokumentRepository,
                               InntektArbeidYtelseTjeneste iayTjeneste) {
        this.grunnlagRepository = grunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.iayTjeneste = iayTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();

        var samletFravær = samleSammenOppgittFravær(behandlingId);
        grunnlagRepository.lagreOgFlushOppgittFravær(behandlingId, samletFravær);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private OppgittFravær samleSammenOppgittFravær(Long behandlingId) {
        Set<OppgittFraværPeriode> fravær = new LinkedHashSet<>();

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fraværFraInntektsmeldinger = fraværFraInntektsmeldinger(behandling);
        if (behandling.erManueltOpprettet()) {
            var oppgittOpt = annetOppgittFravær(behandlingId);
            fravær.addAll(oppgittOpt.orElseThrow().getPerioder());
        } else {
            if (fraværFraInntektsmeldinger.isEmpty()) {
                // Dette bør da være manuelle "revurderinger" hvor vi behandler samme periode som forrige behandling på nytt
                var oppgittOpt = annetOppgittFravær(behandlingId);
                fravær.addAll(oppgittOpt.orElseThrow().getPerioder());
            }
        }
        fravær.addAll(fraværFraInntektsmeldinger); // Tar med eventuelle perioder som tilkommer en åpen manuelt opprettet behandling
        return new OppgittFravær(fravær);
    }

    private Optional<OppgittFravær> annetOppgittFravær(Long behandlingId) {
        return grunnlagRepository.hentOppgittFraværHvisEksisterer(behandlingId);
    }

    private List<OppgittFraværPeriode> fraværFraInntektsmeldinger(Behandling behandling) {
        var inntektsmeldingerJournalposter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> Brevkode.INNTEKTSMELDING.equals(it.getType()))
            .filter(it -> it.getBehandlingId() != null)
            .filter(it -> behandling.getId().equals(it.getBehandlingId()))
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        log.info("Fant inntektsmeldinger knyttet til behandlingen: {}", inntektsmeldingerJournalposter);

        if (inntektsmeldingerJournalposter.isEmpty()) {
            return List.of();
        }

        var fagsak = behandling.getFagsak();
        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(fagsak.getSaksnummer(), fagsak.getAktørId(), fagsak.getYtelseType());
        if (sakInntektsmeldinger.isEmpty()) {
            // Abakus setter ikke ytelsetype på "koblingen" før registerinnhenting så vil bare være feil før første registerinnhenting..
            sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(fagsak.getSaksnummer(), fagsak.getAktørId(), FagsakYtelseType.UDEFINERT);
        }
        var inntektsmeldinger = sakInntektsmeldinger.stream()
            .filter(it -> inntektsmeldingerJournalposter.contains(it.getJournalpostId()))
            .collect(Collectors.toSet());

        if (inntektsmeldinger.size() != inntektsmeldingerJournalposter.size()) {
            var journalposterSomMangler = inntektsmeldingerJournalposter.stream()
                .filter(it -> inntektsmeldinger.stream()
                    .map(Inntektsmelding::getJournalpostId)
                    .noneMatch(it::equals))
                .collect(Collectors.toSet());
            log.warn("Fant inntektsmeldinger '{}' knyttet til behandlingen men følgende mangler '{}'", inntektsmeldingerJournalposter, journalposterSomMangler);
            throw new IllegalStateException("Har inntektsmeldinger som er knyttet til behandlingen, men finner ikke disse i abakus");
        }

        return trekkUtFravær(inntektsmeldinger);
    }

    private List<OppgittFraværPeriode> trekkUtFravær(Set<Inntektsmelding> inntektsmeldinger) {
        return new InntektsmeldingFravær().trekkUtAlleFraværOgValiderOverlapp(inntektsmeldinger);
    }

}
