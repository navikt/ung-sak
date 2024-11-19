package no.nav.ung.sak.domene.person.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import no.nav.ung.kodeverk.person.PersonstatusType;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;

public class PersonstatusEndringIdentifisererTest {

    private AktørId AKTØRID = AktørId.dummy();

    @Test
    public void testPersonstatusUendret() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(PersonstatusType.ABNR));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(PersonstatusType.ABNR));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erPersonstatusEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at personstatus er uendret").isFalse();
    }

    @Test
    public void testPersonstatusUendret_flere_statuser() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(PersonstatusType.ABNR, PersonstatusType.BOSA));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(PersonstatusType.ABNR, PersonstatusType.BOSA));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erPersonstatusEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at personstatus er uendret").isFalse();
    }

    @Test
    public void testPersonstatusEndret_ekstra_status_lagt_til() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(PersonstatusType.ABNR, PersonstatusType.BOSA));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(PersonstatusType.ABNR, PersonstatusType.BOSA, PersonstatusType.FOSV));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erPersonstatusEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at endring i personstatus blir detektert.").isTrue();
    }
    @Test
    public void testPersonstatusEndret_status_endret_type() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(PersonstatusType.ABNR, PersonstatusType.BOSA));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(PersonstatusType.ABNR, PersonstatusType.FOSV));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erPersonstatusEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at endring i personstatus blir detektert.").isTrue();
    }

    @Test
    public void testPersonstatusUendret_men_rekkefølge_i_liste_endret() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(PersonstatusType.ABNR, PersonstatusType.BOSA));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlagMotstattRekkefølge(personopplysningGrunnlag1.getRegisterVersjon().map(PersonInformasjonEntitet::getPersonstatus).orElse(Collections.emptyList()));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erPersonstatusEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at endring i rekkefølge ikke skal detektere endring.").isFalse();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlagMotstattRekkefølge(List<PersonstatusEntitet> personstatuser) {
        final PersonInformasjonBuilder builder1 = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        personstatuser.stream()
            .collect(Collectors.toCollection(LinkedList::new))
            .descendingIterator()
            .forEachRemaining(ps -> builder1.leggTil(builder1.getPersonstatusBuilder(AKTØRID, ps.getPeriode()).medPersonstatus(ps.getPersonstatus())));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(List<PersonstatusType> personstatuser) {
        final PersonInformasjonBuilder builder1 = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        //Opprett personstatuser med forskjellig fra og med dato. Går 1 mnd tilbake for hver status.
        IntStream.range(0, personstatuser.size()).forEach(i -> builder1.leggTil(builder1.getPersonstatusBuilder(AKTØRID, DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(i))).medPersonstatus(personstatuser.get(i))));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }
}
