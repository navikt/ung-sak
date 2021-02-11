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
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@Dependent
public class SøknadPerioderTjeneste {

    private final static KravDokumentType KRAVDOKUMENTTYPE_SØKNAD = KravDokumentType.SØKNAD;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private OmsorgspengerGrunnlagRepository grunnlagRepository;


    @Inject
    public SøknadPerioderTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                  OmsorgspengerGrunnlagRepository grunnlagRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.grunnlagRepository = grunnlagRepository;
    }

    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> hentSøktePerioderMedKravdokument(Fagsak fagsak) {
        var mottatteDokumenter = hentMottatteDokument(fagsak.getId());
        return tilSøktePerioderMedKravdokument(mottatteDokumenter);
    }

    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> hentSøktePerioderMedKravdokument(BehandlingReferanse ref) {
        var mottatteDokumenter = hentMottatteDokument(ref.getFagsakId())
            .stream()
            .filter(dok -> ref.getBehandlingId().equals(dok.getBehandlingId()))
            .collect(Collectors.toList());
        return tilSøktePerioderMedKravdokument(mottatteDokumenter);
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> tilSøktePerioderMedKravdokument(List<MottattDokument> mottatteDokumenter) {
        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søktePerioderMedKravDokument = new HashMap<>();
        for (MottattDokument dok : mottatteDokumenter) {
            var kravDokument = new KravDokument(dok.getJournalpostId(), dok.getMottattTidspunkt(), KRAVDOKUMENTTYPE_SØKNAD);
            var fraværPerioder = grunnlagRepository.hentOppgittFraværFraSøknadHvisEksisterer(dok.getBehandlingId())
                .filter(Objects::nonNull)
                .map(OppgittFravær::getPerioder)
                .orElse(Set.of());
            var søktePerioderPåJp = fraværPerioder.stream()
                .filter(fp -> dok.getJournalpostId().equals(fp.getJournalpostId()))
                .map(fp -> new SøktPeriode<>(fp.getPeriode(), fp.getAktivitetType(), fp.getArbeidsgiver(), fp.getArbeidsforholdRef(), fp))
                .collect(Collectors.toList());
            søktePerioderMedKravDokument.put(kravDokument, søktePerioderPåJp);
        }
        return søktePerioderMedKravDokument;
    }

    private List<MottattDokument> hentMottatteDokument(long fagsakId) {
        var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId, DokumentStatus.GYLDIG)
            .stream()
            .filter(dok -> Brevkode.SØKNAD_UTBETALING_OMS.equals(dok.getType()))
            .filter(dok -> dok.getBehandlingId() != null)
            .collect(Collectors.toList());
        return mottatteDokumenter;
    }
}
