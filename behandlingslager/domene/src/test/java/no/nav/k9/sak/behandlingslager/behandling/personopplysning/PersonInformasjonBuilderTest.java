package no.nav.k9.sak.behandlingslager.behandling.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.sak.typer.AktørId;

public class PersonInformasjonBuilderTest {

    @Test
    public void skal_tilbakestille_kladden_ved_oppdatering() {

        AktørId søker = AktørId.dummy();
        AktørId brn1 = AktørId.dummy();
        AktørId brn2 = AktørId.dummy();

        PersonInformasjonBuilder førsteInnhenting = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        førsteInnhenting.leggTil(lagPersonopplysning(søker, førsteInnhenting));
        førsteInnhenting.leggTil(lagPersonopplysning(brn1, førsteInnhenting));
        førsteInnhenting.leggTil(lagPersonopplysning(brn2, førsteInnhenting));

        førsteInnhenting.leggTil(lagRelasjon(søker, brn1, RelasjonsRolleType.MORA, førsteInnhenting));
        førsteInnhenting.leggTil(lagRelasjon(søker, brn2, RelasjonsRolleType.MORA, førsteInnhenting));

        PersonInformasjonEntitet informasjon = førsteInnhenting.build();

        assertThat(informasjon.getPersonopplysninger()).hasSize(3);
        assertThat(informasjon.getRelasjoner()).hasSize(2);

        PersonInformasjonBuilder oppdater = PersonInformasjonBuilder.oppdater(Optional.of(informasjon), PersonopplysningVersjonType.REGISTRERT);

        PersonInformasjonEntitet ny = oppdater.build();
        assertThat(ny.getPersonopplysninger()).hasSize(3);
        assertThat(ny.getRelasjoner()).hasSize(2);

        oppdater.tilbakestill(søker);
        PersonInformasjonEntitet ny2 = oppdater.build();
        assertThat(ny2.getPersonopplysninger()).hasSize(1);
        assertThat(ny2.getRelasjoner()).isEmpty();

        assertThat(ny2.getPersonopplysninger().stream().map(HarAktørId::getAktørId)).containsExactly(søker);

    }

    private PersonInformasjonBuilder.RelasjonBuilder lagRelasjon(AktørId fra, AktørId til, RelasjonsRolleType type, PersonInformasjonBuilder informasjonBuilder) {
        return informasjonBuilder.getRelasjonBuilder(fra, til, type);
    }


    private PersonInformasjonBuilder.PersonopplysningBuilder lagPersonopplysning(AktørId aktørId, PersonInformasjonBuilder informasjonBuilder) {
        return informasjonBuilder
            .getPersonopplysningBuilder(aktørId)
            .medSivilstand(SivilstandType.GIFT)
            .medRegion(Region.NORDEN)
            .medNavn("Richard Feynman")
            .medFødselsdato(LocalDate.now())
            .medKjønn(NavBrukerKjønn.KVINNE);
    }
}
