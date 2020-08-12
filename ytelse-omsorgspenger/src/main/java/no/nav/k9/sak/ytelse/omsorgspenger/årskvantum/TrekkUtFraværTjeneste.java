package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.InntektsmeldingFravær;
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

    @Inject
    public TrekkUtFraværTjeneste(OmsorgspengerGrunnlagRepository grunnlagRepository, BehandlingRepository behandlingRepository, MottatteDokumentRepository mottatteDokumentRepository, InntektArbeidYtelseTjeneste iayTjeneste) {
        this.grunnlagRepository = grunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.iayTjeneste = iayTjeneste;
    }


    OppgittFravær samleSammenOppgittFravær(Long behandlingId) {

        var behandling = behandlingRepository.hentBehandling(behandlingId);

        List<OppgittFraværPeriode> fravær; // Tar med eventuelle perioder som tilkommer en åpen manuelt opprettet behandling
        if (behandling.erManueltOpprettet()) {
            var oppgittOpt = annetOppgittFravær(behandlingId);
            var perioder = oppgittOpt.orElseThrow().getPerioder();
            log.info("Legger til {} perioder fra kopiert grunnlag", perioder.size());
            fravær = fraværFraInntektsmeldingerPåFagsak(behandling);
        } else {
            var fraværFraInntektsmeldinger = fraværFraInntektsmeldingerPåBehandling(behandling);
            log.info("Legger til {} perioder fra inntektsmeldinger", fraværFraInntektsmeldinger.size());
            if (fraværFraInntektsmeldinger.isEmpty()) {
                // Dette bør da være manuelle "revurderinger" hvor vi behandler samme periode som forrige behandling på nytt
                var oppgittOpt = annetOppgittFravær(behandlingId);
                fravær = new ArrayList<>(oppgittOpt.orElseThrow().getPerioder());
            } else {
                fravær = fraværFraInntektsmeldinger;
            }
        }
        log.info("Fravær har totalt {} perioder", fravær.size());
        if (fravær.isEmpty()) {
            throw new IllegalStateException("Utvikler feil, forventer fraværsperioder til behandlingen");
        }
        return new OppgittFravær(fravær);
    }

    Optional<OppgittFravær> annetOppgittFravær(Long behandlingId) {
        return grunnlagRepository.hentOppgittFraværHvisEksisterer(behandlingId);
    }

    List<OppgittFraværPeriode> fraværFraInntektsmeldingerPåBehandling(Behandling behandling) {
        var inntektsmeldingerJournalposter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> Brevkode.INNTEKTSMELDING.equals(it.getType()))
            .filter(it -> it.getBehandlingId() != null)
            .filter(it -> behandling.getId().equals(it.getBehandlingId()))
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        log.info("Fant inntektsmeldinger knyttet til behandlingen: {}", inntektsmeldingerJournalposter);

        return trekkUtPerioderFraInntektsmeldinger(behandling, inntektsmeldingerJournalposter);
    }

    public List<OppgittFraværPeriode> fraværFraInntektsmeldingerPåFagsak(Behandling behandling) {
        var inntektsmeldingerJournalposter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> Brevkode.INNTEKTSMELDING.equals(it.getType()))
            .filter(it -> it.getBehandlingId() != null)
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        log.info("Fant inntektsmeldinger knyttet til fagsaken: {}", inntektsmeldingerJournalposter);

        return trekkUtPerioderFraInntektsmeldinger(behandling, inntektsmeldingerJournalposter);
    }

    List<OppgittFraværPeriode> trekkUtPerioderFraInntektsmeldinger(Behandling behandling, Set<JournalpostId> inntektsmeldingerJournalposter) {
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
            .sorted(Inntektsmelding.COMP_REKKEFØLGE)
            .collect(Collectors.toCollection(LinkedHashSet::new));

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

    List<OppgittFraværPeriode> trekkUtFravær(Set<Inntektsmelding> inntektsmeldinger) {
        return new InntektsmeldingFravær().trekkUtAlleFraværOgValiderOverlapp(inntektsmeldinger);
    }
}
