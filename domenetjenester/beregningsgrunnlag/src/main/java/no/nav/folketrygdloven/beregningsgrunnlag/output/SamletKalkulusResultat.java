package no.nav.folketrygdloven.beregningsgrunnlag.output;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class SamletKalkulusResultat {
    private final Map<UUID, KalkulusResultat> resultater;
    private final Map<UUID, LocalDate> skjæringstidspunkter;

    public SamletKalkulusResultat(Map<UUID, KalkulusResultat> resultater, Map<UUID, LocalDate> skjæringstidspunkter) {

        this.skjæringstidspunkter = Collections.unmodifiableMap(skjæringstidspunkter);
        this.resultater = Collections.unmodifiableMap(resultater);

        if (!skjæringstidspunkter.keySet().containsAll(resultater.keySet())) {
            // har færre skjæringstidspunkter enn resultater - ikke bra. Kan ha færre resultater enn skjæringstidspunkter - Ok
            throw new IllegalArgumentException("Mismatch skjæringstidspunkt, resultater: " + skjæringstidspunkter.keySet() + " vs. " + resultater.keySet());
        }
    }

    public Map<UUID, KalkulusResultat> getResultater() {
        return Collections.unmodifiableMap(resultater);
    }

    public Map<UUID, LocalDate> getSkjæringstidspunkter() {
        return Collections.unmodifiableMap(skjæringstidspunkter);
    }
}
