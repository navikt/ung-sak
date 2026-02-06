package no.nav.ung.ytelse.aktivitetspenger.hendelsehåndtering;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.integrasjon.pdl.Foedselsdato;
import no.nav.k9.felles.integrasjon.pdl.Pdl;
import no.nav.k9.felles.integrasjon.pdl.Person;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.behandling.revurdering.ÅrsakOgPerioder;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.hendelsemottak.tjenester.FinnFagsakerForAktørTjeneste;
import no.nav.ung.sak.kontrakt.hendelser.FødselHendelse;
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.ung.sak.test.util.aktør.FiktiveFnr;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestRepositories;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenario;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.ung.sak.test.util.behandling.personopplysning.Personopplysning;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class PdlFødselshendelseFagsakTilVurderingUtlederTest {

    @Inject
    private EntityManager entityManager;
    @Inject
    private BehandlingRepository behandlingRepository;

    private Pdl pdlKlient = Mockito.mock(Pdl.class);

    private PdlFødselshendelseFagsakTilVurderingUtleder utleder;
    private PersonopplysningRepository personopplysningRepository;

    @BeforeEach
    void setUp() {
        personopplysningRepository = new PersonopplysningRepository(entityManager);
        utleder = new PdlFødselshendelseFagsakTilVurderingUtleder(
            behandlingRepository,
            new FinnFagsakerForAktørTjeneste(entityManager, new FagsakRepository(entityManager)),
            personopplysningRepository,
            pdlKlient
        );
    }

    @Test
    void skal_finne_fagsak_med_fødsel() {
        // Arrange
        LocalDate fom = LocalDate.now();
        Behandling behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        String barnIdent = new FiktiveFnr().nesteFnr();
        AktørId barnAktørId = AktørId.dummy();
        LocalDate fødselsdato = fom.plusDays(10);
        mockPDL(barnIdent, fødselsdato, barnAktørId);
        FødselHendelse hendelse = lagFødselshendelse(behandling, barnIdent, fødselsdato);

        // Act
        Map<Fagsak, List<ÅrsakOgPerioder>> fagsakÅrsakOgPeriodeMap = utleder.finnFagsakerTilVurdering(hendelse);

        // Assert
        assertEquals(1, fagsakÅrsakOgPeriodeMap.size());
    }

    @Test
    void skal_ikke_finne_fagsak_med_fødsel_dersom_personopplysning_allerede_er_hensyntatt() {
        // Arrange
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusDays(300);
        LocalDate barnFødselsdato = fom.plusDays(10);
        var barnAktørId = AktørId.dummy();
        Behandling behandling = lagBehandlingMedBarn(new Periode(fom, tom), barnAktørId, barnFødselsdato);
        String barnIdent = new FiktiveFnr().nesteFnr();
        mockPDL(barnIdent, barnFødselsdato, barnAktørId);
        FødselHendelse hendelse = lagFødselshendelse(behandling, barnIdent, barnFødselsdato);

        // Act
        var fagsakÅrsakOgPeriodeMap = utleder.finnFagsakerTilVurdering(hendelse);

        // Assert
        assertEquals(0, fagsakÅrsakOgPeriodeMap.size());
    }

    private Behandling lagBehandlingMedBarn(Periode søknadsperiode, AktørId barnAktørId, LocalDate barnFødselsdato) {
        Behandling behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().medAktivitetspengerTestGrunnlag(
            new AktivitetspengerTestScenario(
                "Test Testesen",
                List.of(søknadsperiode),
                LocalDateTimeline.empty(),
                LocalDateTimeline.empty(),
                LocalDate.now().minusYears(19),
                Set.of(),
                List.of(PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT).leggTilPersonopplysning(Personopplysning.builder().aktørId(barnAktørId).fødselsdato(barnFødselsdato).build()).build()),
                null,
                null)
        ).buildOgLagreMedAktivitspenger(AktivitetspengerTestRepositories.lagAlleAktivitetspengerTestRepositoriesOgAbakusTjeneste(entityManager, null));
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
        when(pdlKlient.hentPerson(ArgumentMatchers.assertArg(q -> q.getInput().get("ident").equals(ident)), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(
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
