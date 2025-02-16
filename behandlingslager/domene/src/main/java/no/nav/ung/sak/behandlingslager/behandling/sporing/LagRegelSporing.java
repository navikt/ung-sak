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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LagRegelSporing {

    public static String lagRegelSporingFraTidslinjer(Map<String, LocalDateTimeline<?>> tidslinjer) {
        final var resultatMap = new HashMap<String, List<PeriodeMedVerdi>>();
        for (Map.Entry<String, LocalDateTimeline<?>> entry : tidslinjer.entrySet()) {
            final var segmenter = entry.getValue().toSegments();
            for (var segment : segmenter) {
                final var verdi = segment.getValue() instanceof IngenVerdi ?
                    "INGEN_VERDI" :
                        JsonObjectMapper.toJson(segment.getValue(), JsonMappingFeil.FACTORY::jsonMappingFeil);
                final var periodeMedVerdi = new PeriodeMedVerdi(DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom()), verdi);
                resultatMap.put(entry.getKey(), List.of(periodeMedVerdi));
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
