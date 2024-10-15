package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.søsken;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.in;

import java.time.LocalDate;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
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

    private FinnTidslinjeForOverlappendeSøskensaker tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new FinnTidslinjeForOverlappendeSøskensaker(fagsakRepository, new FinnAktuellTidslinjeForFagsak(vilkårResultatRepository, behandlingRepository));
    }

    @Test
    void skal_ikke_finne_overlappende_fagsaker_med_kun_en_fagsak() {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusMonths(10);
        var fagsak1 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_1, null, SAKSNUMMER1, fom, tom);
        fagsakRepository.opprettNy(fagsak1);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)), fagsak1);

        var resultat = tjeneste.finnTidslinje(BRUKER, YTELSE);

        assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_finne_overlappende_fagsaker_med_to_fagsaker_uten_overlapp() {
        var fom1 = LocalDate.now();
        var tom1 = LocalDate.now().plusMonths(10);
        var fagsak1 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_1, null, SAKSNUMMER1, fom1, tom1);
        fagsakRepository.opprettNy(fagsak1);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1)), fagsak1);

        var fom2 = tom1.plusDays(1);
        var tom2 = fom2.plusDays(10);
        var fagsak2 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_2, null, SAKSNUMMER2, fom2, tom2);
        fagsakRepository.opprettNy(fagsak2);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2)), fagsak2);

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
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1), DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3)), fagsak1);

        var fom2 = tom1.plusDays(1);
        var tom2 = fom3.minusDays(1);
        var fagsak2 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_2, null, SAKSNUMMER2, fom2, tom2);
        fagsakRepository.opprettNy(fagsak2);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2)), fagsak2);

        var resultat = tjeneste.finnTidslinje(BRUKER, YTELSE);

        assertThat(resultat.isEmpty()).isTrue();
    }


    @Test
    void skal_finne_overlappende_fagsaker_med_overlappende_fagsaker_og_like_perioder() {
        var fom1 = LocalDate.now();
        var tom1 = LocalDate.now().plusMonths(10);
        var fagsak1 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_1, null, SAKSNUMMER1, fom1, tom1);
        fagsakRepository.opprettNy(fagsak1);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1)), fagsak1);


        var fagsak2 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_2, null, SAKSNUMMER2, fom1, tom1);
        fagsakRepository.opprettNy(fagsak2);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1)), fagsak2);

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
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1)), fagsak1);

        var fom2 = LocalDate.now().plusMonths(1);
        var tom2 = LocalDate.now().plusMonths(12);
        var fagsak2 = Fagsak.opprettNy(YTELSE, BRUKER, PLEIETRENGENDE_2, null, SAKSNUMMER2, fom2, tom2);
        fagsakRepository.opprettNy(fagsak2);
        opprettBehandling(List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2)), fagsak2);

        var resultat = tjeneste.finnTidslinje(BRUKER, YTELSE);

        assertThat(resultat.isEmpty()).isFalse();
        var intervaller = resultat.getLocalDateIntervals();
        assertThat(intervaller.size()).isEqualTo(1);
        var overlapp = intervaller.first();
        assertThat(overlapp.getFomDato()).isEqualTo(fom2);
        assertThat(overlapp.getTomDato()).isEqualTo(tom1);
    }


    private Behandling opprettBehandling(List<DatoIntervallEntitet> perioder, Fagsak fagsak) {
        var builder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = builder.build();
        var vilkårBuilder = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        perioder.forEach(p -> vilkårBuilder.leggTil(new VilkårPeriodeBuilder().medUtfall(Utfall.OPPFYLT).medPeriode(p.getFomDato(), p.getTomDato())));
        Vilkårene nyttResultat = Vilkårene.builder()
            .leggTil(vilkårBuilder)
            .build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
       vilkårResultatRepository.lagre(behandling.getId(), nyttResultat);
        return behandling;
    }

}
