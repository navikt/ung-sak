package no.nav.k9.sak.web.app.tjenester.behandling.kompletthet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetForBeregningTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.kompletthet.ArbeidsgiverArbeidsforholdIdV2;
import no.nav.k9.sak.kontrakt.kompletthet.inntektsmelding.InntektsmeldingVurdering;
import no.nav.k9.sak.kontrakt.kompletthet.inntektsmelding.Vurdering;
import no.nav.k9.sak.kontrakt.kompletthet.inntektsmelding.VurderingPerPeriode;
import no.nav.k9.sak.kontrakt.kompletthet.inntektsmelding.VurderingPåPeriode;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;

@Dependent
class InntektsmeldingVurderingUtleder {

    private final KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private final Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    @Inject
    InntektsmeldingVurderingUtleder(KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste,
                                    @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    VurderingPerPeriode utled(BehandlingReferanse ref) {
        var alleInntektsmeldinger = kompletthetForBeregningTjeneste.hentAlleUnikeInntektsmeldingerForFagsak(ref.getSaksnummer());


        var tjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());
        var perioderTilVurdering = tjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        var inntektsmeldingerUtenDato = alleInntektsmeldinger.stream()
            .filter(im -> im.getStartDatoPermisjon().isEmpty())
            .collect(Collectors.toSet());

        var resultat = new ArrayList<VurderingPåPeriode>();

        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            var relevantPeriode = kompletthetForBeregningTjeneste.utledRelevantPeriode(ref, periode);
            var aktuelleInntektsmeldinger = kompletthetForBeregningTjeneste.utledRelevanteInntektsmeldinger(alleInntektsmeldinger, relevantPeriode);
            var inntektsmeldingerSomSendesInnTilBeregning = kompletthetForBeregningTjeneste.utledInntektsmeldingerSomSendesInnTilBeregningForPeriode(ref, alleInntektsmeldinger, periode);

            var utdaterteInntektsmeldinger = aktuelleInntektsmeldinger.stream()
                .filter(im -> finnesIkkeI(inntektsmeldingerSomSendesInnTilBeregning.stream(), im))
                .collect(Collectors.toSet());

            var ikkeRelevanteInntektsmeldinger = alleInntektsmeldinger.stream()
                .filter(im -> im.getStartDatoPermisjon().isPresent() && finnesIkkeI(inntektsmeldingerSomSendesInnTilBeregning.stream(), im) && finnesIkkeI(utdaterteInntektsmeldinger.stream(), im))
                .collect(Collectors.toSet());

            var vurderinger = new ArrayList<>(inntektsmeldingerSomSendesInnTilBeregning.stream().map(it -> mapInntektsmelding(it, Vurdering.I_BRUK)).toList());
            vurderinger.addAll(utdaterteInntektsmeldinger.stream().map(it -> mapInntektsmelding(it, Vurdering.ERSTATTET_AV_NYERE, utledVurderingerSomErstatter(vurderinger, it))).toList());
            vurderinger.addAll(ikkeRelevanteInntektsmeldinger.stream().map(it -> mapInntektsmelding(it, Vurdering.IKKE_RELEVANT)).toList());
            vurderinger.addAll(inntektsmeldingerUtenDato.stream().map(it -> mapInntektsmelding(it, Vurdering.MANGLER_DATO)).toList());

            resultat.add(new VurderingPåPeriode(periode.tilPeriode(), vurderinger));
        }

        return new VurderingPerPeriode(resultat);
    }

    private List<JournalpostId> utledVurderingerSomErstatter(List<InntektsmeldingVurdering> vurderinger, Inntektsmelding im) {
        return vurderinger.stream()
            .filter(at -> matcherArbeidsforhold(mapArbeidsforhold(im), at.getArbeidsgiver()))
            .map(InntektsmeldingVurdering::getJournalpostId)
            .collect(Collectors.toList());
    }

    private boolean matcherArbeidsforhold(ArbeidsgiverArbeidsforholdIdV2 arb1, ArbeidsgiverArbeidsforholdIdV2 arb2) {
        if (!Objects.equals(arb1.getArbeidsgiver(), arb2.getArbeidsgiver())) {
            return false;
        }
        return matcherArbeidsforholdId(arb1.getArbeidsforhold(), arb2.getArbeidsforhold());
    }

    private boolean matcherArbeidsforholdId(String arbeidsforhold, String arbeidsforhold1) {
        return (arbeidsforhold == null || arbeidsforhold1 == null) || Objects.equals(arbeidsforhold, arbeidsforhold1);
    }

    private InntektsmeldingVurdering mapInntektsmelding(Inntektsmelding im, Vurdering vurdering) {
        return new InntektsmeldingVurdering(mapArbeidsforhold(im), vurdering, im.getJournalpostId(), im.getStartDatoPermisjon().orElse(null), im.getInnsendingstidspunkt(), im.getKanalreferanse(), List.of());
    }

    private InntektsmeldingVurdering mapInntektsmelding(Inntektsmelding im, Vurdering vurdering, List<JournalpostId> erstattetAv) {
        return new InntektsmeldingVurdering(mapArbeidsforhold(im), vurdering, im.getJournalpostId(), im.getStartDatoPermisjon().orElse(null), im.getInnsendingstidspunkt(), im.getKanalreferanse(), erstattetAv);
    }

    private ArbeidsgiverArbeidsforholdIdV2 mapArbeidsforhold(Inntektsmelding im) {
        return new ArbeidsgiverArbeidsforholdIdV2(im.getArbeidsgiver(),
            im.getEksternArbeidsforholdRef().map(EksternArbeidsforholdRef::getReferanse).orElse(null));
    }

    private boolean finnesIkkeI(Stream<Inntektsmelding> inntektsmeldingerSomSendesInnTilBeregning, Inntektsmelding im) {
        return inntektsmeldingerSomSendesInnTilBeregning.noneMatch(it -> Objects.equals(im.getJournalpostId(), it.getJournalpostId()));
    }
}
