package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@Dependent
public class SøknadPerioderTjeneste {

    private final static KravDokumentType KRAVDOKUMENTTYPE_SØKNAD = KravDokumentType.SØKNAD;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SøknadParser søknadParser;


    @Inject
    public SøknadPerioderTjeneste(MottatteDokumentRepository mottatteDokumentRepository, SøknadParser søknadParser) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadParser = søknadParser;
    }

    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> hentSøktePerioderMedKravdokument(BehandlingReferanse referanse) {
        var mottatteDokumenter = hentMottatteDokument(referanse.getFagsakId()).stream()
            .filter(it -> referanse.getBehandlingId().equals(it.getBehandlingId()))
            .collect(Collectors.toList());
        var kravDokumentMapMedSøktePerioder = tilSøktePerioderMedKravDokument(mottatteDokumenter);
        return kravDokumentMapMedSøktePerioder;
    }

    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> hentSøktePerioderMedKravdokument(Fagsak fagsak) {
        var mottatteDokumenter = hentMottatteDokument(fagsak.getId());
        var kravDokumentMapMedSøktePerioder = tilSøktePerioderMedKravDokument(mottatteDokumenter);
        return kravDokumentMapMedSøktePerioder;
    }

    private List<MottattDokument> hentMottatteDokument(long fagsakId) {
        var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId, DokumentStatus.GYLDIG)
            .stream()
            .filter(dok -> Brevkode.SØKNAD_UTBETALING_OMS.equals(dok.getType()))
            .filter(dok -> dok.getBehandlingId() != null)
            .collect(Collectors.toList());
        return mottatteDokumenter;
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> tilSøktePerioderMedKravDokument(List<MottattDokument> mottatteDokumenter) {

        Map<KravDokument, List<OppgittFraværPeriode>> oppgittFraværMedKravdokument = new HashMap<>();
        mottatteDokumenter.forEach(dok -> {
            var kravDokument = new KravDokument(dok.getJournalpostId(), dok.getMottattTidspunkt(), KRAVDOKUMENTTYPE_SØKNAD);
            var søknad = søknadParser.parseSøknad(dok);
            var oppgitteFraværPerioder = new SøknadOppgittFraværMapper(søknad).map();
            oppgittFraværMedKravdokument.put(kravDokument, oppgitteFraværPerioder);
        });

        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søktePerioderMedKravDokument = oppgittFraværMedKravdokument.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()
                .stream()
                .map(op -> new SøktPeriode<>(op.getPeriode(), op.getAktivitetType(), op.getArbeidsgiver(), op.getArbeidsforholdRef(), op))
                .collect(Collectors.toList())));

        return søktePerioderMedKravDokument;
    }
}
