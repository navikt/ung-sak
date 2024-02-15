package no.nav.k9.sak.ytelse.pleiepengerbarn.hendelsemottak;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.kontrakt.hendelser.DødsfallHendelse;
import no.nav.k9.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.k9.sak.test.util.aktør.FiktiveFnr;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;


@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class DødsfallFagsakTilVurderingUtlederTest {

    public static final AktørId PLEIETRENGENDE_AKTØR_ID = AktørId.dummy();
    public static final LocalDate STP = LocalDate.now();
    public static final LocalDate DØDSDATO = STP.plusDays(4);
    public static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();
    @Inject
    private EntityManager entityManager;
    private DødsfallFagsakTilVurderingUtleder utleder;

    @Inject
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    private final PersoninfoAdapter personinfoAdapter = mock(PersoninfoAdapter.class);
    private TestScenarioBuilder scenarioBuilder;

    @BeforeEach
    void setUp() {
        this.utleder = new DødsfallFagsakTilVurderingUtleder(new FagsakRepository(entityManager), new BehandlingRepository(entityManager), new VilkårResultatRepository(entityManager), new PersonopplysningRepository(entityManager),
            personinfoAdapter, true);
        initScenarioDødsdatoPleietrengende();

    }

    @Test
    void skal_ikke_returnere_årsak_dersom_alle_perioder_er_avslått_i_siste_avsluttet_behandling() {

        scenarioBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.IKKE_OPPFYLT, new Periode(STP, STP.plusDays(10)));
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(PLEIETRENGENDE_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new DødsfallHendelse(builder.build(), DØDSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isTrue();

    }

    @Test
    void skal_ikke_returnere_årsak_dersom_kun_innvilget_før_dødsdato() {

        scenarioBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.IKKE_OPPFYLT, new Periode(STP, STP.plusDays(10)));
        var stp2 = STP.minusMonths(1);
        scenarioBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.OPPFYLT, new Periode(stp2, stp2.plusDays(1)));

        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(PLEIETRENGENDE_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new DødsfallHendelse(builder.build(), DØDSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isTrue();
    }

    @Test
    void skal_returnere_årsak_dersom_innvilget_tom_dødsdato() {

        scenarioBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.OPPFYLT, new Periode(STP, DØDSDATO));
        scenarioBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.IKKE_OPPFYLT, new Periode(DØDSDATO.plusDays(1), DØDSDATO.plusDays(10)));

        var behandling = scenarioBuilder.lagre(entityManager);
        var fagsak = scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(PLEIETRENGENDE_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new DødsfallHendelse(builder.build(), DØDSDATO));



        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isFalse();
        assertThat(fagsakBehandlingÅrsakTypeMap.get(fagsak)).isEqualTo(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN);
    }

    @Test
    void skal_returnere_årsak_dersom_innvilget_fom_dødsdato() {

        scenarioBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.IKKE_OPPFYLT, new Periode(STP, DØDSDATO.minusDays(2)));
        scenarioBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.OPPFYLT, new Periode(DØDSDATO, DØDSDATO.plusDays(10)));

        var behandling = scenarioBuilder.lagre(entityManager);
        var fagsak = scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(PLEIETRENGENDE_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new DødsfallHendelse(builder.build(), DØDSDATO));



        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isFalse();
        assertThat(fagsakBehandlingÅrsakTypeMap.get(fagsak)).isEqualTo(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN);
    }
    @Test
    void skal_returnere_årsak_dersom_alle_perioder_er_avslått_i_siste_ikke_avsluttet_behandling() {

        scenarioBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.IKKE_OPPFYLT, new Periode(STP, STP.plusDays(10)));
        scenarioBuilder.lagre(entityManager);
        var fagsak = scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(PLEIETRENGENDE_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new DødsfallHendelse(builder.build(), DØDSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isFalse();
        assertThat(fagsakBehandlingÅrsakTypeMap.get(fagsak)).isEqualTo(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN);

    }

    @Test
    void skal_returnere_årsak_dersom_alle_perioder_er_innvilget_i_siste_behandling() {
        scenarioBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.OPPFYLT, new Periode(STP, STP.plusDays(10)));
        scenarioBuilder.lagre(entityManager);
        var fagsak = scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);


        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(PLEIETRENGENDE_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new DødsfallHendelse(builder.build(), DØDSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isFalse();
        assertThat(fagsakBehandlingÅrsakTypeMap.get(fagsak)).isEqualTo(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN);
    }

    private void initScenarioDødsdatoPleietrengende() {
        var builder = new Personinfo.Builder();
        var pleietrengendePersoninfoMedDødsdato = builder.medAktørId(PLEIETRENGENDE_AKTØR_ID)
            .medFødselsdato(STP)
            .medDødsdato(DØDSDATO)
            .medNavn("TEST TESTESEN")
            .medPersonIdent(new PersonIdent(new FiktiveFnr().nesteBarnFnr()))
            .build();
        when(personinfoAdapter.hentPersoninfo(PLEIETRENGENDE_AKTØR_ID))
            .thenReturn(pleietrengendePersoninfoMedDødsdato);

        var brukerPersoninfoUtenDødsdato = builder.medAktørId(BRUKER_AKTØR_ID)
            .medFødselsdato(STP.minusYears(20))
            .medPersonIdent(new PersonIdent(new FiktiveFnr().nesteMannFnr()))
            .medNavn("TEST TESTESEN")
            .build();
        when(personinfoAdapter.hentPersoninfo(BRUKER_AKTØR_ID))
            .thenReturn(brukerPersoninfoUtenDødsdato);

        scenarioBuilder = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
            .medBruker(BRUKER_AKTØR_ID)
            .medPleietrengende(PLEIETRENGENDE_AKTØR_ID);
        var builderForRegisteropplysninger = scenarioBuilder.opprettBuilderForRegisteropplysninger();
        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .barn(PLEIETRENGENDE_AKTØR_ID, STP)
            .relasjonTil(BRUKER_AKTØR_ID, RelasjonsRolleType.FARA, null)
            .build();
        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(BRUKER_AKTØR_ID, SivilstandType.GIFT)
            .statsborgerskap(Landkoder.NOR)
            .personstatus(PersonstatusType.BOSA)
            .relasjonTil(PLEIETRENGENDE_AKTØR_ID, RelasjonsRolleType.BARN, null)
            .build();
        scenarioBuilder.medRegisterOpplysninger(søker);
        scenarioBuilder.medRegisterOpplysninger(fødtBarn);
    }

}
