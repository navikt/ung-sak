package no.nav.ung.sak.behandlingslager.behandling.personopplysning;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class PersonopplysningRepositoryImplTest {

    @Inject
    private EntityManager entityManager;

    private PersonopplysningRepository repository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void setUp() throws Exception {

        repository = new PersonopplysningRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Test
    public void skal_hente_eldste_versjon_av_aggregat() {
        final Personinfo personinfo = lagPerson();
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, personinfo.getAktørId());
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        Long behandlingId = behandling.getId();
        PersonInformasjonBuilder informasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.PersonopplysningBuilder personopplysningBuilder = informasjonBuilder.getPersonopplysningBuilder(personinfo.getAktørId());
        personopplysningBuilder.medNavn(personinfo.getNavn()).medFødselsdato(personinfo.getFødselsdato());
        informasjonBuilder.leggTil(personopplysningBuilder);
        repository.lagre(behandlingId, informasjonBuilder);

        informasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        personopplysningBuilder = informasjonBuilder.getPersonopplysningBuilder(personinfo.getAktørId());
        personopplysningBuilder.medNavn(personinfo.getNavn())
            .medFødselsdato(personinfo.getFødselsdato())
            .medDødsdato(LocalDate.now());
        informasjonBuilder.leggTil(personopplysningBuilder);
        repository.lagre(behandlingId, informasjonBuilder);

        PersonopplysningerAggregat personopplysningerAggregat = tilAggregat(behandling, repository.hentPersonopplysninger(behandlingId));
        PersonopplysningerAggregat førsteVersjonPersonopplysningerAggregat = tilAggregat(behandling, repository.hentFørsteVersjonAvPersonopplysninger(behandlingId));

        assertThat(personopplysningerAggregat).isNotEqualTo(førsteVersjonPersonopplysningerAggregat);
        assertThat(personopplysningerAggregat.getSøker()).isEqualToComparingOnlyGivenFields(førsteVersjonPersonopplysningerAggregat.getSøker(), "aktørId", "navn", "fødselsdato", "region", "sivilstand", "brukerKjønn");
        assertThat(personopplysningerAggregat.getSøker()).isNotEqualTo(førsteVersjonPersonopplysningerAggregat.getSøker());
    }

    private PersonopplysningerAggregat tilAggregat(Behandling behandling, PersonopplysningGrunnlagEntitet grunnlag) {
        return new PersonopplysningerAggregat(grunnlag, behandling.getAktørId(), DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()));
    }

    private Personinfo lagPerson() {
        final Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AktørId.dummy())
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medPersonIdent(new PersonIdent("12345678901"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
        return personinfo;
    }
}
