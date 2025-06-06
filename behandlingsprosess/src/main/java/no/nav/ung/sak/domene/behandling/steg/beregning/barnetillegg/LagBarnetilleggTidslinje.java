package no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.BarnetilleggSatsTidslinje;
import no.nav.ung.sak.typer.AktørId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import static no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_ENDE;

/**
 * Finner tidslinje for barnetillegg.
 */
@Dependent
public class LagBarnetilleggTidslinje {


    private final PersonopplysningRepository personopplysningRepository;

    @Inject
    public LagBarnetilleggTidslinje(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

    /**
     * Utleder tidslinje for utbetaling av barnetillegg
     * <p>
     * Alle folkeregistrerte barn gir rett på barnetillegg. For en gitt måned utbetales det 36 kroner per dag for hvert barn som har levd i måneden før.
     * Dette betyr barns død påvirker utbetaling først to kalendermåneder etter dødsdato.
     * Barnetillegg utbetales alltid for hele måneder gitt at alle andre vilkår er oppfylt for måneden.
     *
     * @param behandlingReferanse Behandlingreferanse
     * @return Resultat med tidslinje for barnetillegg der dagsats overstiger 0
     */
    public BarnetilleggVurdering lagTidslinje(BehandlingReferanse behandlingReferanse, LocalDateTimeline<Boolean> perioder) {
        var personopplysningGrunnlagEntitet = personopplysningRepository.hentPersonopplysninger(behandlingReferanse.getBehandlingId());
        var barnAvSøkerAktørId = personopplysningGrunnlagEntitet.getGjeldendeVersjon().getRelasjoner()
            .stream().filter(r -> r.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(PersonRelasjonEntitet::getTilAktørId)
            .toList();

        final var relevantPersonInfo = barnAvSøkerAktørId.stream().map(mapFødselOgDødInformasjonForAktør(personopplysningGrunnlagEntitet)).toList();
        return beregnBarnetillegg(perioder, relevantPersonInfo);

    }

    private static Function<AktørId, FødselOgDødInfo> mapFødselOgDødInformasjonForAktør(PersonopplysningGrunnlagEntitet personopplysningGrunnlagEntitet) {
        return aktørId -> {
            final var personopplysning = personopplysningGrunnlagEntitet.getGjeldendeVersjon().getPersonopplysning(aktørId);
            return new FødselOgDødInfo(aktørId, personopplysning.getFødselsdato(), personopplysning.getDødsdato());
        };
    }

    static BarnetilleggVurdering beregnBarnetillegg(LocalDateTimeline<Boolean> perioder, List<FødselOgDødInfo> relevantPersonInfoBarn) {
        var antallBarnGrunnlagTidslinje = relevantPersonInfoBarn.stream()
            .map(info -> new LocalDateTimeline<>(info.fødselsdato(), getTilDato(info), 1))
            .reduce((t1, t2) -> t1.crossJoin(t2, StandardCombinators::sum))
            .orElse(LocalDateTimeline.empty());

        var relevantBarnetilleggTidslinje = antallBarnGrunnlagTidslinje.intersection(perioder)
            .combine(BarnetilleggSatsTidslinje.BARNETILLEGG_DAGSATS, barnetilleggCombinator(), LocalDateTimeline.JoinStyle.LEFT_JOIN);

        return new BarnetilleggVurdering(relevantBarnetilleggTidslinje, relevantPersonInfoBarn);
    }

    private static LocalDateSegmentCombinator<Integer, BigDecimal, Barnetillegg> barnetilleggCombinator() {
        return (di, lhs, rhs) -> {
            if (rhs == null) {
                throw new IllegalStateException("Fant ingen gyldig satsverdi for perioden " + di);
            }
            var antallBarn = lhs.getValue();
            var barnetilleggSats = rhs.getValue();
            return new LocalDateSegment<>(di, new Barnetillegg(finnUtregnetBarnetillegg(antallBarn, barnetilleggSats), antallBarn));
        };
    }

    private static int finnUtregnetBarnetillegg(Integer antallBarn, BigDecimal barnetilleggSats) {
        return BigDecimal.valueOf(antallBarn).multiply(barnetilleggSats).intValue(); // Både sats og antall barn er heltall, så vi trenger ingen avrunding. Caster direkte til int
    }


    private static LocalDate getTilDato(FødselOgDødInfo info) {
        return info.dødsdato() != null ? info.dødsdato() : TIDENES_ENDE;
    }

}
