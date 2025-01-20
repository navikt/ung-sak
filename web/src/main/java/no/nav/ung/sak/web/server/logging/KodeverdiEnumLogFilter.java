package no.nav.ung.sak.web.server.logging;

import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dette filter filtrerer ikkje, men endre logging av Kodeverdi enums.
 * <p>
 * Ved å bruke dette filter får vi logga Kodeverdi enums på formatet "name(kode)", der name er resultatet av å
 * kalle name() på enum, og kode er resultatet av å kalle getKode(). Viss name og kode har samme verdi blir berre name
 * returnert.
 * <p>
 * Denne er lagt til for å få med namnet på Kodeverdi enum i logg sjølv om ein endre toString() metoden på enum til å berre
 * returnere kode verdien, for at den skal vere kompatibel med openapi spesifikasjon.
 * <p>
 * Viss ein får skrive om alle disse enums til å serialisere til kode string med @JsonValue annotasjon istadenfor å måtte
 * endre toString() kan ein vurdere å fjerne denne igjen, og heller bruke toString() å enum igjen.
 */
public class KodeverdiEnumLogFilter extends Filter<ILoggingEvent> {

    private static <T extends Enum<?> & Kodeverdi> String kodeverdiLogTxt(final T kv) {
        final String kode = kv.getKode();
        final String name = kv.name();
        if(!kode.equals(name)) {
            return "%s(%s)".formatted(kv.name(), kode);
        } else {
            return name;
        }
    }

    private static Object kodeverdiLogTxtEllerOriginal(final Object o) {
        if (o instanceof Enum<?> e && e instanceof Kodeverdi) {
            return kodeverdiLogTxt((Enum<?> & Kodeverdi)e);
        } else {
            return o;
        }
    }

    @Override
    public FilterReply decide(final ILoggingEvent event) {
        if (event.getArgumentArray() == null){
            return FilterReply.NEUTRAL;
        }
        for(int i = 0; i < event.getArgumentArray().length; i++) {
            final Object arg = event.getArgumentArray()[i];
            // Her kan vi avgrense til enkelte klasser viss vi ønsker
            if (arg instanceof Enum<?> e && e instanceof Kodeverdi) {
                event.getArgumentArray()[i] = kodeverdiLogTxt((Enum<?> & Kodeverdi)e);
            } else if (arg instanceof Collection<?>) {
                // Collection verdier som blir logga blir traversert, og viss det er Kodeverdi enums der blir dei også
                // tranformert til ønska tekst for logging
                final var convertedCollection = ((Collection<?>) arg).stream().map(o -> {
                    return kodeverdiLogTxtEllerOriginal(o);
                }).toList();
                event.getArgumentArray()[i] = convertedCollection;
            } else if(arg instanceof Map<?, ?>) {
                // Map nøkler og verdier som blir logga blir traversert, og transformert tilsvarande Collection verdier
                final var convertedMap = ((Map<?, ?>) arg).entrySet().stream()
                    .collect(Collectors.toMap(
                        entry -> kodeverdiLogTxtEllerOriginal(entry.getKey()),
                        entry -> kodeverdiLogTxtEllerOriginal(entry.getValue())
                    ));
                event.getArgumentArray()[i] = convertedMap;
            }
        }
        return FilterReply.NEUTRAL;
    }
}
