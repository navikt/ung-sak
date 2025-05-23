package no.nav.ung.sak.domene.behandling.steg.uttak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VurderUttakStegTest {
    private VilkårTjeneste vilkårTjeneste = mock(VilkårTjeneste.class);
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository = mock(UngdomsytelseGrunnlagRepository.class);
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste = mock(UngdomsprogramPeriodeTjeneste.class);
    private PersonopplysningRepository personopplysningRepository = mock(PersonopplysningRepository.class);
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);

    @Test
    void Forventer_ingen_timeline_empty_ved_ingen_godkjente_perioder() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagMocked();
        var behandlingRepository = scenario.mockBehandlingRepository();

        Fagsak fagsak = behandling.getFagsak();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);

        VurderUttakSteg steg = new VurderUttakSteg(vilkårTjeneste, ungdomsytelseGrunnlagRepository,
            ungdomsprogramPeriodeTjeneste, personopplysningRepository
        );
        LocalDate fom = LocalDate.parse("2024-01-01");
        LocalDate tom = LocalDate.parse("2024-01-31");

        LocalDateTimeline<VilkårUtfallSamlet> samletVilkårResultatTidslinje = new LocalDateTimeline<>(
            fom,
            tom,
            new VilkårUtfallSamlet(
                Utfall.IKKE_OPPFYLT,
                List.of(new VilkårUtfallSamlet.VilkårUtfall(VilkårType.ALDERSVILKÅR, null, Utfall.IKKE_OPPFYLT))));

        when(vilkårTjeneste.samletVilkårsresultat(behandling.getId())).thenReturn(samletVilkårResultatTidslinje);

        when(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId())).thenReturn(new LocalDateTimeline<>(fom, null, true));
        var personInformasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        personInformasjonBuilder.leggTil(personInformasjonBuilder.getPersonopplysningBuilder(fagsak.getAktørId()));
        when(personopplysningRepository.hentPersonopplysninger(behandling.getId())).thenReturn(PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegistrertVersjon(personInformasjonBuilder).build());


        Assertions.assertDoesNotThrow(() -> {
            steg.utførSteg(kontekst);
        });
    }
}
