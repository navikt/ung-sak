package no.nav.k9.sak.domene.person.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

public class StatsborgerskapEndringIdentifisererTest {

    private AktørId AKTØRID = AktørId.dummy();

    @Test
    public void testStatsborgerskapUendret() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(LandOgRegion.get(Landkoder.NOR, Region.NORDEN)));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(LandOgRegion.get(Landkoder.NOR, Region.NORDEN)));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erStatsborgerskapEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at statsborgerskap er uendret").isFalse();
    }

    @Test
    public void testStatsborgerskapUendret_flere_koder() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(LandOgRegion.get(Landkoder.NOR, Region.NORDEN), LandOgRegion.get(Landkoder.SWE, Region.NORDEN)));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(LandOgRegion.get(Landkoder.NOR, Region.NORDEN), LandOgRegion.get(Landkoder.SWE, Region.NORDEN)));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erStatsborgerskapEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at statsborgerskap er uendret").isFalse();
    }

    @Test
    public void testStatsborgerskapUendret_men_rekkefølge_i_liste_endret() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(LandOgRegion.get(Landkoder.NOR, Region.NORDEN), LandOgRegion.get(Landkoder.SWE, Region.NORDEN)));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlagMotstattRekkefølge(personopplysningGrunnlag1.getRegisterVersjon().map(PersonInformasjonEntitet::getStatsborgerskap).orElse(Collections.emptyList()));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erStatsborgerskapEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at endring i rekkefølge ikke skal detektere endring.").isFalse();
    }

    @Test
    public void testStatsborgerskapEndret() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(LandOgRegion.get(Landkoder.SWE, Region.NORDEN)));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(LandOgRegion.get(Landkoder.NOR, Region.NORDEN)));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erStatsborgerskapEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at endring i statsborgerskap blir detektert.").isTrue();
    }

    @Test
    public void testStatsborgerskapEndret_endret_type() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(LandOgRegion.get(Landkoder.SWE, Region.NORDEN), LandOgRegion.get(Landkoder.NOR, Region.NORDEN)));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(LandOgRegion.get(Landkoder.SWE, Region.NORDEN), LandOgRegion.get(Landkoder.USA, Region.UDEFINERT)));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erStatsborgerskapEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at endring i statsborgerskap blir detektert.").isTrue();
    }

    @Test
    public void testStatsborgerskapEndret_ekstra_statsborgerskap_lagt_til() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(LandOgRegion.get(Landkoder.SWE, Region.NORDEN)));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(LandOgRegion.get(Landkoder.SWE, Region.NORDEN), LandOgRegion.get(Landkoder.NOR, Region.NORDEN)));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erStatsborgerskapEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at endring i statsborgerskap blir detektert.").isTrue();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlagMotstattRekkefølge(List<StatsborgerskapEntitet> statsborgerLand) {
        final PersonInformasjonBuilder builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        //Bygg opp identiske statsborgerskap, bare legg de inn i motsatt rekkefølge.
        statsborgerLand.stream()
            .collect(Collectors.toCollection(LinkedList::new))
            .descendingIterator()
            .forEachRemaining(s -> builder1.leggTil(builder1.getStatsborgerskapBuilder(AKTØRID, s.getPeriode(), s.getStatsborgerskap(), s.getRegion()).medStatsborgerskap(s.getStatsborgerskap())));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }


    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(List<LandOgRegion> statsborgerskap) {
        final PersonInformasjonBuilder builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1
            .leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
            IntStream.range(0, statsborgerskap.size())
                .forEach( i -> builder1.leggTil(builder1.getStatsborgerskapBuilder(AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), statsborgerskap.get(i).land, statsborgerskap.get(i).region)));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }

    private static class LandOgRegion {
        private Landkoder land;
        private Region region;

        private static LandOgRegion get(Landkoder land, Region region) {
            LandOgRegion landOgRegion = new LandOgRegion();
            landOgRegion.land = land;
            landOgRegion.region = region;
            return landOgRegion;
        }
    }

}
