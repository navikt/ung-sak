package no.nav.ung.sak.behandlingslager.behandling.sporing;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.typer.Periode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LagRegelSporing {

    public static String lagRegelSporingFraTidslinjer(Map<String, LocalDateTimeline<?>> tidslinjer) {
        final var resultatMap = new HashMap<String, List<Periode>>();
        for (Map.Entry<String, LocalDateTimeline<?>> entry : tidslinjer.entrySet()) {
            final var segmenter = entry.getValue().toSegments();
            for (var segment : segmenter) {
                final List<Periode> verdier = resultatMap.getOrDefault(entry.getKey(), new ArrayList<>());
                if (segment.getValue() instanceof IngenVerdi) {
                    verdier.add(new Periode(segment.getFom(), segment.getTom()));
                } else {
                    final var verdi = JsonObjectMapper.toJson(segment.getValue(), JsonMappingFeil.FACTORY::jsonMappingFeil);
                    verdier.add(new PeriodeMedVerdi(segment.getFom(), segment.getTom(), verdi));
                }
                resultatMap.put(entry.getKey(), verdier);
            }
        }
        return JsonObjectMapper.toJson(resultatMap, JsonMappingFeil.FACTORY::jsonMappingFeil);
    }

    public interface JsonMappingFeil extends DeklarerteFeil {

        JsonMappingFeil FACTORY = FeilFactory.create(JsonMappingFeil.class);

        @TekniskFeil(feilkode = "UNG-34523", feilmelding = "JSON-mapping feil: %s", logLevel = LogLevel.WARN)
        Feil jsonMappingFeil(JsonProcessingException e);
    }


}
