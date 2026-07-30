package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.ytelseperioder.KvalifiserteYtelsesperioderTjeneste;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tester for å finne fagsaker som skal kontrolleres for inntekt i ulike scenarier.
 * Verifiserer at fagsaker blir funnet eller ikke funnet avhengig av programperiode og triggere.
 */
@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class FinnSakerForInntektkontrollTest {

    public static final LocalDate FØRSTE_SEPTEMBER = LocalDate.of(2025, 9, 1);
    public static final LocalDate MIDT_I_SEPTEMBER = LocalDate.of(2025, 9, 15);
    public static final LocalDate LANGT_FRAM = LocalDate.of(2026, 7, 1);
    public static final LocalDate LANGT_BAK = LocalDate.of(2025, 1, 1);
    public static final LocalDate FØRSTE_AUGUST = LocalDate.of(2025, 8, 1);
    public static final LocalDate SISTE_DAG_I_SEPTEMBER = LocalDate.of(2025, 9, 30);
    public static final LocalDate MIDT_I_OKTOBER = LocalDate.of(2025, 10, 15);

    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    @Inject
    private VilkårResultatRepository vilkårResultatRepository;
    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;
    @Inject
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    @Inject
    private TilkjentYtelseRepository tilkjentYtelseRepository;

    @Inject
    private EntityManager entityManager;

    @Inject
    private RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder;

    private FinnSakerForInntektkontroll finnSakerForInntektkontroll;

    private Behandling behandling;
    private KvalifiserteYtelsesperioderTjeneste kvalifiserteYtelsesperioderTjeneste;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;

    @BeforeEach
    void setUp() {
        kvalifiserteYtelsesperioderTjeneste = Mockito.mock(KvalifiserteYtelsesperioderTjeneste.class);
        prosessTriggerPeriodeUtleder = Mockito.mock(ProsessTriggerPeriodeUtleder.class);
        when(prosessTriggerPeriodeUtleder.utledTidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());
        when(kvalifiserteYtelsesperioderTjeneste.finnPeriodeTidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());
        finnSakerForInntektkontroll = new FinnSakerForInntektkontroll(
            behandlingRepository,
            fagsakRepository,
            new VilkårTjeneste(behandlingRepository, null, vilkårResultatRepository),
            tilkjentYtelseRepository,
            new RelevanteKontrollperioderUtleder(
                new UnitTestLookupInstanceImpl<>(prosessTriggerPeriodeUtleder),
                new MånedsvisTidslinjeUtleder(new UnitTestLookupInstanceImpl<>(kvalifiserteYtelsesperioderTjeneste), behandlingRepository),
                behandlingRepository)
        );

        behandling = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.UNGDOMSYTELSE)
            .lagre(entityManager);
    }

    /**
     * Skal ikke finne fagsak uten programperiode.
     * Tester at ingen fagsaker returneres når det ikke finnes noen programperiode.
     */
    @Test
    void skal_ikke_finne_fagsak_uten_programperiode() {
        // Arrange
        // Ingen programperiode opprettes

        // Act
        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();

        // Assert
        assertEquals(0, fagsaker.size());
    }

    /**
     * Skal ikke finne fagsak for kontroll av første måned i programperiode.
     * Tester at ingen fagsaker returneres når programperioden starter i september.
     */
    @Test
    void skal_ikke_finne_fagsak_for_kontroll_av_første_måned_i_programperiode() {
        // Arrange
        opprettPeriode(FØRSTE_SEPTEMBER, LANGT_FRAM);

        // Act
        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();

        // Assert
        assertEquals(0, fagsaker.size());
    }

    /**
     * Skal finne fagsak for kontroll av andre måned i programperiode.
     * Tester at én fagsak returneres når programperioden starter i august og september er andre måned.
     */
    @Test
    void skal_finne_fagsak_for_kontroll_av_andre_måned_i_programperiode() {
        // Arrange
        opprettPeriode(FØRSTE_AUGUST, LANGT_FRAM);

        // Act
        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();

        // Assert
        assertEquals(1, fagsaker.size());
    }

    /**
     * Skal ikke finne fagsak for kontroll av siste måned i programperiode.
     * Tester at ingen fagsaker returneres når programperioden slutter i september.
     */
    @Test
    void skal_finne_fagsak_for_kontroll_av_siste_måned_i_programperiode() {
        // Arrange
        opprettPeriode(LANGT_BAK, MIDT_I_SEPTEMBER);

        // Act
        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();

        // Assert
        assertEquals(1, fagsaker.size());
    }

    /**
     * Skal finne fagsak for kontroll av nest siste måned i programperiode.
     * Tester at én fagsak returneres når programperioden slutter i oktober og september er nest siste måned.
     */
    @Test
    void skal_finne_fagsak_for_kontroll_av_nest_siste_måned_i_programperiode() {
        // Arrange
        opprettPeriode(LANGT_BAK, MIDT_I_OKTOBER);

        // Act
        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();

        // Assert
        assertEquals(1, fagsaker.size());
    }

    /**
     * Skal finne fagsak for kontroll av måned innenfor en programperiode som slutter i oktober.
     * Forventer at én fagsak blir funnet.
     */
    @Test
    void skal_finne_fagsak_for_kontroll_av_måned_i_programperiode() {
        // Arrange
        opprettPeriode(LANGT_BAK, MIDT_I_OKTOBER);

        // Act
        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();

        // Assert
        assertEquals(1, fagsaker.size());
    }

    /**
     * Skal finne fagsak for kontroll av måned i en ubegrenset programperiode.
     * Forventer at én fagsak blir funnet.
     */
    @Test
    void skal_finne_fagsak_for_kontroll_av_måned_i_ubegrenset_programperiode() {
        // Arrange
        opprettPeriode(LANGT_BAK, TIDENES_ENDE);

        // Act
        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();

        // Assert
        assertEquals(1, fagsaker.size());
    }

    /**
     * Skal ikke finne fagsak dersom siste behandling har trigger for inntektskontroll.
     * Forventer at ingen fagsaker blir funnet.
     */
    @Test
    void skal_ikke_finne_fagsak_dersom_siste_behandling_har_trigger_for_inntektskontroll() {
        // Arrange
        opprettPeriode(LANGT_BAK, TIDENES_ENDE);
        when(prosessTriggerPeriodeUtleder.utledTidslinje(anyLong())).thenReturn(new LocalDateTimeline<>(FØRSTE_SEPTEMBER, SISTE_DAG_I_SEPTEMBER, Set.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)));

        // Act
        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();

        // Assert
        assertEquals(0, fagsaker.size());
    }

    /**
     * Skal finne fagsak dersom siste behandling har trigger som ikke gjelder inntektskontroll.
     * Forventer at én fagsak blir funnet.
     */
    @Test
    void skal_finne_fagsak_dersom_siste_behandling_har_trigger_som_ikke_gjelder_inntektskontroll() {
        // Arrange
        opprettPeriode(LANGT_BAK, TIDENES_ENDE);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DatoIntervallEntitet.fraOgMedTilOgMed(FØRSTE_SEPTEMBER, SISTE_DAG_I_SEPTEMBER))));

        // Act
        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();

        // Assert
        assertEquals(1, fagsaker.size());
    }

    /**
     * Skal finne fagsak dersom siste behandling har trigger for inntektskontroll i en annen måned enn kontrollmåneden.
     * Tester at fagsaken blir funnet selv om det finnes en inntektskontroll-trigger, så lenge den ikke gjelder kontrollmåneden (september).
     * Forventer at én fagsak blir funnet.
     */
    @Test
    void skal_finne_fagsak_dersom_siste_behandling_har_trigger_for_inntektskontroll_i_en_annen_måned() {
        // Arrange
        opprettPeriode(LANGT_BAK, TIDENES_ENDE);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, DatoIntervallEntitet.fraOgMedTilOgMed(FØRSTE_AUGUST, FØRSTE_AUGUST.with(TemporalAdjusters.lastDayOfMonth())))));

        // Act
        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();

        // Assert
        assertEquals(1, fagsaker.size());
    }

    // Test for at fagsak ikke blir funnet dersom det allerede finnes en kontrollert inntekt periode for september
    @Test
    void skal_ikke_finne_fagsak_dersom_det_allerede_finnes_kontrollert_inntekt_for_september() {
        // Arrange
        opprettPeriode(LANGT_BAK,TIDENES_ENDE);
        List<KontrollertInntektPeriode> kontrollertInntektForSeptember = List.of(KontrollertInntektPeriode.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(FØRSTE_SEPTEMBER, SISTE_DAG_I_SEPTEMBER))
            .medKilde(KontrollertInntektKilde.BRUKER)
            .medInntekt(BigDecimal.valueOf(1000))
            .build());
        tilkjentYtelseRepository.lagre(behandling.getId(), kontrollertInntektForSeptember);

        // Act
        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();

        // Assert
        assertEquals(0, fagsaker.size());
    }

    @Test
    void skal_finne_fagsak_dersom_det_allerede_finnes_kontrollert_inntekt_for_annen_måned() {
        // Arrange
        opprettPeriode(LANGT_BAK,TIDENES_ENDE);
        List<KontrollertInntektPeriode> kontrollertInntektForAugust = List.of(KontrollertInntektPeriode.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(FØRSTE_AUGUST, FØRSTE_AUGUST.with(TemporalAdjusters.lastDayOfMonth())))
            .medKilde(KontrollertInntektKilde.BRUKER)
            .medInntekt(BigDecimal.valueOf(1000))
            .build());
        tilkjentYtelseRepository.lagre(behandling.getId(), kontrollertInntektForAugust);

        // Act
        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();

        // Assert
        assertEquals(1, fagsaker.size());
    }

    private void opprettPeriode(LocalDate fomDato, LocalDate tomDato) {
        when(kvalifiserteYtelsesperioderTjeneste.finnPeriodeTidslinje(any()))
            .thenReturn(new LocalDateTimeline<>(fomDato, tomDato, true));
    }

    private List<Fagsak> finnFagsakerForInntektskontrollISeptember() {
        return finnSakerForInntektkontroll.finnFagsaker(FØRSTE_SEPTEMBER, SISTE_DAG_I_SEPTEMBER);
    }


}
