package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@Dependent
public class SøknadPerioderTjeneste {

    private static final List<Brevkode> BREVKODER_SØKNAD = List.of(Brevkode.SØKNAD_UTBETALING_OMS, Brevkode.SØKNAD_UTBETALING_OMS_AT);
    private static final List<Brevkode> BREVKODER_IM_KORRIGERING = List.of(Brevkode.FRAVÆRSKORRIGERING_IM_OMS);

    private MottatteDokumentRepository mottatteDokumentRepository;
    private OmsorgspengerGrunnlagRepository grunnlagRepository;


    @Inject
    public SøknadPerioderTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                  OmsorgspengerGrunnlagRepository grunnlagRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.grunnlagRepository = grunnlagRepository;
    }

    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> hentSøktePerioderMedKravdokumentPåFagsak(BehandlingReferanse ref) {
        var mottatteDokumenter = hentMottatteDokument(ref.getFagsakId());
        return tilSøktePerioderMedKravdokument(mottatteDokumenter);
    }

    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> hentSøktePerioderMedKravdokumentPåBehandling(BehandlingReferanse ref) {
        var mottatteDokumenter = hentMottatteDokument(ref.getFagsakId())
            .stream()
            .filter(dok -> ref.getBehandlingId().equals(dok.getBehandlingId()))
            .collect(Collectors.toList());
        return tilSøktePerioderMedKravdokument(mottatteDokumenter);
    }

    private Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> tilSøktePerioderMedKravdokument(List<MottattDokument> mottatteDokumenter) {
        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søktePerioderMedKravDokument = new HashMap<>();
        for (MottattDokument dok : mottatteDokumenter) {
            if (BREVKODER_SØKNAD.contains(dok.getType())) {
                var kravDokument = new KravDokument(dok.getJournalpostId(), dok.getMottattTidspunkt(), KravDokumentType.SØKNAD);
                var fraværPerioderFraSøknad = grunnlagRepository.hentOppgittFraværFraSøknadHvisEksisterer(dok.getBehandlingId())
                    .map(OppgittFravær::getPerioder)
                    .orElse(Set.of());
                søktePerioderMedKravDokument.put(kravDokument, mapFraværPerioder(fraværPerioderFraSøknad, dok));
            } else if (BREVKODER_IM_KORRIGERING.contains(dok.getType())) {
                var kravDokument = new KravDokument(dok.getJournalpostId(), dok.getMottattTidspunkt(), KravDokumentType.INNTEKTSMELDING_MED_REFUSJONSKRAV);
                var fraværskorrigeringerFraIm = grunnlagRepository.hentOppgittFraværFraFraværskorrigeringerHvisEksisterer(dok.getBehandlingId())
                    .map(OppgittFravær::getPerioder)
                    .orElse(Set.of());
                søktePerioderMedKravDokument.put(kravDokument, mapFraværPerioder(fraværskorrigeringerFraIm, dok));
            } else {
                throw new IllegalArgumentException("Mapping av fraværsperidoder er ikke støttet for brevkode=" + dok.getType());
            }
        }
        return søktePerioderMedKravDokument;
    }

    private List<SøktPeriode<OppgittFraværPeriode>> mapFraværPerioder(Set<OppgittFraværPeriode> fraværPerioder, MottattDokument dok) {
        var søktePerioderPåJp = fraværPerioder.stream()
            .filter(fp -> dok.getJournalpostId().equals(fp.getJournalpostId()))
            .map(fp -> new SøktPeriode<>(fp.getPeriode(), fp.getAktivitetType(), fp.getArbeidsgiver(), fp.getArbeidsforholdRef(), fp))
            .collect(Collectors.toList());
        return søktePerioderPåJp;
    }

    private List<MottattDokument> hentMottatteDokument(long fagsakId) {
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId, DokumentStatus.GYLDIG)
            .stream()
            .filter(dok -> BREVKODER_SØKNAD.contains(dok.getType()) || BREVKODER_IM_KORRIGERING.contains(dok.getType()))
            .filter(dok -> dok.getBehandlingId() != null)
            .collect(Collectors.toList());
    }
}
