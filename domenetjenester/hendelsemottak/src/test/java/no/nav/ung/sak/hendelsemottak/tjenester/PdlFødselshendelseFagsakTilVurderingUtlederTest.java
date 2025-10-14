package no.nav.ung.sak.hendelsemottak.tjenester;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.integrasjon.pdl.*;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.hendelser.FødselHendelse;
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.aktør.FiktiveFnr;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.ung.sak.test.util.behandling.personopplysning.Personopplysning;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class PdlFødselshendelseFagsakTilVurderingUtlederTest {

    @Inject
    private EntityManager entityManager;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    private Pdl pdlKlient = Mockito.mock(Pdl.class);


    private PdlFødselshendelseFagsakTilVurderingUtleder utleder;
    private PersonopplysningRepository personopplysningRepository;

    @BeforeEach
    void setUp() {
        personopplysningRepository = new PersonopplysningRepository(entityManager);
        utleder = new PdlFødselshendelseFagsakTilVurderingUtleder(
            behandlingRepository,
            ungdomsprogramPeriodeRepository,
            new FinnFagsakerForAktørTjeneste(entityManager, new FagsakRepository(entityManager)),
            personopplysningRepository,
            pdlKlient
        );
    }

    @Test
    void skal_finne_fagsak_med_fødsel_innenfor_programperiode() {
        // Arrange
        LocalDate fom = LocalDate.now();
        Behandling behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        DatoIntervallEntitet programperiode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(260));
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(programperiode)));
        String barnIdent = new FiktiveFnr().nesteFnr();
        AktørId barnAktørId = AktørId.dummy();
        LocalDate fødselsdato = fom.plusDays(10);
        mockPDL(barnIdent, fødselsdato, barnAktørId);
        FødselHendelse hendelse = lagFødselshendelse(behandling, barnIdent, fødselsdato);

        // Act
        Map<Fagsak, ÅrsakOgPeriode> fagsakÅrsakOgPeriodeMap = utleder.finnFagsakerTilVurdering(hendelse);

        // Assert
        assertEquals(1, fagsakÅrsakOgPeriodeMap.size());
    }

    @Test
    void skal_ikke_finne_fagsak_med_fødsel_dersom_personopplysning_allerede_er_hensyntatt() {
        // Arrange
        LocalDate fom = LocalDate.now();
        LocalDate barnFødselsdato = fom.plusDays(10);
        var barnAktørId = AktørId.dummy();
        DatoIntervallEntitet programperiode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(260));
        Behandling behandling = lagBehandlingMedBarn(programperiode, fom, barnAktørId, barnFødselsdato);
        String barnIdent = new FiktiveFnr().nesteFnr();
        mockPDL(barnIdent, barnFødselsdato, barnAktørId);
        FødselHendelse hendelse = lagFødselshendelse(behandling, barnIdent, barnFødselsdato);

        // Act
        Map<Fagsak, ÅrsakOgPeriode> fagsakÅrsakOgPeriodeMap = utleder.finnFagsakerTilVurdering(hendelse);

        // Assert
        assertEquals(0, fagsakÅrsakOgPeriodeMap.size());
    }

    private Behandling lagBehandlingMedBarn(DatoIntervallEntitet programperiode, LocalDate fom, AktørId barnAktørId, LocalDate barnFødselsdato) {
        Behandling behandling = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(
            new UngTestScenario(
                "Test Testesen",
                List.of(new UngdomsprogramPeriode(programperiode)),
                LocalDateTimeline.empty(),
                null,
                LocalDateTimeline.empty(),
                LocalDateTimeline.empty(),
                LocalDateTimeline.empty(),
                fom.minusYears(28),
                List.of(fom),
                Set.of(),
                List.of(PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT).leggTilPersonopplysning(Personopplysning.builder().aktørId(barnAktørId).fødselsdato(barnFødselsdato).build()).build()),
                null
            )
        ).buildOgLagreMedUng(UngTestRepositories.lagAlleUngTestRepositoriesOgAbakusTjeneste(entityManager, null));
        return behandling;
    }


    private void mockPDL(String ident, LocalDate fødselsdato, AktørId barnAktørId) {
        Foedselsdato foedselsdato = new Foedselsdato(fødselsdato.getYear(), fødselsdato.format(DateTimeFormatter.ISO_LOCAL_DATE), null, null);
        Person person = new Person(
            Collections.emptyList(), // adressebeskyttelse
            Collections.emptyList(), // bostedsadresse
            Collections.emptyList(), // deltBosted
            Collections.emptyList(), // doedfoedtBarn
            Collections.emptyList(), // doedsfall
            null,                    // falskIdentitet
            List.of(foedselsdato),   // foedselsdato (her setter du inn en Foedselsdato-instans)
            Collections.emptyList(), // folkeregisteridentifikator
            Collections.emptyList(), // folkeregisterpersonstatus
            Collections.emptyList(), // forelderBarnRelasjon
            Collections.emptyList(), // foreldreansvar
            Collections.emptyList(), // fullmakt
            Collections.emptyList(), // identitetsgrunnlag
            Collections.emptyList(), // kjoenn
            Collections.emptyList(), // kontaktadresse
            Collections.emptyList(), // kontaktinformasjonForDoedsbo
            Collections.emptyList(), // navn
            Collections.emptyList(), // opphold
            Collections.emptyList(), // oppholdsadresse
            Collections.emptyList(), // sikkerhetstiltak
            Collections.emptyList(), // sivilstand
            Collections.emptyList(), // statsborgerskap
            Collections.emptyList(), // telefonnummer
            Collections.emptyList(), // tilrettelagtKommunikasjon
            Collections.emptyList(), // utenlandskIdentifikasjonsnummer
            Collections.emptyList(), // innflyttingTilNorge
            Collections.emptyList(), // utflyttingFraNorge
            Collections.emptyList()  // vergemaalEllerFremtidsfullmakt
        );
        when(pdlKlient.hentPerson(ArgumentMatchers.assertArg(q -> q.getInput().get("ident").equals(ident)), ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn(
            person
        );
        when(pdlKlient.hentAktørIdForPersonIdent(ArgumentMatchers.matches(ident))).thenReturn(Optional.of(barnAktørId.getAktørId()));


    }

    private static FødselHendelse lagFødselshendelse(Behandling behandling, String barnIdent, LocalDate fødselsdato) {
        HendelseInfo hendelseInfo = lagHendelseInfo(behandling);
        FødselHendelse hendelse = new FødselHendelse(
            hendelseInfo,
            fødselsdato,
            new PersonIdent(barnIdent)
        );
        return hendelse;
    }

    private static HendelseInfo lagHendelseInfo(Behandling behandling) {
        HendelseInfo.Builder builder = new HendelseInfo.Builder();
        builder.medHendelseId("123");
        builder.leggTilAktør(behandling.getAktørId());
        builder.medOpprettet(LocalDateTime.now());
        HendelseInfo hendelseInfo = builder.build();
        return hendelseInfo;
    }

}
