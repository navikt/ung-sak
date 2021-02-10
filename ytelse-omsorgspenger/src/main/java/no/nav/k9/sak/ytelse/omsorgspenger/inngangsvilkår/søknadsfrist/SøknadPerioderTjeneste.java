package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlag;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@Dependent
public class SøknadPerioderTjeneste {

    private final static KravDokumentType KRAVDOKUMENTTYPE_SØKNAD = KravDokumentType.SØKNAD;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private BehandlingRepository behandlingRepository;


    @Inject
    public SøknadPerioderTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                  OmsorgspengerGrunnlagRepository grunnlagRepository,
                                  BehandlingRepository behandlingRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.grunnlagRepository = grunnlagRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> hentSøktePerioderMedKravdokument(Fagsak fagsak) {
        // Bør henlagte behandlinger filtreres?
        var alleBehandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(fagsak.getSaksnummer());

        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> oppgittFraværMedKravdokument = new HashMap<>();
        alleBehandlinger.stream().forEach(b -> {
            var fraværFraBehandling = hentSøktePerioderMedKravdokument(BehandlingReferanse.fra(b));
            fraværFraBehandling.putAll(fraværFraBehandling);
        });
        return oppgittFraværMedKravdokument;
    }


    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> hentSøktePerioderMedKravdokument(BehandlingReferanse referanse) {
        var uttakPerioder = grunnlagRepository.hentGrunnlag(referanse.getBehandlingId())
            .map(OmsorgspengerGrunnlag::getOppgittFraværFraSøknad)
            .filter(Objects::nonNull)
            .map(OppgittFravær::getPerioder)
            .orElse(Set.of());
        return mapMotMottattDokument(referanse.getFagsakId(), uttakPerioder);
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> mapMotMottattDokument(long fagsakId, Set<OppgittFraværPeriode> uttakPerioder) {
        var jpIder = uttakPerioder.stream().map(it -> it.getJournalpostId()).collect(Collectors.toSet());

        var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokument(fagsakId, jpIder)
            .stream()
            .filter(dok -> Brevkode.SØKNAD_UTBETALING_OMS.equals(dok.getType()))
            .collect(Collectors.toList());

        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søktePerioderMedKravDokument = new HashMap<>();
        mottatteDokumenter.forEach(dok -> {
            var kravDokument = new KravDokument(dok.getJournalpostId(), dok.getMottattTidspunkt(), KRAVDOKUMENTTYPE_SØKNAD);
            var OppgittFraværPerioder = uttakPerioder.stream().filter(it -> it.getJournalpostId().equals(dok.getJournalpostId())).collect(Collectors.toList());
            var oppgitteFraværPerioder = OppgittFraværPerioder.stream()
                .map(pa -> new OppgittFraværPeriode(pa.getFom(), pa.getTom(), pa.getAktivitetType(), pa.getArbeidsgiver(), pa.getArbeidsforholdRef(), pa.getFraværPerDag()))
                .map(op -> new SøktPeriode<>(op.getPeriode(), op.getAktivitetType(), op.getArbeidsgiver(), op.getArbeidsforholdRef(), op))
                .collect(Collectors.toList());

            søktePerioderMedKravDokument.put(kravDokument, oppgitteFraværPerioder);
        });
        return søktePerioderMedKravDokument;
    }
}
