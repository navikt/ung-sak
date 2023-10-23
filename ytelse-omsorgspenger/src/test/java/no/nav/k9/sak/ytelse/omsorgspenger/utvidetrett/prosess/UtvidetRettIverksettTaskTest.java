package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskDataBuilder;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.test.util.fagsak.FagsakBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.UtvidetRettKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.KroniskSyktBarn;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.UtvidetRett;

@CdiDbAwareTest
class UtvidetRettIverksettTaskTest {

    @Inject
    private VilkårTjeneste vilkårTjeneste;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private VilkårResultatRepository vilkårResultatRepository;

    private UtvidetRettTestKlient utvidetRettKlient = new UtvidetRettTestKlient();
    private UtvidetRettIverksettTask utvidetRettIverksettTask;

    private AktørId søker = AktørId.dummy();
    private AktørId pleietrengende = AktørId.dummy();

    @BeforeEach
    void setUp() {
        PeriodisertUtvidetRettIverksettTjeneste periodisertUtvidetRettIverksettTjeneste = new PeriodisertUtvidetRettIverksettTjeneste(vilkårResultatRepository, true);
        utvidetRettIverksettTask = new UtvidetRettIverksettTask(vilkårTjeneste, behandlingRepository, utvidetRettKlient, periodisertUtvidetRettIverksettTjeneste, true);
    }

    @Test
    void skal_sende_iverksetting_av_innvilgelse() {
        LocalDate fom = LocalDate.of(2023, 1, 1);
        LocalDate tom = LocalDate.of(2030, 12, 31);

        Fagsak fagsak = lagFagsak();
        Behandling behandling = lagFørstegangsbehandling(fagsak);
        leggTilInnvilgetVilkår(fom, tom, behandling);

        ProsessTaskData taskdata = ProsessTaskDataBuilder.forProsessTask(UtvidetRettIverksettTask.class).medProperty("behandlingId", behandling.getId().toString()).build();
        utvidetRettIverksettTask.doTask(taskdata);

        List<IverksettelseHendelse> iverksettelserSendt = utvidetRettKlient.getHendelser();
        assertThat(iverksettelserSendt).hasSize(1);
        assertThat(iverksettelserSendt.get(0).innvilgelse()).isTrue();
        KroniskSyktBarn ksb = (KroniskSyktBarn) iverksettelserSendt.get(0).hendelse();
        assertThat(ksb.getSøker().getAktørId()).isEqualTo(søker);
        assertThat(ksb.getBarn().getAktørId()).isEqualTo(pleietrengende);
        assertThat(ksb.getPeriode().getFom()).isEqualTo(fom);
        assertThat(ksb.getPeriode().getTom()).isEqualTo(tom);
        assertThat(ksb.getBehandlingUuid()).isEqualTo(behandling.getUuid());
    }

