package no.nav.ung.sak.web.app.tjenester.behandling.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.ung.sak.kontrakt.person.PersonopplysningDto;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;


@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class PersonopplysningDtoTjenesteTest {

    @Inject
    public EntityManager entityManager;

    @Inject
    PersonopplysningTjeneste personopplysningTjeneste;

    private BehandlingRepositoryProvider repositoryProvider;
    private PersonopplysningDtoTjeneste tjeneste;

    @BeforeEach
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        tjeneste = new PersonopplysningDtoTjeneste(this.personopplysningTjeneste, new BehandlingRepository(entityManager));
    }

    @Test
    public void skal_takle_at_man_spør_etter_opplysninger_utenfor_tidsserien() {
        //sørger for at vi bommer når vi spør etter personstatus
        LocalDate enTilfeldigDato = LocalDate.of(1989, 9, 29);
        Behandling behandling = lagBehandling();

        Optional<PersonopplysningDto> personopplysningDto = tjeneste.lagPersonopplysningDto(behandling.getId(), enTilfeldigDato);

        assertThat(personopplysningDto).isPresent();
    }

    private Behandling lagBehandling() {
        var scenario = TestScenarioBuilder
            .builderMedSøknad(AktørId.dummy());
        return scenario.lagre(repositoryProvider);
    }
}
