package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.perioder.KravDokument;
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

    OMPVurderSøknadsfristTjeneste() {
    }

    @Inject
    public OMPVurderSøknadsfristTjeneste(InntektsmeldingerPerioderTjeneste inntektsmeldingerPerioderTjeneste,
                                         InntektsmeldingSøktePerioderMapper inntektsmeldingMapper,
                                         SøknadPerioderTjeneste søknadPerioderTjeneste,
                                         VurderSøknadsfrist vurderSøknadsfrist) {
        this.inntektsmeldingerPerioderTjeneste = inntektsmeldingerPerioderTjeneste;
        this.inntektsmeldingMapper = inntektsmeldingMapper;
        this.søknadPerioderTjeneste = søknadPerioderTjeneste;
        this.vurderSøknadsfrist = vurderSøknadsfrist;
    }

    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> vurderSøknadsfrist(BehandlingReferanse referanse) {
        var perioderTilVurdering = hentPerioderTilVurdering(referanse);

        return vurderSøknadsfrist(perioderTilVurdering);
    }

    @Override
    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> hentPerioderTilVurdering(BehandlingReferanse referanse) {
        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søktePerioder = new HashMap<>();

        var inntektsmeldinger = inntektsmeldingerPerioderTjeneste.hentUtInntektsmeldingerRelevantForBehandling(referanse);
        søktePerioder.putAll(inntektsmeldingMapper.mapTilSøktePerioder(inntektsmeldinger));
        var søktePerioderFraSøknad = søknadPerioderTjeneste.hentSøktePerioderMedKravdokumentPåFagsak(referanse);
        søktePerioder.putAll(søktePerioderFraSøknad);

        return søktePerioder;
    }

    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> vurderSøknadsfrist(Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søknaderMedPerioder) {
        return vurderSøknadsfrist.vurderSøknadsfrist(søknaderMedPerioder);
    }

}
