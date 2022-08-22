package no.nav.k9.sak.domene.person.pdl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.k9.felles.integrasjon.pdl.Bostedsadresse;
import no.nav.k9.felles.integrasjon.pdl.Oppholdsadresse;
import no.nav.k9.felles.integrasjon.pdl.Vegadresse;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.sak.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.k9.sak.behandlingslager.aktør.historikk.Gyldighetsperiode;

class PersoninfoTjenesteTest {

    @Test
    void midlertidig_oppholdsadresse_skal_bare_telle_så_lenge_den_er_gyldig() {

        //Date gyldigFraOgMed, Date gyldigTilOgMed, String coAdressenavn, UtenlandskAdresse utenlandskAdresse, Vegadresse vegadresse, Matrikkeladresse matrikkeladresse, String oppholdAnnetSted, Folkeregistermetadata folkeregistermetadata, Metadata metadata
        Oppholdsadresse oppholdsadresse = new Oppholdsadresse(
            Date.from(Instant.parse("2022-10-20T22:00:00.000Z")),
            Date.from(Instant.parse("2022-10-20T22:00:00.000Z")),
            null, null, null, null, null, null, null
        );

        //String matrikkelId, String husnummer, String husbokstav, String bruksenhetsnummer, String adressenavn, String kommunenummer, String bydelsnummer, String tilleggsnavn, String postnummer, Koordinater koordinater
        Vegadresse vegadresse1 = new Vegadresse("m1", null, null, null, "VEI 1", null, null, null, "0001", null);
        Vegadresse vegadresse2 = new Vegadresse("m2", null, null, null, "VEI 2", null, null, null, "0001", null);

        //String angittFlyttedato, Date gyldigFraOgMed, Date gyldigTilOgMed, String coAdressenavn, Vegadresse vegadresse, Matrikkeladresse matrikkeladresse, UtenlandskAdresse utenlandskAdresse, UkjentBosted ukjentBosted, Folkeregistermetadata folkeregistermetadata, Metadata metadata
        Bostedsadresse bostedsadresse1 = new Bostedsadresse("2022-09-25", Date.from(Instant.parse("2022-10-20T22:00:00.000Z")), null, null, vegadresse1, null, null, null, null, null);
        Bostedsadresse bostedsadresse2 = new Bostedsadresse(null, Date.from(Instant.parse("2021-01-28T23:00:00.000Z")), null, null, vegadresse2, null, null, null, null, null);
        List<AdressePeriode> adressene = PersoninfoTjeneste.mapAdresserHistorikk(List.of(bostedsadresse1, bostedsadresse2), List.of(), List.of(oppholdsadresse));
        List<AdressePeriode> periodisertAdresse2 = PersoninfoTjeneste.periodiserAdresse(adressene);

        AdressePeriode adresssperiode1 = AdressePeriode.builder()
            .medGyldighetsperiode(Gyldighetsperiode.innenfor(LocalDate.of(2021, 1, 29), LocalDate.of(2022, 9, 24)))
            .medAdresseType(AdresseType.BOSTEDSADRESSE)
            .medMatrikkelId("m2")
            .medAdresselinje1("VEI 2")
            .medPostnummer("0001")
            .medLand("NOR")
            .build();
        AdressePeriode adresssperiode2 = AdressePeriode.builder()
            .medGyldighetsperiode(Gyldighetsperiode.innenfor(LocalDate.of(2022, 9, 25), LocalDate.of(2022, 10, 20)))
            .medAdresseType(AdresseType.BOSTEDSADRESSE)
            .medMatrikkelId("m1")
            .medAdresselinje1("VEI 1")
            .medPostnummer("0001")
            .medLand("NOR")
            .build();
        AdressePeriode adresssperiode3 = AdressePeriode.builder()
            .medGyldighetsperiode(Gyldighetsperiode.innenfor(LocalDate.of(2022, 10, 21), LocalDate.of(2022, 10, 21)))
            .medAdresseType(AdresseType.UKJENT_ADRESSE)
            .medLand("???")
            .build();
        AdressePeriode adresssperiode4 = AdressePeriode.builder()
            .medGyldighetsperiode(Gyldighetsperiode.innenfor(LocalDate.of(2022, 10, 22), LocalDate.of(9999, 12, 31)))
            .medAdresseType(AdresseType.BOSTEDSADRESSE)
            .medMatrikkelId("m1")
            .medAdresselinje1("VEI 1")
            .medPostnummer("0001")
            .medLand("NOR")
            .build();

        Assertions.assertThat(periodisertAdresse2).containsOnly(adresssperiode1, adresssperiode2, adresssperiode3, adresssperiode4);
    }

}
