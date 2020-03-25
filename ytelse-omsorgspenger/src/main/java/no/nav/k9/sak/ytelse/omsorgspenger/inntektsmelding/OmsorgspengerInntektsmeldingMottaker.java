package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingInnhold;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingMottaker;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@FagsakYtelseTypeRef("OMP")
@BehandlingTypeRef
@ApplicationScoped
public class OmsorgspengerInntektsmeldingMottaker implements InntektsmeldingMottaker {

    private OmsorgspengerGrunnlagRepository grunnlagRepository;

    public OmsorgspengerInntektsmeldingMottaker() {
        // for proxy
    }

    @Inject
    public OmsorgspengerInntektsmeldingMottaker(OmsorgspengerGrunnlagRepository grunnlagRepository) {
        this.grunnlagRepository = grunnlagRepository;
    }

    @Override
    public void mottattInntektsmelding(BehandlingReferanse ref, List<InntektsmeldingInnhold> inntektsmeldinger) {
        Long behandlingId = ref.getBehandlingId();
        List<OppgittFraværPeriode> perioder = trekkUtAlleFraværOgValiderOverlapp(inntektsmeldinger);
        grunnlagRepository.lagreOgFlushOppgittFravær(behandlingId, new OppgittFravær(perioder));
    }

    private List<OppgittFraværPeriode> trekkUtAlleFraværOgValiderOverlapp(List<InntektsmeldingInnhold> inntektsmeldinger) {
        var aktivitetType = UttakArbeidType.ARBEIDSTAKER;
        List<OppgittFraværPeriode> alle = new ArrayList<>();
        Map<Object, List<OppgittFraværPeriode>> mapByAktivitet = new LinkedHashMap<>();
        for (var im : inntektsmeldinger) {
            var arbeidsgiver = im.getInntektsmelding().getArbeidsgiver();
            var arbeidsforholdRef = im.getInntektsmelding().getArbeidsforholdRef();
            var dummyGruppe = Arrays.asList(aktivitetType, arbeidsgiver, arbeidsforholdRef);

            var liste = im.getOmsorgspengerFravær().stream()
                .map(pa -> new OppgittFraværPeriode(pa.getFom(), pa.getTom(), aktivitetType, arbeidsgiver, arbeidsforholdRef, pa.getVarighetPerDag()))
                .collect(Collectors.toList());
            mapByAktivitet.computeIfAbsent(dummyGruppe, k -> new ArrayList<>()).addAll(liste);
            alle.addAll(liste);
        }

        // sjekker mot overlappende data - krasj and burn hvis overlappende segmenter
        for (var entry : mapByAktivitet.entrySet()) {
            var segments = entry.getValue().stream().map(ofp -> new LocalDateSegment<>(ofp.getFom(), ofp.getTom(), ofp)).collect(Collectors.toList());
            new LocalDateTimeline<>(segments);
        }
        return alle;
    }

}
