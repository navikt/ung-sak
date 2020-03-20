package no.nav.k9.sak.behandlingslager.behandling.medlemskap;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.medlem.MedlemskapType;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class MedlemskapRepositoryImplTest {

    @Rule
    public RepositoryRule repositoryRule = new UnittestRepositoryRule();

    private MedlemskapRepository repository = new MedlemskapRepository(repositoryRule.getEntityManager());
    private FagsakRepository fagsakRepository = new FagsakRepository(repositoryRule.getEntityManager());
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    @Test
    public void skal_hente_eldste_versjon_av_aggregat() {
        Behandling behandling = lagBehandling();
        MedlemskapPerioderEntitet perioder = new MedlemskapPerioderBuilder().medMedlemskapType(MedlemskapType.FORELOPIG).build();
        Long behandlingId = behandling.getId();
        repository.lagreMedlemskapRegisterOpplysninger(behandlingId, List.of(perioder));

        perioder = new MedlemskapPerioderBuilder().medMedlemskapType(MedlemskapType.ENDELIG).build();
        repository.lagreMedlemskapRegisterOpplysninger(behandlingId, List.of(perioder));

        Optional<MedlemskapAggregat> medlemskapAggregat = repository.hentMedlemskap(behandlingId);
        Optional<MedlemskapAggregat> førsteVersjonMedlemskapAggregat = repository.hentFørsteVersjonAvMedlemskap(behandlingId);

        MedlemskapPerioderEntitet perioderEntitet = medlemskapAggregat.get().getRegistrertMedlemskapPerioder()
                .stream().findFirst().get();
        MedlemskapPerioderEntitet førstePerioderEntitet = førsteVersjonMedlemskapAggregat.get()
                .getRegistrertMedlemskapPerioder().stream().findFirst().get();

        assertThat(medlemskapAggregat.get()).isNotEqualTo(førsteVersjonMedlemskapAggregat.get());
        assertThat(perioderEntitet.getMedlemskapType()).isEqualTo(MedlemskapType.ENDELIG);
        assertThat(førstePerioderEntitet.getMedlemskapType()).isEqualTo(MedlemskapType.FORELOPIG);
    }

    @Test
    public void skal_lagre_vurdering_av_løpende_medlemskap() {
        Behandling behandling = lagBehandling();
        LocalDate vurderingsdato = LocalDate.now();
        VurdertMedlemskapPeriodeEntitet.Builder builder = new VurdertMedlemskapPeriodeEntitet.Builder();
        VurdertLøpendeMedlemskapBuilder løpendeMedlemskapBuilder = builder.getBuilderFor(vurderingsdato);

        løpendeMedlemskapBuilder.medBosattVurdering(true);
        løpendeMedlemskapBuilder.medVurderingsdato(LocalDate.now());

        builder.leggTil(løpendeMedlemskapBuilder);

        VurdertMedlemskapPeriodeEntitet hvaSkalLagres = builder.build();
        repository.lagreLøpendeMedlemskapVurdering(behandling.getId(), hvaSkalLagres);

        Optional<MedlemskapAggregat> medlemskapAggregat = repository.hentMedlemskap(behandling.getId());
        assertThat(medlemskapAggregat).isPresent();
        assertThat(medlemskapAggregat.get().getVurderingLøpendeMedlemskap()).contains(hvaSkalLagres);
    }

    private Behandling lagBehandling() {
        final Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AktørId.dummy())
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12345678901"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, personinfo.getAktørId());
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }
}
