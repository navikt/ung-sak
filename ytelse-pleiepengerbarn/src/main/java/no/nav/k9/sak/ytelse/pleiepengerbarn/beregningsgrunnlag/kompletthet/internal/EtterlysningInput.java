package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.internal;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.PeriodeMedMangler;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.TidligereEtterlysning;

class EtterlysningInput {

    private Map<AksjonspunktDefinisjon, LocalDateTime> aksjonspunkter;
    private Map<DatoIntervallEntitet, List<ManglendeVedlegg>> relevanteMangler;
    private Map<DatoIntervallEntitet, List<TidligereEtterlysning>> etterlysningerBestilt;

    EtterlysningInput(Map<AksjonspunktDefinisjon, LocalDateTime> aksjonspunkter,
                      Map<DatoIntervallEntitet, List<ManglendeVedlegg>> relevanteMangler,
                      Map<DatoIntervallEntitet, List<TidligereEtterlysning>> etterlysningerBestilt) {

        this.aksjonspunkter = Objects.requireNonNull(aksjonspunkter);
        this.relevanteMangler = Objects.requireNonNull(relevanteMangler);
        this.etterlysningerBestilt = etterlysningerBestilt;
    }

    Map<AksjonspunktDefinisjon, LocalDateTime> getAksjonspunkter() {
        return aksjonspunkter;
    }

    List<PeriodeMedMangler> getRelevanteMangler() {
        return relevanteMangler.entrySet()
            .stream()
            .map(it -> new PeriodeMedMangler(it.getKey(), it.getValue()))
            .collect(Collectors.toList());
    }

    /**
     * Filtrere mot tidligere bestillinger
     *
     * @param relevantBrevType brevtype
     * @return bestillinger som mangler utsendelse
     */
    List<PeriodeMedMangler> getRelevanteFiltrerteMangler(DokumentMalType relevantBrevType) {
        if (etterlysningerBestilt == null || etterlysningerBestilt.isEmpty()) {
            return getRelevanteMangler();
        }
        return relevanteMangler.entrySet().stream().map(it -> filtrerBort(it, relevantBrevType)).collect(Collectors.toList());
    }

    private PeriodeMedMangler filtrerBort(Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> entry, DokumentMalType relevantBrevType) {
        var relevanteBrevbestillinger = etterlysningerBestilt.entrySet()
            .stream()
            .filter(it -> it.getKey().overlapper(entry.getKey()))
            .collect(Collectors.toList());

        var manglerFortsattIkkeEtterlyst = entry.getValue()
            .stream()
            .filter(it -> relevanteBrevbestillinger.stream().map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .noneMatch(bt -> Objects.equals(bt.getDokumentType(), relevantBrevType) && Objects.equals(it.getArbeidsgiver(), bt.getArbeidsgiver())))
            .collect(Collectors.toList());

        return new PeriodeMedMangler(entry.getKey(), manglerFortsattIkkeEtterlyst);
    }
}
