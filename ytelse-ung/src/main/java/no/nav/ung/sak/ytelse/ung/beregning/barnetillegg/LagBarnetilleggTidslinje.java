package no.nav.ung.sak.ytelse.ung.beregning.barnetillegg;

import static no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_ENDE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.typer.AktørId;

/**
 * Finner tidslinje for antall barn.
 */
@Dependent
public class LagBarnetilleggTidslinje {


    public static final BigDecimal BARNETILLEGG_DAGSATS = BigDecimal.valueOf(36);

    private final PersonopplysningRepository personopplysningRepository;
    private final HentFødselOgDød hentFødselOgDød;

    @Inject
    public LagBarnetilleggTidslinje(PersonopplysningRepository personopplysningRepository, HentFødselOgDød hentFødselOgDød) {
        this.personopplysningRepository = personopplysningRepository;
        this.hentFødselOgDød = hentFødselOgDød;
    }

    /**
     * Utleder tidslinje for utbetaling av barnetillegg
     *
     * @param behandlingReferanse Behandlingreferanse
     * @return Tidslinje for barnetillegg der denne overstiger 0
     */
    public BarnetilleggVurdering lagTidslinje(BehandlingReferanse behandlingReferanse, LocalDateTimeline<Boolean> perioder) {
        var personopplysningGrunnlagEntitet = personopplysningRepository.hentPersonopplysninger(behandlingReferanse.getBehandlingId());
        var barnAvSøkerAktørId = personopplysningGrunnlagEntitet.getGjeldendeVersjon().getRelasjoner()
            .stream().filter(r -> r.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(PersonRelasjonEntitet::getTilAktørId)
            .toList();

        var relevantPersonInfoBarn = barnAvSøkerAktørId.stream()
            .map(this::finnRelevantPersonInfo)
            .toList();

        return beregnBarnetillegg(perioder, relevantPersonInfoBarn);

    }

    static BarnetilleggVurdering beregnBarnetillegg(LocalDateTimeline<Boolean> perioder, List<FødselOgDødInfo> relevantPersonInfoBarn) {
        var antallBarnGrunnlagTidslinje = relevantPersonInfoBarn.stream()
            .map(info -> new LocalDateTimeline<>(info.fødselsdato().plusMonths(1).withDayOfMonth(1), getTilDato(info), 1))
            .reduce((t1, t2) -> t1.crossJoin(t2, StandardCombinators::sum))
            .orElse(LocalDateTimeline.empty());

        var relevantBarnetilleggTidslinje = antallBarnGrunnlagTidslinje.intersection(perioder)
            .mapValue(antallBarn -> new Barnetillegg(finnUtregnetBarnetillegg(antallBarn), antallBarn));

        return new BarnetilleggVurdering(relevantBarnetilleggTidslinje, relevantPersonInfoBarn);
    }

    private static int finnUtregnetBarnetillegg(Integer antallBarn) {
        return BigDecimal.valueOf(antallBarn).multiply(BARNETILLEGG_DAGSATS).intValue(); // Både sats og antall barn er heltall, så vi trenger ingen avrunding. Caster direkte til long
    }


    private static LocalDate getTilDato(FødselOgDødInfo info) {
        return info.dødsdato() != null ? info.dødsdato().plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()) : TIDENES_ENDE;
    }

    private FødselOgDødInfo finnRelevantPersonInfo(AktørId barnAktørId) {
        return hentFødselOgDød.hentFødselOgDødInfo(barnAktørId);
    }


}
