package no.nav.k9.sak.domene.person.tps;

import static java.util.Collections.singletonList;
import static no.nav.k9.kodeverk.person.NavBrukerKjønn.KVINNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandlingslager.aktør.Adresseinfo;
import no.nav.k9.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.k9.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.k9.sak.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.feil.PersonIkkeFunnet;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class TpsTjenesteTest {
    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final AktørId ENDRET_AKTØR_ID = AktørId.dummy();
    private static final PersonIdent FNR = new PersonIdent("12345678901");
    private static final PersonIdent ENDRET_FNR = new PersonIdent("02345678901");
    private static final LocalDate FØDSELSDATO = LocalDate.of(1992, Month.OCTOBER, 13);
    private static final String NAVN = "Anne-Berit Hjartdal";
    // Familierelasjon
    private static final AktørId AKTØR_ID_RELASJON = AktørId.dummy();
    private static final PersonIdent FNR_RELASJON = new PersonIdent("01345678901");
    private static final Familierelasjon FAMILIERELASJON = new Familierelasjon(FNR_RELASJON, RelasjonsRolleType.BARN, true);
    private static final Map<PersonIdent, AktørId> AKTØR_ID_VED_FNR = new HashMap<>();

    private TpsTjeneste tpsTjeneste;

    @BeforeEach
    public void oppsett() {
        AKTØR_ID_VED_FNR.put(FNR, AKTØR_ID);
        AKTØR_ID_VED_FNR.put(ENDRET_FNR, ENDRET_AKTØR_ID);
        AKTØR_ID_VED_FNR.put(FNR_RELASJON, AKTØR_ID_RELASJON);
        tpsTjeneste = new TpsTjenesteImpl(new TpsAdapterMock(), mock(PersoninfoAdapter.class));
    }

    @Test
    public void skal_ikke_hente_bruker_for_ukjent_aktør() {
        Optional<Personinfo> funnetBruker = tpsTjeneste.hentBrukerForAktør(AktørId.dummy());
        assertThat(funnetBruker).isNotPresent();
    }

    @Test
    public void skal_hente_bruker_for_kjent_fnr() {
        Optional<Personinfo> funnetBruker = tpsTjeneste.hentBrukerForFnr(FNR);
        assertThat(funnetBruker).isPresent();
    }

    @Test
    public void skal_ikke_hente_bruker_for_ukjent_fnr() {
        Optional<Personinfo> funnetBruker = tpsTjeneste.hentBrukerForFnr(new PersonIdent("666"));
        assertThat(funnetBruker).isNotPresent();
    }

    @Test
    public void test_hentGeografiskTilknytning_finnes() {
        GeografiskTilknytning geografiskTilknytning = tpsTjeneste.hentGeografiskTilknytning(FNR);
        assertThat(geografiskTilknytning).isNotNull();
    }

    @Test
    public void test_hentGeografiskTilknytning_finnes_ikke() {
        Assertions.assertThrows(TekniskException.class, () -> tpsTjeneste.hentGeografiskTilknytning(new PersonIdent("666")));
    }

    private static class TpsAdapterMock implements TpsAdapter {
        private static final String ADR1 = "Adresselinje1";
        private static final String ADR2 = "Adresselinje2";
        private static final String ADR3 = "Adresselinje3";
        private static final String POSTNR = "1234";
        private static final String POSTSTED = "Oslo";
        private static final String LAND = "Norge";

        @Override
        public Optional<AktørId> hentAktørIdForPersonIdent(PersonIdent fnr) {
            return Optional.ofNullable(AKTØR_ID_VED_FNR.get(fnr));
        }

        @Override
        public Personinfo hentKjerneinformasjon(PersonIdent fnr, AktørId aktørId) {
            if (!AKTØR_ID_VED_FNR.containsKey(fnr)) {
                return null;
            }
            return new Personinfo.Builder()
                .medAktørId(aktørId)
                .medPersonIdent(fnr)
                .medNavn(NAVN)
                .medFødselsdato(FØDSELSDATO)
                .medKjønn(KVINNE)
                .medFamilierelasjon(new HashSet<>(singletonList(FAMILIERELASJON)))
                .build();
        }

        @Override
        public Personhistorikkinfo hentPersonhistorikk(AktørId aktørId, Periode periode) {
            return null;
        }

        @Override
        public Adresseinfo hentAdresseinformasjon(PersonIdent fnr) {
            return new Adresseinfo.Builder(AdresseType.BOSTEDSADRESSE, fnr, NAVN, PersonstatusType.BOSA)
                .medAdresselinje1(ADR1)
                .medAdresselinje2(ADR2)
                .medAdresselinje3(ADR3)
                .medPostNr(POSTNR)
                .medPoststed(POSTSTED)
                .medLand(LAND)
                .build();
        }

        @Override
        public GeografiskTilknytning hentGeografiskTilknytning(PersonIdent fnr) {
            if (FNR.equals(fnr)) {
                return new GeografiskTilknytning("0219", "KLIE");
            }
            throw TpsFeilmeldinger.FACTORY.geografiskTilknytningIkkeFunnet(
                new HentGeografiskTilknytningPersonIkkeFunnet("finner ikke person", new PersonIkkeFunnet())).toException();
        }

        @Override
        public PersoninfoBasis hentKjerneinformasjonBasis(PersonIdent fnr, AktørId aktørId) {
            return null;
        }

        @Override
        public List<GeografiskTilknytning> hentDiskresjonskoderForFamilierelasjoner(PersonIdent fnr) {
            return Collections.emptyList();
        }
    }
}
