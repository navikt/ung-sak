package no.nav.ung.ytelse.ungdomsprogramytelsen.hendelsehåndtering;

import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.revurdering.ÅrsakOgPerioder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.hendelsemottak.tjenester.FinnFagsakerForAktørTjeneste;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.ung.sak.kontrakt.hendelser.UngdomsprogramOpphørHendelse;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;


@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class UngdomsprogramOpphørFagsakTilVurderingUtlederTest {

    public static final LocalDate STP = LocalDate.now();
    public static final LocalDate OPPHØRSDATO = STP.plusDays(100);
    public static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();
    @Inject
    private EntityManager entityManager;
    private UngdomsprogramOpphørFagsakTilVurderingUtleder utleder;

    @Inject
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private TestScenarioBuilder scenarioBuilder;
    private VilkårTjeneste vilkårTjeneste;

    @BeforeEach
    void setUp() {
        var fagsakRepository = new FagsakRepository(entityManager);
        vilkårTjeneste = mock(VilkårTjeneste.class);
        // Default: returner oppfylte vilkår for hele perioden (STP til langt fram)
        var oppfyltSamlet = new VilkårUtfallSamlet(Utfall.OPPFYLT, List.of());
        when(vilkårTjeneste.samletVilkårsresultat(anyLong()))
            .thenReturn(new LocalDateTimeline<>(STP, STP.plusYears(2), oppfyltSamlet));

        this.utleder = new UngdomsprogramOpphørFagsakTilVurderingUtleder(
            new BehandlingRepository(entityManager),
            new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository),
            new FinnFagsakerForAktørTjeneste(entityManager, fagsakRepository),
            vilkårTjeneste
        );
        scenarioBuilder = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.UNGDOMSYTELSE)
            .medBruker(BRUKER_AKTØR_ID);
    }


    @Test
    void skal_ikke_returnere_årsak_dersom_det_ikke_finnes_fagsak_for_person() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(AktørId.dummy());
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_returnere_årsak_dersom_ungdomsprogramperiode_sluttdato_er_lik_opphørsdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, OPPHØRSDATO))));

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));


        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_returnere_årsak_dersom_opphørsdato_er_lik_periodeMaksDato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        // Periode som strekker seg forbi opphørsdato, men periodeMaksDato == opphørsdato (naturlig avslutning)
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, OPPHØRSDATO.plusDays(50)))),
            false,
            OPPHØRSDATO);

        behandling.avsluttBehandling();
        entityManager.flush();

        // Naturlig avslutning: vilkår evaluert kun opp til og med opphørsdato (ikke utover maksdato)
        var oppfyltSamlet = new VilkårUtfallSamlet(Utfall.OPPFYLT, List.of());
        when(vilkårTjeneste.samletVilkårsresultat(anyLong()))
            .thenReturn(new LocalDateTimeline<>(STP, OPPHØRSDATO, oppfyltSamlet));

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));

        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_returnere_årsak_dersom_kun_ikke_oppfylte_vilkårsperioder_etter_maksdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        final var gammelOpphørsdato = OPPHØRSDATO.plusDays(10);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, gammelOpphørsdato))),
            false,
            OPPHØRSDATO);

        behandling.avsluttBehandling();
        entityManager.flush();

        // Vilkår: OPPFYLT opp til maksdato, IKKE_OPPFYLT etter — ingen revurdering forventet
        var oppfyltSamlet = new VilkårUtfallSamlet(Utfall.OPPFYLT, List.of());
        var ikkeOppfyltSamlet = new VilkårUtfallSamlet(Utfall.IKKE_OPPFYLT, List.of());
        when(vilkårTjeneste.samletVilkårsresultat(anyLong()))
            .thenReturn(new LocalDateTimeline<>(List.of(
                new no.nav.fpsak.tidsserie.LocalDateSegment<>(STP, OPPHØRSDATO, oppfyltSamlet),
                new no.nav.fpsak.tidsserie.LocalDateSegment<>(OPPHØRSDATO.plusDays(1), gammelOpphørsdato, ikkeOppfyltSamlet)
            )));

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));

        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isTrue();
    }

    @Test
    void skal_returnere_årsak_dersom_oppfylte_vilkårsperioder_finnes_etter_maksdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        final var gammelOpphørsdato = OPPHØRSDATO.plusDays(10);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, gammelOpphørsdato))),
            false,
            OPPHØRSDATO);

        behandling.avsluttBehandling();
        entityManager.flush();

        var vilkåreneMock = mock(Vilkårene.class);
        var oppfyltPeriode = mock(VilkårPeriode.class);
        when(oppfyltPeriode.getGjeldendeUtfall()).thenReturn(Utfall.OPPFYLT);
        when(vilkåreneMock.getVilkårTimeline(VilkårType.UNGDOMSPROGRAMVILKÅRET))
            .thenReturn(new LocalDateTimeline<>(OPPHØRSDATO.plusDays(1), gammelOpphørsdato, oppfyltPeriode));
        when(vilkårTjeneste.hentHvisEksisterer(anyLong())).thenReturn(Optional.of(vilkåreneMock));

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));

        validerHarÅrsak(fagsakBehandlingÅrsakTypeMap, DatoIntervallEntitet.fraOgMedTilOgMed(OPPHØRSDATO.plusDays(1), gammelOpphørsdato));
    }

    @Test
    void skal_returnere_årsak_selv_om_opphørsdato_ikke_treffer_fagsakperiode() {
        var behandling = scenarioBuilder.lagre(entityManager);
        var fagsak = scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        final var tidligereOpphørsdato = OPPHØRSDATO.minusDays(10);

        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, tidligereOpphørsdato))),
            false,
            OPPHØRSDATO.plusDays(30));

        // Simulerer sak der fagsakperioden ikke overlapper hendelsesdato, men vi fortsatt skal finne siste fagsak.
        behandlingRepositoryProvider.getFagsakRepository().oppdaterPeriode(fagsak.getId(), STP, tidligereOpphørsdato);

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));

        validerHarÅrsak(fagsakBehandlingÅrsakTypeMap, DatoIntervallEntitet.fraOgMedTilOgMed(tidligereOpphørsdato.plusDays(1), OPPHØRSDATO));
    }


    @Test
    void skal_returnere_årsak_dersom_ungdomsprogramperiode_sluttdato_er_etter_opphørsdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        final var gammelOpphørsdato = OPPHØRSDATO.plusDays(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, gammelOpphørsdato))));

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));


        validerHarÅrsak(fagsakBehandlingÅrsakTypeMap, DatoIntervallEntitet.fraOgMedTilOgMed(OPPHØRSDATO.plusDays(1), gammelOpphørsdato));
    }

    @Test
    void skal_returnere_årsak_dersom_ungdomsprogramperiode_sluttdato_er_før_opphørsdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        final var gammelOpphørsdato = OPPHØRSDATO.minusDays(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, gammelOpphørsdato))));

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));


        validerHarÅrsak(fagsakBehandlingÅrsakTypeMap, DatoIntervallEntitet.fraOgMedTilOgMed(gammelOpphørsdato.plusDays(1), OPPHØRSDATO));
    }

    @Test
    void skal_returnere_årsak_dersom_en_ungdomsprogramperiode_som_går_over_opphørsdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        final var gammelOpphørsdato = OPPHØRSDATO.plusDays(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, gammelOpphørsdato))));

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));


        validerHarÅrsak(fagsakBehandlingÅrsakTypeMap, DatoIntervallEntitet.fraOgMedTilOgMed(OPPHØRSDATO.plusDays(1), gammelOpphørsdato));
    }

    @Test
    void skal_ikke_returnere_årsak_dersom_ingen_oppfylte_vilkår_etter_opphørsdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        // Programperiode som strekker seg forbi opphørsdato
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, OPPHØRSDATO.plusDays(10)))));

        behandling.avsluttBehandling();
        entityManager.flush();

        // Overstyr mock: vilkårsresultat oppfylt FØR opphørsdato, IKKE_OPPFYLT etter (f.eks. pga aldersvilkår-avslag)
        var oppfyltSamlet = new VilkårUtfallSamlet(Utfall.OPPFYLT, List.of());
        var ikkeOppfyltSamlet = new VilkårUtfallSamlet(Utfall.IKKE_OPPFYLT, List.of());
        var tidslinje = new LocalDateTimeline<>(STP, OPPHØRSDATO, oppfyltSamlet)
            .crossJoin(new LocalDateTimeline<>(OPPHØRSDATO.plusDays(1), OPPHØRSDATO.plusDays(10), ikkeOppfyltSamlet));
        when(vilkårTjeneste.samletVilkårsresultat(anyLong()))
            .thenReturn(tidslinje);

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));

        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_returnere_årsak_dersom_vilkårsresultat_ikke_dekker_perioden_etter_opphørsdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        // Programperiode som strekker seg forbi opphørsdato
        final var gammelOpphørsdato = OPPHØRSDATO.plusDays(10);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, gammelOpphørsdato))));

        behandling.avsluttBehandling();
        entityManager.flush();

        // Overstyr mock: vilkårsresultat kun evaluert FØR opphørsdato (ikke evaluert etter)
        var oppfyltSamlet = new VilkårUtfallSamlet(Utfall.OPPFYLT, List.of());
        when(vilkårTjeneste.samletVilkårsresultat(anyLong()))
            .thenReturn(new LocalDateTimeline<>(STP, OPPHØRSDATO, oppfyltSamlet));

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));

        // Vilkårsresultatet dekker ikke perioden etter opphørsdato → ingen kjent aktiv ytelse → ignorerer hendelse
        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_returnere_årsak_dersom_vilkårsresultat_er_tomt() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        // Programperiode som strekker seg forbi opphørsdato
        final var gammelOpphørsdato = OPPHØRSDATO.plusDays(5);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, gammelOpphørsdato))));

        behandling.avsluttBehandling();
        entityManager.flush();

        // Overstyr mock: vilkårsresultat er tomt (vilkår ikke vurdert ennå)
        when(vilkårTjeneste.samletVilkårsresultat(anyLong()))
            .thenReturn(LocalDateTimeline.empty());

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));

        // Tomt vilkårsresultat → ingen kjent aktiv ytelse → ignorerer hendelse
        assertThat(fagsakBehandlingÅrsakTypeMap.isEmpty()).isTrue();
    }

    @Test
    void skal_returnere_årsak_dersom_periodeMaksDato_er_ulik_opphørsdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        // Periode som strekker seg forbi opphørsdato, med periodeMaksDato != opphørsdato
        final var gammelOpphørsdato = OPPHØRSDATO.plusDays(10);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, gammelOpphørsdato))),
            false,
            OPPHØRSDATO.plusDays(20)); // maksdato er lenger enn opphørsdato

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));

        // periodeMaksDato != opphørsdato, så sjekk 1 treffer ikke → skal opprette revurdering
        validerHarÅrsak(fagsakBehandlingÅrsakTypeMap, DatoIntervallEntitet.fraOgMedTilOgMed(OPPHØRSDATO.plusDays(1), gammelOpphørsdato));
    }

    @Test
    void skal_returnere_årsak_dersom_opphørsdato_er_lik_maksdato_men_programperiode_slutter_foer_maksdato() {
        var behandling = scenarioBuilder.lagre(entityManager);
        scenarioBuilder.lagreFagsak(behandlingRepositoryProvider);
        // Forlengelsesscenario: en tidligere revurdering stoppet programperioden 10 dager FØR maksdato.
        // Ny opphørshendelse setter opphørsdato = maksdato → skal opprette revurdering (forlengelse).
        final var gammelProgramTom = OPPHØRSDATO.minusDays(10);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(STP, gammelProgramTom))),
            false,
            OPPHØRSDATO); // periodeMaksDato == opphørsdato fra hendelse

        behandling.avsluttBehandling();
        entityManager.flush();

        var builder = new HendelseInfo.Builder();
        builder.leggTilAktør(BRUKER_AKTØR_ID);
        builder.medHendelseId("1");
        builder.medOpprettet(LocalDateTime.now());
        var fagsakBehandlingÅrsakTypeMap = utleder.finnFagsakerTilVurdering(new UngdomsprogramOpphørHendelse(builder.build(), OPPHØRSDATO));

        // Programperiode ender FØR maksdato → forlengelse → skal opprette revurdering
        validerHarÅrsak(fagsakBehandlingÅrsakTypeMap, DatoIntervallEntitet.fraOgMedTilOgMed(gammelProgramTom.plusDays(1), OPPHØRSDATO));
    }

    private static void validerHarÅrsak(Map<Fagsak, List<ÅrsakOgPerioder>> fagsakBehandlingÅrsakTypeMap, DatoIntervallEntitet forventetPeriode) {
        assertThat(fagsakBehandlingÅrsakTypeMap.keySet().size()).isEqualTo(1);
        assertThat(fagsakBehandlingÅrsakTypeMap.values().iterator().next().getFirst().behandlingÅrsak()).isEqualTo(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        assertThat(fagsakBehandlingÅrsakTypeMap.values().iterator().next().getFirst().perioder().iterator().next()).isEqualTo(forventetPeriode);

    }

}
