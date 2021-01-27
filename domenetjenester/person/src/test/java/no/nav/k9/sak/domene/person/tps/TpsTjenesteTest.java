package no.nav.k9.sak.domene.person.tps;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static no.nav.k9.kodeverk.person.NavBrukerKjønn.KVINNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.k9.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.feil.PersonIkkeFunnet;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class TpsTjenesteTest {
    private TpsTjeneste testSubject;

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
    private PersoninfoAdapter personinfoAdapter;

    @BeforeEach
    public void oppsett() {
        AKTØR_ID_VED_FNR.put(FNR, AKTØR_ID);
        AKTØR_ID_VED_FNR.put(ENDRET_FNR, ENDRET_AKTØR_ID);
        AKTØR_ID_VED_FNR.put(FNR_RELASJON, AKTØR_ID_RELASJON);

        personinfoAdapter = mock(PersoninfoAdapter.class);
        when(personinfoAdapter.hentAktørIdForPersonIdent(FNR)).thenReturn(of(AKTØR_ID_VED_FNR.get(FNR)));


        testSubject = new TpsTjenesteImpl(personinfoAdapter);
    }

    @Test
    public void skal_ikke_hente_bruker_for_ukjent_aktør() {
        Optional<Personinfo> funnetBruker = testSubject.hentBrukerForAktør(AktørId.dummy());
        assertThat(funnetBruker).isNotPresent();
    }

    @Test
    public void skal_hente_bruker_for_kjent_fnr() {
        when(personinfoAdapter.hentKjerneinformasjon(AKTØR_ID)).thenReturn(
            new Personinfo.Builder()
                .medAktørId(AKTØR_ID)
                .medPersonIdent(FNR)
                .medNavn(NAVN)
                .medFødselsdato(FØDSELSDATO)
                .medKjønn(KVINNE)
                .medFamilierelasjon(new HashSet<>(singletonList(FAMILIERELASJON)))
                .build()
        );

        Optional<Personinfo> funnetBruker = testSubject.hentBrukerForFnr(FNR);
        assertThat(funnetBruker).isPresent();
    }

    @Test
    public void skal_ikke_hente_bruker_for_ukjent_fnr() {
        Optional<Personinfo> funnetBruker = testSubject.hentBrukerForFnr(new PersonIdent("666"));
        assertThat(funnetBruker).isNotPresent();
    }

    @Test
    public void test_hentGeografiskTilknytning_finnes() {
        when(personinfoAdapter.hentGeografiskTilknytning(FNR)).thenReturn(new GeografiskTilknytning("0219", "KLIE"));

        GeografiskTilknytning geografiskTilknytning = testSubject.hentGeografiskTilknytning(FNR);
        assertThat(geografiskTilknytning).isNotNull();
    }

    @Test
    public void test_hentGeografiskTilknytning_finnes_ikke() {
        doThrow(TpsFeilmeldinger.FACTORY.geografiskTilknytningIkkeFunnet(
            new HentGeografiskTilknytningPersonIkkeFunnet("finner ikke person", new PersonIkkeFunnet())).toException()
        )
            .when(personinfoAdapter).hentGeografiskTilknytning(new PersonIdent("666"));

        Assertions.assertThrows(TekniskException.class, () -> testSubject.hentGeografiskTilknytning(new PersonIdent("666")));
    }
}
