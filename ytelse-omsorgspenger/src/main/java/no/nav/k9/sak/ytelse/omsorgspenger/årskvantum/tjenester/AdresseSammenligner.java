package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;

public class AdresseSammenligner {

    private AdresseSammenligner() {
    }

    public static LocalDateTimeline<Boolean> sammeBostedsadresseEllerDeltBosted(List<PersonAdresseEntitet> søkersAdresser, List<PersonAdresseEntitet> barnsAdresser) {
        List<PersonAdresseEntitet> søkersBostedsadresser = filtrerAdresser(søkersAdresser, Set.of(AdresseType.BOSTEDSADRESSE));
        List<PersonAdresseEntitet> barnsBostedsadresser = filtrerAdresser(barnsAdresser, Set.of(AdresseType.BOSTEDSADRESSE, AdresseType.DELT_BOSTEDSADRESSE));
        return lagTidslinjeHvorLike(søkersBostedsadresser, barnsBostedsadresser);
    }

    public static LocalDateTimeline<Boolean> sammeBostedsadresse(List<PersonAdresseEntitet> søkersAdresser, List<PersonAdresseEntitet> barnsAdresser) {
        List<PersonAdresseEntitet> søkersBostedsadresser = filtrerAdresser(søkersAdresser, Set.of(AdresseType.BOSTEDSADRESSE));
        List<PersonAdresseEntitet> barnsBostedsadresser = filtrerAdresser(barnsAdresser, Set.of(AdresseType.BOSTEDSADRESSE));
        return lagTidslinjeHvorLike(søkersBostedsadresser, barnsBostedsadresser);
    }

    public static LocalDateTimeline<Boolean> perioderDeltBosted(List<PersonAdresseEntitet> søkersAdresser, List<PersonAdresseEntitet> barnsAdresser) {
        List<PersonAdresseEntitet> søkersBostedsadresser = filtrerAdresser(søkersAdresser, Set.of(AdresseType.BOSTEDSADRESSE));
        List<PersonAdresseEntitet> barnsBostedsadresser = filtrerAdresser(barnsAdresser, Set.of(AdresseType.DELT_BOSTEDSADRESSE));
        return lagTidslinjeHvorLike(søkersBostedsadresser, barnsBostedsadresser);
    }

    private static LocalDateTimeline<Boolean> lagTidslinjeHvorLike(List<PersonAdresseEntitet> søkersBostedsadresser, List<PersonAdresseEntitet> barnsBostedsadresser) {
        var segmenter = new ArrayList<LocalDateSegment<Boolean>>();
        for (PersonAdresseEntitet søkersAdresse : søkersBostedsadresser) {
            for (PersonAdresseEntitet barnsAdresse : barnsBostedsadresser) {
                if (likeAdresser(barnsAdresse, søkersAdresse) && søkersAdresse.getPeriode().overlapper(barnsAdresse.getPeriode())) {
                    LocalDateInterval overlapp = søkersAdresse.getPeriode().overlapp(barnsAdresse.getPeriode()).toLocalDateInterval();
                    segmenter.add(new LocalDateSegment<>(overlapp, true));
                }
            }
        }
        return new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch).compress();
    }

    private static boolean likeAdresser(PersonAdresseEntitet adresse1, PersonAdresseEntitet adresse2) {
        return Objects.equals(adresse1.getAdresselinje1(), adresse2.getAdresselinje1())
            && Objects.equals(adresse1.getPostnummer(), adresse2.getPostnummer())
            && Objects.equals(adresse1.getLand(), adresse2.getLand());
    }

    private static List<PersonAdresseEntitet> filtrerAdresser(List<PersonAdresseEntitet> søkersAdresser, Set<AdresseType> adresseTyper) {
        return søkersAdresser.stream().filter(s -> adresseTyper.contains(s.getAdresseType())).toList();
    }
}
