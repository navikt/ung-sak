package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.utenlandsopphold;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UtenlandsoppholdPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.UtenlandsoppholdInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.UtenlandsoppholdÅrsak;

import java.util.*;
import java.util.stream.Collectors;

public class MapUtenlandsopphold {

    public static Map<LukketPeriode, UtenlandsoppholdInfo> map(Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravDokumenter,
                                                        Set<PerioderFraSøknad> perioderFraSøknader,
                                                        LocalDateTimeline<Boolean> tidslinjeTilVurdering) {

        var kravDokumenterSorted = kravDokumenter.keySet().stream().sorted(KravDokument::compareTo).collect(Collectors.toCollection(LinkedHashSet::new));
        var resultatTimeline = new LocalDateTimeline<UtenlandsoppholdInfo>(List.of());
        for (KravDokument kravDokument : kravDokumenterSorted) {
            var dokumenter = perioderFraSøknader.stream()
                .filter(it -> it.getJournalpostId().equals(kravDokument.getJournalpostId()))
                .collect(Collectors.toSet());
            if (dokumenter.size() == 1) {
                var perioderFraSøknad = dokumenter.iterator().next();
                for (UtenlandsoppholdPeriode utenlandsoppholdPeriode : perioderFraSøknad.getUtenlandsopphold()) {
                    var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(
                        utenlandsoppholdPeriode.getPeriode().getFomDato(),
                        utenlandsoppholdPeriode.getPeriode().getTomDato(),
                        new UtenlandsoppholdInfo(map(utenlandsoppholdPeriode.getÅrsak()), utenlandsoppholdPeriode.getLand().getKode())
                    )));
                    if (utenlandsoppholdPeriode.isAktiv()) {
                        resultatTimeline = resultatTimeline.combine(timeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                    } else {
                        resultatTimeline = resultatTimeline.disjoint(timeline);
                    }
                }
            } else {
                throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + dokumenter);
            }
        }

        var utenlandsperioder = new HashMap<LukketPeriode, UtenlandsoppholdInfo>();
        resultatTimeline.compress()
            .intersection(tidslinjeTilVurdering)
            .toSegments()
            .forEach(it -> utenlandsperioder.put(new LukketPeriode(it.getFom(), it.getTom()), it.getValue()));
        return utenlandsperioder;
    }

    private static UtenlandsoppholdÅrsak map(no.nav.k9.kodeverk.uttak.UtenlandsoppholdÅrsak utenlandsoppholdÅrsak) {
        return switch(utenlandsoppholdÅrsak) {
            case BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING -> UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING;
            case BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD -> UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD;
            case INGEN -> UtenlandsoppholdÅrsak.INGEN;
        };
    }


}
