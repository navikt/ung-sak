package no.nav.k9.sak.behandlingslager.behandling.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class PersonopplysningRepositoryImplTest {

    @Inject
    private EntityManager entityManager;

    private PersonopplysningRepository repository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private Map<Landkoder, Region> landRegion;

    @BeforeEach
    public void setUp() throws Exception {

        repository = new PersonopplysningRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();

        landRegion = new HashMap<>();
        landRegion.put(Landkoder.NOR, Region.NORDEN);
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
        PersonInformasjonBuilder informasjonBuilder = repository.opprettBuilderForRegisterdata(behandlingId);
        PersonInformasjonBuilder.PersonopplysningBuilder personopplysningBuilder = informasjonBuilder.getPersonopplysningBuilder(personinfo.getAktørId());
        personopplysningBuilder.medNavn(personinfo.getNavn())
            .medFødselsdato(personinfo.getFødselsdato())
            .medSivilstand(personinfo.getSivilstandType())
            .medRegion(Region.NORDEN);
        informasjonBuilder.leggTil(personopplysningBuilder);
        repository.lagre(behandlingId, informasjonBuilder);

        informasjonBuilder = repository.opprettBuilderForRegisterdata(behandlingId);
        personopplysningBuilder = informasjonBuilder.getPersonopplysningBuilder(personinfo.getAktørId());
        personopplysningBuilder.medNavn(personinfo.getNavn())
            .medFødselsdato(personinfo.getFødselsdato())
            .medSivilstand(personinfo.getSivilstandType())
            .medRegion(Region.NORDEN)
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
        return new PersonopplysningerAggregat(grunnlag, behandling.getAktørId(), DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), landRegion);
    }

    private Personinfo lagPerson() {
        final Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AktørId.dummy())
            .medSivilstandType(SivilstandType.SAMBOER)
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12345678901"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
        return personinfo;
    }
}
