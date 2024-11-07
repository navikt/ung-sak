package no.nav.k9.sak.ytelse.ung.beregning.barnetillegg;

import static no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_ENDE;

import java.time.LocalDate;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.integrasjon.pdl.PdlKlient;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.typer.AktørId;

/**
 * Finner tidslinje for antall barn.
 */
@Dependent
public class LagAntallBarnTidslinje {

    private final PersonopplysningRepository personopplysningRepository;
    private final HentFødselOgDød hentFødselOgDød;

    @Inject
    public LagAntallBarnTidslinje(PersonopplysningRepository personopplysningRepository, HentFødselOgDød hentFødselOgDød) {
        this.personopplysningRepository = personopplysningRepository;
        this.hentFødselOgDød = hentFødselOgDød;
    }

    /**
     * Utleder tidslinje for perioder der bruker har ett eller flere barn. I perioder uten barn er tidslinjen tom
     *
     * @param behandlingReferanse Behandlingreferanse
     * @return Tidslinje for antall barn der antall er mer eller lik 1
     */
    public LocalDateTimeline<Integer> lagAntallBarnTidslinje(BehandlingReferanse behandlingReferanse) {
        var personopplysningGrunnlagEntitet = personopplysningRepository.hentPersonopplysninger(behandlingReferanse.getBehandlingId());
        var barnAvSøkerAktørId = personopplysningGrunnlagEntitet.getGjeldendeVersjon().getRelasjoner()
            .stream().filter(r -> r.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(PersonRelasjonEntitet::getTilAktørId)
            .toList();

        var relevantPersonInfoBarn = barnAvSøkerAktørId.stream()
            .map(this::finnRelevantPersonInfo)
            .toList();

        return relevantPersonInfoBarn.stream()
            .map(info -> new LocalDateTimeline<>(info.fødselsdato(), getTilDato(info), 1))
            .reduce((t1, t2) -> t1.crossJoin(t2, StandardCombinators::sum))
            .orElse(LocalDateTimeline.empty());
    }

    private static LocalDate getTilDato(HentFødselOgDød.FødselOgDødInfo info) {
        return info.dødsdato() != null ? info.dødsdato() : TIDENES_ENDE;
    }

    private HentFødselOgDød.FødselOgDødInfo finnRelevantPersonInfo(AktørId barnAktørId) {
        return hentFødselOgDød.hentFødselOgDødInfo(barnAktørId);
    }




}
