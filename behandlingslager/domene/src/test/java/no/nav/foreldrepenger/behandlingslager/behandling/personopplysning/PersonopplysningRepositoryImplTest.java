package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class PersonopplysningRepositoryImplTest {

    @Rule
    public RepositoryRule repositoryRule = new UnittestRepositoryRule();

    private PersonopplysningRepository repository = new PersonopplysningRepository(repositoryRule.getEntityManager());
    private FagsakRepository fagsakRepository = new FagsakRepository(repositoryRule.getEntityManager());
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private Map<Landkoder, Region> landRegion;

    @Before
    public void setUp() throws Exception {
        landRegion = new HashMap<>();
        landRegion.put(Landkoder.NOR, Region.NORDEN);
    }

    @Test
    public void skal_hente_eldste_versjon_av_aggregat() {
        final Personinfo personinfo = lagPerson();
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNy(personinfo));
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
