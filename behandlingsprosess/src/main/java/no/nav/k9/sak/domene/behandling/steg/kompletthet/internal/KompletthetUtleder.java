package no.nav.k9.sak.domene.behandling.steg.kompletthet.internal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetsAksjon;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;

class KompletthetUtleder {

    private static final Logger LOGGER = LoggerFactory.getLogger(KompletthetUtleder.class);

    KompletthetsAksjon utled(VurdererInput input) {
        Objects.requireNonNull(input);

        var relevanteKompletthetsvurderinger = utledRelevanteVurderinger(input);

        if (relevanteKompletthetsvurderinger.isEmpty()) {
            log("Ingen relevante kompletthetsvurderinger", input.getUtvidetLogging());
            return KompletthetsAksjon.fortsett();
        }

        var erKomplett = relevanteKompletthetsvurderinger.entrySet()
            .stream()
            .allMatch(it -> it.getValue().isEmpty());

        if (erKomplett) {
            log("Ingen manglende vedlegg for noen av de vurderte periodene. Periodene til vurdering var: " + relevanteKompletthetsvurderinger.keySet(), input.getUtvidetLogging());
            return KompletthetsAksjon.fortsett();
        }

        var kanFortsette = utledRelevanteVurdering(relevanteKompletthetsvurderinger, input)
            .entrySet()
            .stream()
            .allMatch(it -> it.getValue().isEmpty());

        kanFortsette = kanFortsette && !(input.erManueltOpprettetRevurdering() && input.harIkkeFåttMulighetTilÅTaStillingPåNytt());

        if (kanFortsette) {
            log("Komplett etter utvidet sjekk", input.getUtvidetLogging());
            log("harIkkeFåttMulighetTilÅTaStillingPåNytt: " + input.harIkkeFåttMulighetTilÅTaStillingPåNytt(), input.getUtvidetLogging());
            log("erManueltOpprettetRevurdering: " + input.erManueltOpprettetRevurdering(), input.getUtvidetLogging());
            return KompletthetsAksjon.fortsett();
        }

        return KompletthetsAksjon.uavklart();
    }

    Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledRelevanteVurderinger(VurdererInput input) {
        var perioderTilVurdering = input.getPerioderTilVurdering();
        var perioderTilVurderingMedSøknadsfristOppfylt = input.getPerioderTilVurderingMedSøknadsfristOppfylt();

        var kompletthetsVurderinger = input.getManglendeVedleggPerPeriode();
        return kompletthetsVurderinger.entrySet()
            .stream()
            .filter(it -> perioderTilVurdering.contains(it.getKey()) && perioderTilVurderingMedSøknadsfristOppfylt.stream().anyMatch(at -> at.overlapper(it.getKey())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<DatoIntervallEntitet, List<ManglendeVedlegg>> utledRelevanteVurdering(Map<DatoIntervallEntitet, List<ManglendeVedlegg>> relevanteKompletthetsvurderinger,
                                                                                      VurdererInput input) {

        var kompletthetPerioder = input.getKompletthetsPerioder();
        var vurderingStatuserSomTilsierKomplett = input.getVurderingDetSkalTasHensynTil();

        return relevanteKompletthetsvurderinger.entrySet()
            .stream()
            .filter(it -> erFortsattAnsettSomIkkeKomplett(kompletthetPerioder, it, vurderingStatuserSomTilsierKomplett, input.getUtvidetLogging()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean erFortsattAnsettSomIkkeKomplett(List<KompletthetPeriode> kompletthetPerioder,
                                                    Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it,
                                                    Set<Vurdering> vurderingStatuserSomTilsierKomplett, boolean utvidetLogging) {

        if (vurderingStatuserSomTilsierKomplett.isEmpty() || kompletthetPerioder.isEmpty()) {
            return true;
        }

        var harIkkeBlittTattStillingTilFør = harIkkeBlittTattStillingTilFør(kompletthetPerioder, it);
        var harBlittTattStillingTilOgHarRelevantVurdering = harBlittTattStillingTilOgHarRelevantVurdering(kompletthetPerioder, it, vurderingStatuserSomTilsierKomplett);
        log("harIkkeBlittTattStillingTilFør : " + harIkkeBlittTattStillingTilFør, utvidetLogging);
        log("harBlittTattStillingTilOgHarRelevantVurdering : " + harBlittTattStillingTilOgHarRelevantVurdering, utvidetLogging);
        return harIkkeBlittTattStillingTilFør
            || harBlittTattStillingTilOgHarRelevantVurdering;
    }

    private boolean harBlittTattStillingTilOgHarRelevantVurdering(List<KompletthetPeriode> kompletthetPerioder, Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it, Set<Vurdering> vurderingStatuserSomTilsierKomplett) {
        return kompletthetPerioder.stream().anyMatch(at -> Objects.equals(at.getSkjæringstidspunkt(), it.getKey().getFomDato())
            && !vurderingStatuserSomTilsierKomplett.contains(at.getVurdering()));
    }

    private boolean harIkkeBlittTattStillingTilFør(List<KompletthetPeriode> kompletthetPerioder, Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it) {
        return kompletthetPerioder.stream().noneMatch(at -> Objects.equals(at.getSkjæringstidspunkt(), it.getKey().getFomDato()));
    }

    private static void log(String message, boolean utvidetLogging) {
        if (utvidetLogging) {
            LOGGER.info(message);
        }
    }
}
