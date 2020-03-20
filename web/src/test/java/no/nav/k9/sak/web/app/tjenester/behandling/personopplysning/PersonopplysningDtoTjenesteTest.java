package no.nav.k9.sak.web.app.tjenester.behandling.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.kontrakt.person.PersonopplysningDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.web.app.tjenester.behandling.personopplysning.PersonopplysningDtoTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;


@RunWith(CdiRunner.class)
public class PersonopplysningDtoTjenesteTest {


    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    @Inject
    PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository = new MedisinskGrunnlagRepository(repoRule.getEntityManager());
    private PersonopplysningDtoTjeneste tjeneste;

    @Before
    public void setUp() {
        tjeneste = new PersonopplysningDtoTjeneste(this.personopplysningTjeneste, medisinskGrunnlagRepository, repositoryProvider);
    }

    @Test
    public void skal_takle_at_man_spør_etter_opplysninger_utenfor_tidsserien() {
        //sørger for at vi bommer når vi spør etter personstatus
        LocalDate enTilfeldigDato = LocalDate.of(1989, 9, 29);
        Behandling behandling = lagBehandling();

        Optional<PersonopplysningDto> personopplysningDto = tjeneste.lagPersonopplysningDto(behandling.getId(), enTilfeldigDato);

        assertThat(personopplysningDto).isPresent();
        assertThat(personopplysningDto.get().getAvklartPersonstatus().getOverstyrtPersonstatus()).isEqualByComparingTo(PersonstatusType.UDEFINERT);
    }

    private Behandling lagBehandling() {
        var scenario = TestScenarioBuilder
            .builderMedSøknad(AktørId.dummy());
        return scenario.lagre(repositoryProvider);
    }
}
