package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.HarAktørId;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.person.SivilstandType;

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

        assertThat(oppdater.build().getPersonopplysninger()).hasSize(3);
        assertThat(oppdater.build().getRelasjoner()).hasSize(2);

        oppdater.tilbakestill(søker);

        assertThat(oppdater.build().getPersonopplysninger().stream().map(HarAktørId::getAktørId)).containsExactly(søker);
        assertThat(oppdater.build().getRelasjoner()).isEmpty();

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
