package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;

class SkalForlengeAktivitetstatusTest {

    public static final long BEHANDLING_ID = 1L;
    public static final LocalDate STP = LocalDate.now();
    private SkalForlengeAktivitetstatus skalForlengeAktivitetstatus;
    private final BehandlingReferanse behandlingReferanse = mock(BehandlingReferanse.class);
    private final BehandlingReferanse originalBehandlingreferanse = mock(BehandlingReferanse.class);
    @BeforeEach
    void setUp() {
        skalForlengeAktivitetstatus = new SkalForlengeAktivitetstatus(new UnitTestLookupInstanceImpl<>((referanse, sakInntektsmeldinger, vilkårsPeriode) -> sakInntektsmeldinger) {});
        when(behandlingReferanse.getBehandlingId()).thenReturn(BEHANDLING_ID);
    }

    @Test
    void skal_forlengele_aktivitetstatus_ved_forlengelse_i_opptjening_og_revurdering_i_beregning() {
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10));
        SkalForlengeAktivitetstatus.SkalForlengeStatusInput input = new SkalForlengeAktivitetstatus.SkalForlengeStatusInput(
            behandlingReferanse,
            originalBehandlingreferanse,
            Set.of(),
            List.of(),
            new TreeSet<>(),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(true, vilkårsperiode))),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(false, vilkårsperiode))),
            new TreeSet<>(Set.of(vilkårsperiode)),
            Set.of(),
            Set.of()
        );

        var periodeForForlengelseAvStatus = skalForlengeAktivitetstatus.finnPerioderForForlengelseAvStatus(input);

        assertThat(periodeForForlengelseAvStatus.size()).isEqualTo(1);
        var resultatperiode = periodeForForlengelseAvStatus.iterator().next();
        assertThat(resultatperiode.getPeriode()).isEqualTo(vilkårsperiode);
    }

    @Test
    void skal_ikke_forlenge_aktivitetstatus_ved_revurdering_i_opptjening_og_revurdering_i_beregning() {
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10));
        SkalForlengeAktivitetstatus.SkalForlengeStatusInput input = new SkalForlengeAktivitetstatus.SkalForlengeStatusInput(
            behandlingReferanse,
            originalBehandlingreferanse,
            Set.of(),
            List.of(),
            new TreeSet<>(),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(false, vilkårsperiode))),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(false, vilkårsperiode))),
            new TreeSet<>(Set.of(vilkårsperiode)),
            Set.of(),
            Set.of()
        );

        var periodeForForlengelseAvStatus = skalForlengeAktivitetstatus.finnPerioderForForlengelseAvStatus(input);

        assertThat(periodeForForlengelseAvStatus.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_forlenge_aktivitetstatus_ved_avslått_i_forrige_behandling() {
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10));
        SkalForlengeAktivitetstatus.SkalForlengeStatusInput input = new SkalForlengeAktivitetstatus.SkalForlengeStatusInput(
            behandlingReferanse,
            originalBehandlingreferanse,
            Set.of(),
            List.of(),
            new TreeSet<>(),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(true, vilkårsperiode))),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(false, vilkårsperiode))),
            new TreeSet<>(),
            Set.of(),
            Set.of()
        );

        var periodeForForlengelseAvStatus = skalForlengeAktivitetstatus.finnPerioderForForlengelseAvStatus(input);

        assertThat(periodeForForlengelseAvStatus.isEmpty()).isTrue();
    }

    @Test
    void skal_forlenge_aktivitetstatus_ved_innvilget_i_forrige_behandling_med_ulik_periode_men_likt_stp() {
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10));
        var vilkårsperiodeForrigeBehandling = DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(5));

        SkalForlengeAktivitetstatus.SkalForlengeStatusInput input = new SkalForlengeAktivitetstatus.SkalForlengeStatusInput(
            behandlingReferanse,
            originalBehandlingreferanse,
            Set.of(),
            List.of(),
            new TreeSet<>(),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(true, vilkårsperiode))),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(false, vilkårsperiode))),
            new TreeSet<>(Set.of(vilkårsperiodeForrigeBehandling)),
            Set.of(),
            Set.of()
        );

        var periodeForForlengelseAvStatus = skalForlengeAktivitetstatus.finnPerioderForForlengelseAvStatus(input);

        assertThat(periodeForForlengelseAvStatus.size()).isEqualTo(1);
        var resultatperiode = periodeForForlengelseAvStatus.iterator().next();
        assertThat(resultatperiode.getPeriode()).isEqualTo(vilkårsperiode);
    }

    @Test
    void skal_ikke_forlenge_aktivitetstatus_ved_innvilget_i_forrige_behandling_med_ulik_periode_og_ulikt_stp() {
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10));
        var vilkårsperiodeForrigeBehandling = DatoIntervallEntitet.fraOgMedTilOgMed(STP.plusDays(1), STP.plusDays(5));

        SkalForlengeAktivitetstatus.SkalForlengeStatusInput input = new SkalForlengeAktivitetstatus.SkalForlengeStatusInput(
            behandlingReferanse,
            originalBehandlingreferanse,
            Set.of(),
            List.of(),
            new TreeSet<>(),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(true, vilkårsperiode))),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(false, vilkårsperiode))),
            new TreeSet<>(Set.of(vilkårsperiodeForrigeBehandling)),
            Set.of(),
            Set.of()
        );

        var periodeForForlengelseAvStatus = skalForlengeAktivitetstatus.finnPerioderForForlengelseAvStatus(input);

        assertThat(periodeForForlengelseAvStatus.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_forlenge_aktivitetstatus_ved_mottatt_inntektsmelding_for_nytt_arbeidsforhold() {
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10));

        var im = lagInntektsmelding("12346778", InternArbeidsforholdRef.nullRef());
        var mottattInntektsmelding = lagMottattInntektsmelding();

        SkalForlengeAktivitetstatus.SkalForlengeStatusInput input = new SkalForlengeAktivitetstatus.SkalForlengeStatusInput(
            behandlingReferanse,
            originalBehandlingreferanse,
            Set.of(im),
            List.of(mottattInntektsmelding),
            new TreeSet<>(),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(true, vilkårsperiode))),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(false, vilkårsperiode))),
            new TreeSet<>(Set.of(vilkårsperiode)),
            Set.of(),
            Set.of()
        );

        var periodeForForlengelseAvStatus = skalForlengeAktivitetstatus.finnPerioderForForlengelseAvStatus(input);

        assertThat(periodeForForlengelseAvStatus.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_forlenge_aktivitetstatus_ved_prosesstrigger_for_reberegning() {
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10));

        SkalForlengeAktivitetstatus.SkalForlengeStatusInput input = new SkalForlengeAktivitetstatus.SkalForlengeStatusInput(
            behandlingReferanse,
            originalBehandlingreferanse,
            Set.of(),
            List.of(),
            new TreeSet<>(Set.of(vilkårsperiode)),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(true, vilkårsperiode))),
            new TreeSet<>(Set.of(lagPeriodeTilVurdering(false, vilkårsperiode))),
            new TreeSet<>(Set.of(vilkårsperiode)),
            Set.of(),
            Set.of()
        );

        var periodeForForlengelseAvStatus = skalForlengeAktivitetstatus.finnPerioderForForlengelseAvStatus(input);

        assertThat(periodeForForlengelseAvStatus.isEmpty()).isTrue();
    }

    private static Inntektsmelding lagInntektsmelding(String orgnr, InternArbeidsforholdRef arbeidsforholdId) {
        return InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medArbeidsforholdId(arbeidsforholdId)
            .medBeløp(BigDecimal.TEN)
            .medKanalreferanse("KANALREFERANSE")
            .build();
    }

    private static MottattDokument lagMottattInntektsmelding() {
        var builder = new MottattDokument.Builder();
        builder.medBehandlingId(BEHANDLING_ID);
        builder.medFagsakId(2L);
        var mottattInntektsmelding = builder.build();
        return mottattInntektsmelding;
    }


    private static PeriodeTilVurdering lagPeriodeTilVurdering(boolean erForlengelse, DatoIntervallEntitet periode) {
        var periodeTilVurdering = new PeriodeTilVurdering(periode);
        periodeTilVurdering.setErForlengelse(erForlengelse);
        return periodeTilVurdering;
    }

    @Test
    void skal_gi_ingen_endring_ved_en_IM_uten_referanse() {

        var gjeldendeIM = lagInntektsmelding("12346778", InternArbeidsforholdRef.nullRef());

        var erEndret = SkalForlengeAktivitetstatus.erEndret(List.of(gjeldendeIM), List.of(gjeldendeIM));

        assertThat(erEndret).isFalse();

    }

    @Test
    void skal_gi_ingen_endring_ved_en_IM_med_referanse() {

        var gjeldendeIM = lagInntektsmelding("12346778", InternArbeidsforholdRef.nyRef());

        var erEndret = SkalForlengeAktivitetstatus.erEndret(List.of(gjeldendeIM), List.of(gjeldendeIM));

        assertThat(erEndret).isFalse();

    }

    @Test
    void skal_gi_endring_når_ulike_arbeidsgivere_likt_antall_IM() {

        var gjeldendeIM = lagInntektsmelding("12346778", InternArbeidsforholdRef.nullRef());

        var forrigeIM = lagInntektsmelding("2442323", InternArbeidsforholdRef.nullRef());

        var erEndret = SkalForlengeAktivitetstatus.erEndret(List.of(gjeldendeIM), List.of(forrigeIM));

        assertThat(erEndret).isTrue();

    }

    @Test
    void skal_gi_endring_når_tilkommet_inntektsmelding_for_samme_AG() {

        var gjeldendeIM = lagInntektsmelding("12346778", InternArbeidsforholdRef.nyRef());

        var forrigeIM = lagInntektsmelding("12346778", InternArbeidsforholdRef.nyRef());

        var erEndret = SkalForlengeAktivitetstatus.erEndret(List.of(gjeldendeIM, forrigeIM), List.of(forrigeIM));

        assertThat(erEndret).isTrue();

    }

    @Test
    void skal_gi_endring_når_tilkommet_inntektsmelding_for_ny_AG() {

        var gjeldendeIM = lagInntektsmelding("12346778", InternArbeidsforholdRef.nyRef());

        var forrigeIM = lagInntektsmelding("32423423", InternArbeidsforholdRef.nyRef());

        var erEndret = SkalForlengeAktivitetstatus.erEndret(List.of(gjeldendeIM, forrigeIM), List.of(forrigeIM));

        assertThat(erEndret).isTrue();
    }

    @Test
    void skal_gi_endring_når_forrige_IM_var_tom() {

        var gjeldendeIM = lagInntektsmelding("12346778", InternArbeidsforholdRef.nyRef());


        var erEndret = SkalForlengeAktivitetstatus.erEndret(List.of(gjeldendeIM), List.of());

        assertThat(erEndret).isTrue();
    }


    @Test
    void skal_gi_endring_når_gjeldende_IM_er_tom() {

        var forrigeIM = lagInntektsmelding("32423423", InternArbeidsforholdRef.nyRef());


        var erEndret = SkalForlengeAktivitetstatus.erEndret(List.of(), List.of(forrigeIM));

        assertThat(erEndret).isTrue();
    }

    @Test
    void skal_gi_ingen_endring_når_begge_listene_er_tomme() {

        var erEndret = SkalForlengeAktivitetstatus.erEndret(List.of(), List.of());

        assertThat(erEndret).isFalse();
    }


}
