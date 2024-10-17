package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.søsken;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.AnnenPart;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utenlandsopphold;
import no.nav.pleiepengerbarn.uttak.kontrakter.UtenlandsoppholdÅrsak;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class FinnTidslinjeForOverlappendeSøskensakerTest {

    public static final AktørId BRUKER = AktørId.dummy();
    public static final Saksnummer SAKSNUMMER1 = new Saksnummer("123");
    public static final AktørId PLEIETRENGENDE_1 = AktørId.dummy();

    public static final Saksnummer SAKSNUMMER2 = new Saksnummer("456");
    public static final AktørId PLEIETRENGENDE_2 = AktørId.dummy();
    public static final FagsakYtelseType YTELSE = FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private VilkårResultatRepository vilkårResultatRepository;

    private UttakTjeneste uttakTjeneste = mock(UttakTjeneste.class);

    private FinnTidslinjeForOverlappendeSøskensaker tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new FinnTidslinjeForOverlappendeSøskensaker(fagsakRepository,
            new FinnAktuellTidslinjeForFagsak(vilkårResultatRepository, behandlingRepository, uttakTjeneste));
    }

    @Test
    void skal_ikke_finne_overlappende_fagsaker_med_kun_en_fagsak() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusMonths(10);
        var fagsak1 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_1, null, SAKSNUMMER1, fom, tom);
        fagsakRepository.opprettNy(fagsak1);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)), fagsak1, BehandlingStatus.UTREDES);

        var resultat = tjeneste.finnTidslinje(BRUKER, YTELSE);

        assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_finne_overlappende_fagsaker_med_to_fagsaker_uten_overlapp() {
        var fom1 = LocalDate.now();
        var tom1 = LocalDate.now().plusMonths(10);
        var fagsak1 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_1, null, SAKSNUMMER1, fom1, tom1);
        fagsakRepository.opprettNy(fagsak1);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1)), fagsak1, BehandlingStatus.UTREDES);

        var fom2 = tom1.plusDays(1);
        var tom2 = fom2.plusDays(10);
        var fagsak2 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_2, null, SAKSNUMMER2, fom2, tom2);
        fagsakRepository.opprettNy(fagsak2);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2)), fagsak2, BehandlingStatus.UTREDES);

        var resultat = tjeneste.finnTidslinje(BRUKER, YTELSE);

        assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_finne_overlappende_fagsaker_med_to_fagsaker_uten_overlapp_med_mellomliggende_periode() {
        var fom1 = LocalDate.now();
        var tom1 = LocalDate.now().plusMonths(1);
        var fom3 = fom1.plusMonths(2);
        var tom3 = fom1.plusMonths(3);
        var fagsak1 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_1, null, SAKSNUMMER1, fom1, tom3);
        fagsakRepository.opprettNy(fagsak1);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1), DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3)), fagsak1, BehandlingStatus.UTREDES);

        var fom2 = tom1.plusDays(1);
        var tom2 = fom3.minusDays(1);
        var fagsak2 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_2, null, SAKSNUMMER2, fom2, tom2);
        fagsakRepository.opprettNy(fagsak2);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2)), fagsak2, BehandlingStatus.UTREDES);

        var resultat = tjeneste.finnTidslinje(BRUKER, YTELSE);

        assertThat(resultat.isEmpty()).isTrue();
    }


    @Test
    void skal_finne_overlappende_fagsaker_med_overlappende_fagsaker_og_like_perioder() {
        var fom1 = LocalDate.now();
        var tom1 = LocalDate.now().plusMonths(10);
        var fagsak1 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_1, null, SAKSNUMMER1, fom1, tom1);
        fagsakRepository.opprettNy(fagsak1);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1)), fagsak1, BehandlingStatus.UTREDES);


        var fagsak2 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_2, null, SAKSNUMMER2, fom1, tom1);
        fagsakRepository.opprettNy(fagsak2);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1)), fagsak2, BehandlingStatus.UTREDES);

        var resultat = tjeneste.finnTidslinje(BRUKER, YTELSE);

        assertThat(resultat.isEmpty()).isFalse();
        var intervaller = resultat.getLocalDateIntervals();
        assertThat(intervaller.size()).isEqualTo(1);
        var overlapp = intervaller.first();
        assertThat(overlapp.getFomDato()).isEqualTo(fom1);
        assertThat(overlapp.getTomDato()).isEqualTo(tom1);
    }

    @Test
    void skal_ikke_finne_overlappende_fagsaker_med_overlappende_fagsaker_og_like_perioder_dersom_den_ene_har_0_uttaksgrad() {
        var fom1 = LocalDate.now();
        var tom1 = LocalDate.now().plusMonths(10);
        var fagsak1 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_1, null, SAKSNUMMER1, fom1, tom1);
        fagsakRepository.opprettNy(fagsak1);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1)), fagsak1, BehandlingStatus.UTREDES);


        var fagsak2 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_2, null, SAKSNUMMER2, fom1, tom1);
        fagsakRepository.opprettNy(fagsak2);
        var behandling2 = opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1)), fagsak2, BehandlingStatus.AVSLUTTET);
        lagreAvslåttUttak(behandling2, fom1, tom1);

        var resultat = tjeneste.finnTidslinje(BRUKER, YTELSE);

        assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    void skal_finne_overlappende_fagsaker_med_overlappende_fagsaker_og_like_perioder_den_ene_har_innvilget_uttak() {
        var fom1 = LocalDate.now();
        var tom1 = LocalDate.now().plusMonths(10);
        var fagsak1 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_1, null, SAKSNUMMER1, fom1, tom1);
        fagsakRepository.opprettNy(fagsak1);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1)), fagsak1, BehandlingStatus.UTREDES);


        var fagsak2 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_2, null, SAKSNUMMER2, fom1, tom1);
        fagsakRepository.opprettNy(fagsak2);
        var behandling2 = opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1)), fagsak2, BehandlingStatus.AVSLUTTET);
        lagreInnvilgetUttak(behandling2, fom1, tom1);

        var resultat = tjeneste.finnTidslinje(BRUKER, YTELSE);

        assertThat(resultat.isEmpty()).isFalse();
        var intervaller = resultat.getLocalDateIntervals();
        assertThat(intervaller.size()).isEqualTo(1);
        var overlapp = intervaller.first();
        assertThat(overlapp.getFomDato()).isEqualTo(fom1);
        assertThat(overlapp.getTomDato()).isEqualTo(tom1);
    }

    @Test
    void skal_finne_overlappende_fagsaker_med_overlappende_fagsaker_og_delvis_overlappende_perioder() {
        var fom1 = LocalDate.now();
        var tom1 = LocalDate.now().plusMonths(10);
        var fagsak1 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_1, null, SAKSNUMMER1, fom1, tom1);
        fagsakRepository.opprettNy(fagsak1);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1)), fagsak1, BehandlingStatus.UTREDES);

        var fom2 = LocalDate.now().plusMonths(1);
        var tom2 = LocalDate.now().plusMonths(12);
        var fagsak2 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_2, null, SAKSNUMMER2, fom2, tom2);
        fagsakRepository.opprettNy(fagsak2);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2)), fagsak2, BehandlingStatus.UTREDES);

        var resultat = tjeneste.finnTidslinje(BRUKER, YTELSE);

        assertThat(resultat.isEmpty()).isFalse();
        var intervaller = resultat.getLocalDateIntervals();
        assertThat(intervaller.size()).isEqualTo(1);
        var overlapp = intervaller.first();
        assertThat(overlapp.getFomDato()).isEqualTo(fom2);
        assertThat(overlapp.getTomDato()).isEqualTo(tom1);
    }


    private Behandling opprettBehandling(List<DatoIntervallEntitet> perioder, Fagsak fagsak, BehandlingStatus behandlingStatus) {
        var builder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = builder.medBehandlingStatus(behandlingStatus).build();
        var vilkårBuilder = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        perioder.forEach(p -> vilkårBuilder.leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.OPPFYLT).medPeriode(p.getFomDato(), p.getTomDato())));
        Vilkårene nyttResultat = Vilkårene.builder()
            .leggTil(vilkårBuilder)
            .build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
       vilkårResultatRepository.lagre(behandling.getId(), nyttResultat);
        return behandling;
    }



    private void lagreAvslåttUttak(Behandling behandling, LocalDate fom, LocalDate tom) {
        var periode = new LukketPeriode(fom, tom);
        UttaksperiodeInfo uttaksperiodeInfo = new UttaksperiodeInfo(no.nav.pleiepengerbarn.uttak.kontrakter.Utfall.IKKE_OPPFYLT,
            BigDecimal.ZERO, null, null, List.of(), BigDecimal.ZERO, null, Set.of(), Map.of(), BigDecimal.valueOf(100), null, Set.of(),
            behandling.getUuid().toString(), AnnenPart.ALENE, null, null, null, false, new Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN), false);
        var uttaksplan = new Uttaksplan(Map.of(periode, uttaksperiodeInfo), List.of());
        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true)).thenReturn(uttaksplan);
    }

    private void lagreInnvilgetUttak(Behandling behandling, LocalDate fom, LocalDate tom) {
        var periode = new LukketPeriode(fom, tom);
        UttaksperiodeInfo uttaksperiodeInfo = new UttaksperiodeInfo(no.nav.pleiepengerbarn.uttak.kontrakter.Utfall.OPPFYLT,
            BigDecimal.valueOf(100), null, null, List.of(), BigDecimal.valueOf(100), null, Set.of(), Map.of(), BigDecimal.valueOf(100), null, Set.of(),
            behandling.getUuid().toString(), AnnenPart.ALENE, null, null, null, false, new Utenlandsopphold(null, UtenlandsoppholdÅrsak.INGEN), false);
        var uttaksplan = new Uttaksplan(Map.of(periode, uttaksperiodeInfo), List.of());
        when(uttakTjeneste.hentUttaksplan(behandling.getUuid(), true)).thenReturn(uttaksplan);
    }


}
