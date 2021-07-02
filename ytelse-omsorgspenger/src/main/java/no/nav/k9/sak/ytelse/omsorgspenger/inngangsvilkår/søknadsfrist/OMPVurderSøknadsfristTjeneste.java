package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristRepository;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OMPVurderSøknadsfristTjeneste implements VurderSøknadsfristTjeneste<OppgittFraværPeriode> {

    private InntektsmeldingerPerioderTjeneste inntektsmeldingerPerioderTjeneste;
    private InntektsmeldingSøktePerioderMapper inntektsmeldingMapper;
    private SøknadPerioderTjeneste søknadPerioderTjeneste;
    private VurderSøknadsfrist vurderSøknadsfrist;
    private AvklartSøknadsfristRepository avklartSøknadsfristRepository;

    OMPVurderSøknadsfristTjeneste() {
    }

    @Inject
    public OMPVurderSøknadsfristTjeneste(InntektsmeldingerPerioderTjeneste inntektsmeldingerPerioderTjeneste,
                                         InntektsmeldingSøktePerioderMapper inntektsmeldingMapper,
                                         SøknadPerioderTjeneste søknadPerioderTjeneste,
                                         VurderSøknadsfrist vurderSøknadsfrist,
                                         AvklartSøknadsfristRepository avklartSøknadsfristRepository) {
        this.inntektsmeldingerPerioderTjeneste = inntektsmeldingerPerioderTjeneste;
        this.inntektsmeldingMapper = inntektsmeldingMapper;
        this.søknadPerioderTjeneste = søknadPerioderTjeneste;
        this.vurderSøknadsfrist = vurderSøknadsfrist;
        this.avklartSøknadsfristRepository = avklartSøknadsfristRepository;
    }

    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> vurderSøknadsfrist(BehandlingReferanse referanse) {
        var perioderTilVurdering = hentPerioderTilVurdering(referanse);

        return vurderSøknadsfrist(referanse.getBehandlingId(), perioderTilVurdering);
    }

    @Override
    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> hentPerioderTilVurdering(BehandlingReferanse referanse) {

        var inntektsmeldinger = inntektsmeldingerPerioderTjeneste.hentUtInntektsmeldingerRelevantForBehandling(referanse);
        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søktePerioder = new HashMap<>(inntektsmeldingMapper.mapTilSøktePerioder(inntektsmeldinger));
        var søktePerioderFraSøknad = søknadPerioderTjeneste.hentSøktePerioderMedKravdokumentPåFagsak(referanse);
        søktePerioder.putAll(søktePerioderFraSøknad);

        return søktePerioder;
    }

    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> vurderSøknadsfrist(Long behandlingId, Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søknaderMedPerioder) {
        var avklartSøknadsfristResultat = avklartSøknadsfristRepository.hentHvisEksisterer(behandlingId);
        return vurderSøknadsfrist.vurderSøknadsfrist(søknaderMedPerioder, avklartSøknadsfristResultat);
    }

    @Override
    public Set<KravDokument> relevanteKravdokumentForBehandling(BehandlingReferanse referanse) {
        var inntektsmeldinger = inntektsmeldingerPerioderTjeneste.hentUtInntektsmeldingerRelevantForBehandling(referanse);
        var kravDokumenter = inntektsmeldinger.stream()
            .map(it -> new KravDokument(it.getJournalpostId(), it.getInnsendingstidspunkt(), KravDokumentType.INNTEKTSMELDING))
            .collect(Collectors.toCollection(HashSet::new));

        var søktePerioderFraSøknad = søknadPerioderTjeneste.hentSøktePerioderMedKravdokumentPåBehandling(referanse);
        kravDokumenter.addAll(søktePerioderFraSøknad.keySet());

        return kravDokumenter;
    }

}
