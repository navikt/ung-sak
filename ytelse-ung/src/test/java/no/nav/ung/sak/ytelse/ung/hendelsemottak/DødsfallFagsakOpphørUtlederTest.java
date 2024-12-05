package no.nav.ung.sak.ytelse.ung.hendelsemottak;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.kontrakt.hendelser.DødsfallHendelse;
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.ung.sak.test.util.aktør.FiktiveFnr;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class DødsfallFagsakOpphørUtlederTest {

    public static final LocalDate STP = LocalDate.now();
    public static final LocalDate DØDSDATO = STP.plusDays(4);
    public static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();

    @Inject
    private EntityManager entityManager;
    private DødsfallFagsakOpphørUtleder utleder;

    @Inject
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    private final PersoninfoAdapter personinfoAdapter = mock(PersoninfoAdapter.class);
    private TestScenarioBuilder scenarioBuilder;


    @BeforeEach
    void setUp() {
        this.utleder = new DødsfallFagsakOpphørUtleder(new FagsakRepository(entityManager),
            new BehandlingRepository(entityManager),
            new VilkårResultatRepository(entityManager),
            new PersonopplysningRepository(entityManager),
            personinfoAdapter
        );

        scenarioBuilder = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.UNGDOMSYTELSE).medBruker(BRUKER_AKTØR_ID);

        var builder = new Personinfo.Builder();
        var søkerPersoninfoMedDødsdato = builder.medAktørId(BRUKER_AKTØR_ID)
            .medFødselsdato(STP)
            .medDødsdato(DØDSDATO)
            .medNavn("TEST TESTESEN")
            .medPersonIdent(new PersonIdent(new FiktiveFnr().nesteFnr()))
            .build();
        when(personinfoAdapter.hentPersoninfo(BRUKER_AKTØR_ID))
            .thenReturn(søkerPersoninfoMedDødsdato);
    }

    @Test
    void skal_ikke_returnere_årsak_ingen_fagsaker_for_ugyldig_aktør() {
        AktørId bruker_med_npid = AktørId.dummy();
        when(personinfoAdapter.hentPersoninfo(bruker_med_npid))
            .thenThrow(new IllegalStateException("Ugyldig aktør"));

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(bruker_med_npid);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new DødsfallHendelse(builder.build(), DØDSDATO));

        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_returnere_årsak_dersom_alle_perioder_er_avslått_i_siste_avsluttet_behandling() {

        scenarioBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.IKKE_OPPFYLT, new Periode(STP, STP.plusDays(10)));
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
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
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new DødsfallHendelse(builder.build(), DØDSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isTrue();
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
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new DødsfallHendelse(builder.build(), DØDSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isFalse();
        assertThat(fagsakBehandlingÅrsakTypeMap.get(fagsak)).isEqualTo(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER);
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
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new DødsfallHendelse(builder.build(), DØDSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isFalse();
        assertThat(fagsakBehandlingÅrsakTypeMap.get(fagsak)).isEqualTo(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER);
    }

    @Test
    void skal_returnere_årsak_dersom_alle_perioder_er_avslått_i_siste_ikke_avsluttet_behandling() {

        scenarioBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.IKKE_OPPFYLT, new Periode(STP, STP.plusDays(10)));
        scenarioBuilder.lagre(entityManager);
        var fagsak = scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new DødsfallHendelse(builder.build(), DØDSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isFalse();
        assertThat(fagsakBehandlingÅrsakTypeMap.get(fagsak)).isEqualTo(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER);

    }

    @Test
    void skal_returnere_årsak_dersom_alle_perioder_er_innvilget_i_siste_behandling() {
        scenarioBuilder.leggTilVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, Utfall.OPPFYLT, new Periode(STP, STP.plusDays(10)));
        scenarioBuilder.lagre(entityManager);
        var fagsak = scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);


        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new DødsfallHendelse(builder.build(), DØDSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isFalse();
        assertThat(fagsakBehandlingÅrsakTypeMap.get(fagsak)).isEqualTo(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER);
    }
}
