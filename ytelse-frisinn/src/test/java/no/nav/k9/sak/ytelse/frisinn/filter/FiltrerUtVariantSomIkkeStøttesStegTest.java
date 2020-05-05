package no.nav.k9.sak.ytelse.frisinn.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;

public class FiltrerUtVariantSomIkkeStøttesStegTest {

    private final FiltrerUtVariantSomIkkeStøttesSteg steg = new FiltrerUtVariantSomIkkeStøttesSteg();

    @Test
    public void skal_legge_på_vent_hvis_SN_og_FL() {
        var opptjeningBuilder = OppgittOpptjeningBuilder.ny();
        var perioden = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(2), LocalDate.now());
        var oppgittOpptjening = opptjeningBuilder
            .leggTilFrilansOpplysninger(OppgittOpptjeningBuilder.OppgittFrilansBuilder.ny()
                .medErNyoppstartet(false)
                .leggTilOppgittOppdrag(List.of(OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder.ny()
                    .medInntekt(BigDecimal.TEN)
                    .medPeriode(perioden).build()))
                .build())
            .leggTilEgneNæringer(List.of(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
                .medBruttoInntekt(BigDecimal.TEN)))
            .build();

        var oppgittUttak = new UttakAktivitet(new UttakAktivitetPeriode(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, perioden), new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, perioden));
        var uttakGrunnlag = new UttakGrunnlag(1L, oppgittUttak,
            oppgittUttak, new Søknadsperioder(new Søknadsperiode(perioden)));

        var stegResultat = steg.filtrerBehandlinger(Optional.of(uttakGrunnlag), oppgittOpptjening);

        assertThat(stegResultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_FRISINN_MANGLENDE_FUNKSJONALITET);
    }

    @Test
    public void skal_legge_på_vent_hvis_SN_med_FL_inntekter() {
        var opptjeningBuilder = OppgittOpptjeningBuilder.ny();
        var perioden = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(2), LocalDate.now());
        var oppgittOpptjening = opptjeningBuilder
            .leggTilFrilansOpplysninger(OppgittOpptjeningBuilder.OppgittFrilansBuilder.ny()
                .medErNyoppstartet(false)
                .leggTilOppgittOppdrag(List.of(OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder.ny()
                    .medInntekt(BigDecimal.TEN)
                    .medPeriode(perioden).build()))
                .build())
            .leggTilEgneNæringer(List.of(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
                .medBruttoInntekt(BigDecimal.TEN)))
            .build();

        var oppgittUttak = new UttakAktivitet(new UttakAktivitetPeriode(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, perioden));
        var uttakGrunnlag = new UttakGrunnlag(1L, oppgittUttak,
            oppgittUttak, new Søknadsperioder(new Søknadsperiode(perioden)));

        var stegResultat = steg.filtrerBehandlinger(Optional.of(uttakGrunnlag), oppgittOpptjening);

        assertThat(stegResultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_FRISINN_MANGLENDE_FUNKSJONALITET);
    }

    @Test
    public void skal_legge_på_vent_hvis_FL_med_SN_inntekter() {
        var opptjeningBuilder = OppgittOpptjeningBuilder.ny();
        var perioden = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(2), LocalDate.now());
        var oppgittOpptjening = opptjeningBuilder
            .leggTilFrilansOpplysninger(OppgittOpptjeningBuilder.OppgittFrilansBuilder.ny()
                .medErNyoppstartet(false)
                .leggTilOppgittOppdrag(List.of(OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder.ny()
                    .medInntekt(BigDecimal.TEN)
                    .medPeriode(perioden).build()))
                .build())
            .leggTilEgneNæringer(List.of(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
                .medBruttoInntekt(BigDecimal.TEN)))
            .build();

        var oppgittUttak = new UttakAktivitet(new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, perioden));
        var uttakGrunnlag = new UttakGrunnlag(1L, oppgittUttak,
            oppgittUttak, new Søknadsperioder(new Søknadsperiode(perioden)));

        var stegResultat = steg.filtrerBehandlinger(Optional.of(uttakGrunnlag), oppgittOpptjening);

        assertThat(stegResultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_FRISINN_MANGLENDE_FUNKSJONALITET);
    }

    @Test
    public void skal_legge_på_vent_hvis_nyoppstartet_FL() {
        var opptjeningBuilder = OppgittOpptjeningBuilder.ny();
        var perioden = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(2), LocalDate.now());
        var oppgittOpptjening = opptjeningBuilder
            .leggTilFrilansOpplysninger(OppgittOpptjeningBuilder.OppgittFrilansBuilder.ny()
                .medErNyoppstartet(true)
                .leggTilOppgittOppdrag(List.of(OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder.ny()
                    .medInntekt(BigDecimal.TEN)
                    .medPeriode(perioden).build()))
                .build())
            .build();

        var oppgittUttak = new UttakAktivitet(new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, perioden));
        var uttakGrunnlag = new UttakGrunnlag(1L, oppgittUttak,
            oppgittUttak, new Søknadsperioder(new Søknadsperiode(perioden)));

        var stegResultat = steg.filtrerBehandlinger(Optional.of(uttakGrunnlag), oppgittOpptjening);

        assertThat(stegResultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_FRISINN_MANGLENDE_FUNKSJONALITET);
    }

    @Test
    public void skal_ikke_legges_på_vent_hvis_kun_FL() {
        var opptjeningBuilder = OppgittOpptjeningBuilder.ny();
        var perioden = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(2), LocalDate.now());
        var oppgittOpptjening = opptjeningBuilder
            .leggTilFrilansOpplysninger(OppgittOpptjeningBuilder.OppgittFrilansBuilder.ny()
                .medErNyoppstartet(false)
                .leggTilOppgittOppdrag(List.of(OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder.ny()
                    .medInntekt(BigDecimal.TEN)
                    .medPeriode(perioden).build()))
                .build())
            .build();

        var oppgittUttak = new UttakAktivitet(new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, perioden));
        var uttakGrunnlag = new UttakGrunnlag(1L, oppgittUttak,
            oppgittUttak, new Søknadsperioder(new Søknadsperiode(perioden)));

        var stegResultat = steg.filtrerBehandlinger(Optional.of(uttakGrunnlag), oppgittOpptjening);

        assertThat(stegResultat.getAksjonspunktResultater()).isEmpty();
    }


    @Test
    public void skal_ikke_legges_på_vent_hvis_kun_SN_med_inntenkt_hele_2019() {
        var opptjeningBuilder = OppgittOpptjeningBuilder.ny();
        var perioden = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(2), LocalDate.now());
        var oppgittOpptjening = opptjeningBuilder
            .leggTilEgneNæringer(List.of(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019,1,1), LocalDate.of(2019,12,31)))
                .medBruttoInntekt(BigDecimal.TEN)))
            .build();

        var oppgittUttak = new UttakAktivitet(new UttakAktivitetPeriode(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, perioden));
        var uttakGrunnlag = new UttakGrunnlag(1L, oppgittUttak,
            oppgittUttak, new Søknadsperioder(new Søknadsperiode(perioden)));

        var stegResultat = steg.filtrerBehandlinger(Optional.of(uttakGrunnlag), oppgittOpptjening);

        assertThat(stegResultat.getAksjonspunktResultater()).isEmpty();
    }

    @Test
    public void skal_legges_på_vent_hvis_kun_SN_uten_inntenkt_hele_2019() {
        var opptjeningBuilder = OppgittOpptjeningBuilder.ny();
        var perioden = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(2), LocalDate.now());
        var oppgittOpptjening = opptjeningBuilder
            .leggTilEgneNæringer(List.of(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019,1,2), LocalDate.of(2019,12,31)))
                .medBruttoInntekt(BigDecimal.TEN)))
            .build();

        var oppgittUttak = new UttakAktivitet(new UttakAktivitetPeriode(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, perioden));
        var uttakGrunnlag = new UttakGrunnlag(1L, oppgittUttak,
            oppgittUttak, new Søknadsperioder(new Søknadsperiode(perioden)));

        var stegResultat = steg.filtrerBehandlinger(Optional.of(uttakGrunnlag), oppgittOpptjening);

        assertThat(stegResultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktResultater().get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_FRISINN_MANGLENDE_FUNKSJONALITET);
    }
}
