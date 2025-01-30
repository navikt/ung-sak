package no.nav.ung.sak.domene.behandling.steg.uttak;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.AvslagIkkeNokDagerVurderer;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.AvslagVedDødVurderer;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.InnvilgetUttakVurderer;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakDelResultat;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.FinnForbrukteDager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;
import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

class VurderUttakTjeneste {


    static Optional<UngdomsytelseUttakPerioder> vurderUttak(LocalDateTimeline<Boolean> godkjentePerioder,
                                                            LocalDateTimeline<Boolean> ungdomsprogramtidslinje,
                                                            Optional<LocalDate> søkersDødsdato) {
        if (godkjentePerioder.isEmpty()) {
            return Optional.empty();
        }

        var levendeBrukerTidslinje = søkersDødsdato.map(d -> new LocalDateTimeline<>(TIDENES_BEGYNNELSE, d, true)).orElse(new LocalDateTimeline<>(TIDENES_BEGYNNELSE, TIDENES_ENDE, true));


        // Finner tidslinje med nok dager tilgjengelig
        List<UttakDelResultat> delresultater = new ArrayList<>();
        final var nokDagerDelresultat = new InnvilgetUttakVurderer(godkjentePerioder, ungdomsprogramtidslinje, levendeBrukerTidslinje).vurder();
        delresultater.add(nokDagerDelresultat);
        final var uttakAvslagEtterSøkersDødDelResultat = new AvslagVedDødVurderer(nokDagerDelresultat.restTidslinjeTilVurdering(), levendeBrukerTidslinje).vurder();
        delresultater.add(uttakAvslagEtterSøkersDødDelResultat);
        final var ikkeNokDagerPeriodeDelResultat = new AvslagIkkeNokDagerVurderer(uttakAvslagEtterSøkersDødDelResultat.restTidslinjeTilVurdering()).vurder();
        delresultater.add(ikkeNokDagerPeriodeDelResultat);

        final var uttakPerioder = delresultater.stream().map(UttakDelResultat::resultatPerioder)
                .flatMap(List::stream)
                .toList();

        var ungdomsytelseUttakPerioder = new UngdomsytelseUttakPerioder(uttakPerioder);
        ungdomsytelseUttakPerioder.setRegelInput(lagRegelInput(godkjentePerioder, ungdomsprogramtidslinje, søkersDødsdato));
        ungdomsytelseUttakPerioder.setRegelSporing(lagSporing(delresultater));
        return Optional.of(ungdomsytelseUttakPerioder);
    }

    private static String lagSporing(List<UttakDelResultat> delresultater) {
        return delresultater.stream().map(UttakDelResultat::regelSporing)
                .reduce((m1, m2) -> {
                    final var newMap = new HashMap<>(m2);
                    newMap.putAll(m1);
                    return newMap;
                })
                .map(it -> JsonObjectMapper.toJson(it, JsonMappingFeil.FACTORY::jsonMappingFeil))
                .orElse("");
    }

    private static String lagRegelInput(LocalDateTimeline<Boolean> godkjentePerioder, LocalDateTimeline<Boolean> ungdomsprogramtidslinje, Optional<LocalDate> søkersDødsdato) {
        return """
                {
                    "godkjentePerioder": ":godkjentePerioder",
                    "ungdomsprogramtidslinje": ":ungdomsprogramtidslinje",
                    "søkersDødsdato": :søkersDødsdato
                }
                """.stripLeading()
                .replaceFirst(":godkjentePerioder", godkjentePerioder.getLocalDateIntervals().toString())
                .replaceFirst(":ungdomsprogramtidslinje", ungdomsprogramtidslinje.getLocalDateIntervals().toString())
                .replaceFirst(":søkersDødsdato", søkersDødsdato.map(Objects::toString).map(it -> "\"" + it + "\"").orElse("null"));
    }

    interface JsonMappingFeil extends DeklarerteFeil {

        JsonMappingFeil FACTORY = FeilFactory.create(JsonMappingFeil.class);

        @TekniskFeil(feilkode = "UNG-34524", feilmelding = "JSON-mapping feil: %s", logLevel = LogLevel.WARN)
        Feil jsonMappingFeil(JsonProcessingException e);
    }

}
