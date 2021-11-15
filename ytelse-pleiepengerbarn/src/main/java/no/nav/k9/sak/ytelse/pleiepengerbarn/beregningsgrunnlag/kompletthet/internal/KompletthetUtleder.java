package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.internal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.KompletthetsAksjon;

class KompletthetUtleder {

    KompletthetsAksjon utled(VurdererInput input) {
        Objects.requireNonNull(input);

        var relevanteKompletthetsvurderinger = utledRelevanteVurderinger(input);

        if (relevanteKompletthetsvurderinger.isEmpty()) {
            return KompletthetsAksjon.fortsett();
        }

        var erKomplett = relevanteKompletthetsvurderinger.entrySet()
            .stream()
            .allMatch(it -> it.getValue().isEmpty());

        if (erKomplett) {
            return KompletthetsAksjon.fortsett();
        }

        var kanFortsette = utledRelevanteVurdering(relevanteKompletthetsvurderinger, input)
            .entrySet()
            .stream()
            .allMatch(it -> it.getValue().isEmpty());

        if (kanFortsette) {
            return KompletthetsAksjon.fortsett();
        }

        return KompletthetsAksjon.uavklart();
    }

    Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledRelevanteVurderinger(VurdererInput input) {
        var perioderTilVurdering = input.getPerioderTilVurdering();

        var kompletthetsVurderinger = input.getManglendeVedleggPerPeriode();
        return kompletthetsVurderinger.entrySet()
            .stream()
            .filter(it -> perioderTilVurdering.contains(it.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledRelevanteVurdering(Map<DatoIntervallEntitet, List<ManglendeVedlegg>> relevanteKompletthetsvurderinger,
                                                                                      VurdererInput input) {

        var kompletthetPerioder = input.getKompletthetsPerioder();
        var vurderingStatuserSomTilsierKomplett = input.getVurderingDetSkalTasHensynTil();

        return relevanteKompletthetsvurderinger.entrySet()
            .stream()
            .filter(it -> erFortsattAnsettSomIkkeKomplett(kompletthetPerioder, it, vurderingStatuserSomTilsierKomplett))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean erFortsattAnsettSomIkkeKomplett(List<KompletthetPeriode> kompletthetPerioder,
                                                    Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it,
                                                    Set<Vurdering> vurderingstyperDetSkalTasHensynTil) {

        if (vurderingstyperDetSkalTasHensynTil.isEmpty() || kompletthetPerioder.isEmpty()) {
            return true;
        }

        return harIkkeBlittTattStillingTilFør(kompletthetPerioder, it)
            || harBlittTattStillingTilOgHarRelevantVurdering(kompletthetPerioder, it, vurderingstyperDetSkalTasHensynTil);
    }

    private boolean harBlittTattStillingTilOgHarRelevantVurdering(List<KompletthetPeriode> kompletthetPerioder, Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it, Set<Vurdering> vurderingstyperDetSkalTasHensynTil) {
        return kompletthetPerioder.stream().anyMatch(at -> Objects.equals(at.getSkjæringstidspunkt(), it.getKey().getFomDato())
            && !vurderingstyperDetSkalTasHensynTil.contains(at.getVurdering()));
    }

    private boolean harIkkeBlittTattStillingTilFør(List<KompletthetPeriode> kompletthetPerioder, Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it) {
        return kompletthetPerioder.stream().noneMatch(at -> Objects.equals(at.getSkjæringstidspunkt(), it.getKey().getFomDato()));
    }
}
