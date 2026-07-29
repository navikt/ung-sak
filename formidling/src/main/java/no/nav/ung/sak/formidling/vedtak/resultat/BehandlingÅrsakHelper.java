package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class BehandlingÅrsakHelper {

    private final Set<BehandlingÅrsakType> årsaker;

    private BehandlingÅrsakHelper(LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        this.årsaker = detaljertResultat.stream()
            .flatMap(it -> it.getValue().behandlingsårsaker().stream())
            .collect(Collectors.toSet());
    }

    public static BehandlingÅrsakHelper of(LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return new BehandlingÅrsakHelper(detaljertResultat);
    }

    public boolean har(BehandlingÅrsakType årsak) {
        return årsaker.contains(årsak);
    }

    public boolean harNoen(BehandlingÅrsakType... årsaker) {
        return Arrays.stream(årsaker).anyMatch(this.årsaker::contains);
    }
}