    @Test
    void skal_sende_avslag_på_tidligere_innvilget_periode_som_er_etter_ny_innvilget_periode() {
        Fagsak fagsak = lagFagsak();

        LocalDate fom1 = LocalDate.of(2023, 1, 1);
        LocalDate tom1 = LocalDate.of(2030, 12, 31);
        Behandling behandling = lagFørstegangsbehandling(fagsak);
        leggTilInnvilgetVilkår(fom1, tom1, behandling);

        Behandling revurdering = lagRevurdering(behandling);
        LocalDate fom2 = LocalDate.of(2023, 1, 1);
        LocalDate tom2 = LocalDate.of(2024, 12, 31);
        LocalDateTimeline<Utfall> resultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom2, tom2, Utfall.OPPFYLT),
            new LocalDateSegment<>(tom2.plusDays(1), tom1, Utfall.IKKE_OPPFYLT) //settes til ikke-oppfylt i AvklarUtvidetRett (aksjonspunkt-oppdaterer)
        ));
        leggTilVilkårResultat(resultat, revurdering);

        ProsessTaskData taskdata = ProsessTaskDataBuilder.forProsessTask(UtvidetRettIverksettTask.class).medProperty("behandlingId", revurdering.getId().toString()).build();
        utvidetRettIverksettTask.doTask(taskdata);

        List<IverksettelseHendelse> iverksettelserSendt = utvidetRettKlient.getHendelser();
        assertThat(iverksettelserSendt).hasSize(1);

        assertThat(iverksettelserSendt.get(0).innvilgelse()).isFalse();
        KroniskSyktBarn ksb = (KroniskSyktBarn) iverksettelserSendt.get(0).hendelse();
        assertThat(ksb.getSøker().getAktørId()).isEqualTo(søker);
        assertThat(ksb.getBarn().getAktørId()).isEqualTo(pleietrengende);
        assertThat(ksb.getPeriode().getFom()).isEqualTo(tom2.plusDays(1));
        assertThat(ksb.getPeriode().getTom()).isEqualTo(tom1);
        assertThat(ksb.getBehandlingUuid()).isEqualTo(revurdering.getUuid());
    }

    @Test
    void skal_ikke_sende_endringer_på_tidligere_innvilget_periode_som_er_før_revurderingens_periode() {
        Fagsak fagsak = lagFagsak();

        LocalDate fom1 = LocalDate.of(2023, 1, 1);
        LocalDate tom1 = LocalDate.of(2023, 12, 31);
        Behandling behandling = lagFørstegangsbehandling(fagsak);
        leggTilInnvilgetVilkår(fom1, tom1, behandling);

        Behandling revurdering = lagRevurdering(behandling);
        LocalDate fom2 = LocalDate.of(2024, 1, 1);
        LocalDate tom2 = LocalDate.of(2024, 12, 31);
        leggTilInnvilgetVilkår(fom2, tom2, revurdering);

        ProsessTaskData taskdata = ProsessTaskDataBuilder.forProsessTask(UtvidetRettIverksettTask.class).medProperty("behandlingId", revurdering.getId().toString()).build();
        utvidetRettIverksettTask.doTask(taskdata);

        List<IverksettelseHendelse> iverksettelserSendt = utvidetRettKlient.getHendelser();
        assertThat(iverksettelserSendt).hasSize(1);

        assertThat(iverksettelserSendt.get(0).innvilgelse()).isTrue();
        KroniskSyktBarn ksb = (KroniskSyktBarn) iverksettelserSendt.get(0).hendelse();
        assertThat(ksb.getSøker().getAktørId()).isEqualTo(søker);
        assertThat(ksb.getBarn().getAktørId()).isEqualTo(pleietrengende);
        assertThat(ksb.getPeriode().getFom()).isEqualTo(fom2);
        assertThat(ksb.getPeriode().getTom()).isEqualTo(tom2);
        assertThat(ksb.getBehandlingUuid()).isEqualTo(revurdering.getUuid());
    }

    @Test
    void skal_feile_dersom_det_må_sendes_over_mer_enn_en_periode_til_omsorgsdager() {
        //testen kan fjernes dersom omsorgsdager får støtte for å ta imot flere perioder for samme behandling
        Fagsak fagsak = lagFagsak();

        LocalDate fom1 = LocalDate.of(2023, 1, 1);
        LocalDate tom1 = LocalDate.of(2030, 12, 31);
        Behandling behandling = lagFørstegangsbehandling(fagsak);
        leggTilInnvilgetVilkår(fom1, tom1, behandling);

        Behandling revurdering = lagRevurdering(behandling);
        LocalDate fom2 = LocalDate.of(2022, 1, 1);
        LocalDate tom2 = LocalDate.of(2024, 12, 31);

        LocalDateTimeline<Utfall> resultat = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom2, tom2, Utfall.OPPFYLT),
            new LocalDateSegment<>(tom2.plusDays(1), tom1, Utfall.IKKE_OPPFYLT)
        ));
        leggTilVilkårResultat(resultat, revurdering);

        ProsessTaskData taskdata = ProsessTaskDataBuilder.forProsessTask(UtvidetRettIverksettTask.class).medProperty("behandlingId", revurdering.getId().toString()).build();

        IllegalStateException exception;
        try {
            utvidetRettIverksettTask.doTask(taskdata);
            exception = null;
        } catch (IllegalStateException e) {
            exception = e;
        }
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).startsWith("Kan ikke sende mer enn en periode ved iverksetting av rammevedtak");
    }

    private Behandling lagFørstegangsbehandling(Fagsak fagsak) {
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        return behandling;
    }

    private Behandling lagRevurdering(Behandling orginalBehandlign) {
        Behandling behandling = Behandling.fraTidligereBehandling(orginalBehandlign, BehandlingType.REVURDERING).build();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        return behandling;
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER_KS).medBruker(søker).medPleietrengende(pleietrengende).build();
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    private void leggTilInnvilgetVilkår(LocalDate fom, LocalDate tom, Behandling behandling) {
        vilkårResultatRepository.lagre(behandling.getId(), new VilkårResultatBuilder().leggTil(new VilkårBuilder(VilkårType.UTVIDETRETT).leggTil(new VilkårPeriodeBuilder().medPeriode(fom, tom).medBegrunnelse("joda").medUtfall(Utfall.OPPFYLT))).build());
    }

    private void leggTilVilkårResultat(LocalDateTimeline<Utfall> utfall, Behandling behandling) {
        VilkårBuilder builder = new VilkårBuilder(VilkårType.UTVIDETRETT);
        for (LocalDateSegment<Utfall> segment : utfall.toSegments()) {
            Avslagsårsak avslagsårsak = segment.getValue() == Utfall.IKKE_OPPFYLT ? Avslagsårsak.IKKE_UTVIDETRETT : null;
            builder.leggTil(new VilkårPeriodeBuilder().medPeriode(segment.getFom(), segment.getTom()).medBegrunnelse("joda").medUtfall(segment.getValue()).medAvslagsårsak(avslagsårsak));
        }
        vilkårResultatRepository.lagre(behandling.getId(), new VilkårResultatBuilder().leggTil(builder).build());
    }

    private static class UtvidetRettTestKlient implements UtvidetRettKlient {

        private List<IverksettelseHendelse> hendelser = new ArrayList<>();

        @Override
        public void innvilget(UtvidetRett innvilget) {
            hendelser.add(new IverksettelseHendelse(true, innvilget));
        }

        @Override
        public void avslått(UtvidetRett avslått) {
            hendelser.add(new IverksettelseHendelse(false, avslått));
        }

        public List<IverksettelseHendelse> getHendelser() {
            return hendelser;
        }
    }

    record IverksettelseHendelse(boolean innvilgelse, UtvidetRett hendelse) {
    }
}
