package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.utenlandsopphold;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UtenlandsoppholdTidslinjeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.delt.UtledetUtenlandsopphold;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.UtenlandsoppholdInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.UtenlandsoppholdÅrsak;

public class MapUtenlandsopphold {

    public static Map<LukketPeriode, UtenlandsoppholdInfo> map(Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravDokumenter,
                                                        Set<PerioderFraSøknad> perioderFraSøknader,
                                                        LocalDateTimeline<Boolean> tidslinjeTilVurdering) {

        LocalDateTimeline<UtledetUtenlandsopphold> utenlandsoppholdTidslinje = UtenlandsoppholdTidslinjeTjeneste.byggTidslinje(kravDokumenter, perioderFraSøknader);
        LocalDateTimeline<UtenlandsoppholdInfo> resultatTimeline = utenlandsoppholdTidslinje.mapValue(v -> new UtenlandsoppholdInfo(mapÅrsak(v.getÅrsak()), v.getLandkode().getKode()));

        var utenlandsperioder = new HashMap<LukketPeriode, UtenlandsoppholdInfo>();
        resultatTimeline.compress()
            .intersection(tidslinjeTilVurdering)
            .forEach(it -> utenlandsperioder.put(new LukketPeriode(it.getFom(), it.getTom()), it.getValue()));
        return utenlandsperioder;
    }

    private static UtenlandsoppholdÅrsak mapÅrsak(no.nav.k9.kodeverk.uttak.UtenlandsoppholdÅrsak utenlandsoppholdÅrsak) {
        return switch(utenlandsoppholdÅrsak) {
            case BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING -> UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING;
            case BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD -> UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD;
            case INGEN -> UtenlandsoppholdÅrsak.INGEN;
        };
    }


}
