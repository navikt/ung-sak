package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
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

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
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
                               InntektArbeidYtelseTjeneste iayTjeneste,
                               InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.grunnlagRepository = grunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.iayTjeneste = iayTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
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

        var fraværFraInntektsmeldinger = fraværFraInntektsmeldinger(behandlingId);

        if (fraværFraInntektsmeldinger.isEmpty()) {
            // Dette bør da være "revurderinger" hvor vi behandler samme periode som forrige behandling på nytt
            var oppgittOpt = annetOppgittFravær(behandlingId);
            fravær.addAll(oppgittOpt.orElseThrow().getPerioder());
        }
        fravær.addAll(fraværFraInntektsmeldinger);
        return new OppgittFravær(fravær);
    }

    private Optional<OppgittFravær> annetOppgittFravær(Long behandlingId) {
        return grunnlagRepository.hentOppgittFraværHvisEksisterer(behandlingId);
    }

    private List<OppgittFraværPeriode> fraværFraInntektsmeldinger(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var inntektsmeldingerJournalposter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())
            .stream()
            .filter(it -> Brevkode.INNTEKTSMELDING.equals(it.getType()))
            .filter(it -> behandlingId.equals(it.getBehandlingId()) || it.getBehandlingId() == null)
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());

        var fagsak = behandling.getFagsak();
        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(fagsak.getSaksnummer(), fagsak.getAktørId(), fagsak.getYtelseType());
        if (!inntektsmeldingerJournalposter.isEmpty() && sakInntektsmeldinger.isEmpty()) {
            // Abakus setter ikke ytelsetype på "koblingen" før registerinnhenting så vil bare være feil før første registerinnhenting..
            sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(fagsak.getSaksnummer(), fagsak.getAktørId(), FagsakYtelseType.UDEFINERT);
        }
        var inntektsmeldinger = sakInntektsmeldinger.stream()
            .filter(it -> inntektsmeldingerJournalposter.contains(it.getJournalpostId()))
            .collect(Collectors.toSet());

        return trekkUtFravær(inntektsmeldinger);
    }

    private List<OppgittFraværPeriode> trekkUtFravær(Set<Inntektsmelding> inntektsmeldinger) {
        return new InntektsmeldingFravær().trekkUtAlleFraværOgValiderOverlapp(inntektsmeldinger);
    }

}
