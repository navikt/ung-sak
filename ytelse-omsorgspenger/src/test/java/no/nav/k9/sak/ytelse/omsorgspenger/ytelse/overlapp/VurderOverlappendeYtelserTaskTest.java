package no.nav.k9.sak.ytelse.omsorgspenger.ytelse.overlapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.YtelseBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class VurderOverlappendeYtelserTaskTest {

    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.SYKEPENGER;
    private VurderOverlappendeYtelserTask task = new VurderOverlappendeYtelserTask();

    @Test
    public void skal_returnere_aksjonspunkt_hvis_eq_ytelse_periode() throws Exception {
        var fom = LocalDate.now();
        var tom = fom.plusDays(10);

        var vilkårTimeline = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);

        var builder = byggYtelse(fom, tom);

        var resultat = task.harOverlappendeYtelse(vilkårTimeline, builder.build(), UUID.randomUUID());
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_returnere_aksjonspunkt_hvis_overlapp_ytelse_periode() throws Exception {
        var fom = LocalDate.now();
        var tom = fom.plusDays(10);

        var vilkårTimeline = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);

        var builder = byggYtelse(fom.minusDays(5), tom.minusDays(5));

        var resultat = task.harOverlappendeYtelse(vilkårTimeline, builder.build(), UUID.randomUUID());

        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_ikke_returnere_aksjonspunkt_hvis_ikke_overlapp_ytelse_periode() throws Exception {
        var fom = LocalDate.now();
        var tom = fom.plusDays(10);

        var vilkårTimeline = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);

        var builder = byggYtelse(fom.minusDays(10), fom.minusDays(1));

        var resultat = task.harOverlappendeYtelse(vilkårTimeline, builder.build(), UUID.randomUUID());

        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_returnere_aksjonspunkt_hvis_eq_ytelse_anvist_periode() throws Exception {
        var fom = LocalDate.now();
        var tom = fom.plusDays(10);

        var vilkårTimeline = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var builder = InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty());

        var yt = YtelseBuilder.oppdatere(Optional.empty())
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)).medYtelseType(YTELSE_TYPE);
        builder.leggTilYtelse(yt);

        var ytelseAnvist = yt.getAnvistBuilder().medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusDays(5), tom.minusDays(5)));
        yt.medYtelseAnvist(ytelseAnvist.build());

        var resultat = task.harOverlappendeYtelse(vilkårTimeline, builder.build(), UUID.randomUUID());

        assertThat(resultat).isTrue();
    }

    private InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder byggYtelse(LocalDate fom, LocalDate tom) {
        var builder = InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty());
        var yt = YtelseBuilder.oppdatere(Optional.empty()).medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medYtelseType(YTELSE_TYPE);

        builder.leggTilYtelse(yt);
        return builder;
    }
}
