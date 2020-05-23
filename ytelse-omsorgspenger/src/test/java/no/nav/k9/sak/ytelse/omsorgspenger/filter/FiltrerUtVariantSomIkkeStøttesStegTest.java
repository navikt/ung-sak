package no.nav.k9.sak.ytelse.omsorgspenger.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.domene.iay.modell.YtelseBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class FiltrerUtVariantSomIkkeStøttesStegTest {

    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.SYKEPENGER;
    private FiltrerUtVariantSomIkkeStøttesSteg steg = new FiltrerUtVariantSomIkkeStøttesSteg();

    @Test
    public void skal_returnere_aksjonspunkt_hvis_eq_ytelse_periode() throws Exception {
        var fom = LocalDate.now();
        var tom = fom.plusDays(10);

        var vilkårTimeline = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);

        var builder = byggYtelse(fom, tom);

        var resultat = steg.filtrerBehandlinger(vilkårTimeline, builder.build());

        assertThat(resultat.getAksjonspunktResultater()).isNotNull()
            .allMatch(a -> a.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.AUTO_VENT_FILTER_MANGLENDE_FUNKSJONALITET));
    }

    @Test
    public void skal_returnere_aksjonspunkt_hvis_overlapp_ytelse_periode() throws Exception {
        var fom = LocalDate.now();
        var tom = fom.plusDays(10);

        var vilkårTimeline = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);

        var builder = byggYtelse(fom.minusDays(5), tom.minusDays(5));

        var resultat = steg.filtrerBehandlinger(vilkårTimeline, builder.build());

        assertThat(resultat.getAksjonspunktResultater()).isNotNull()
            .allMatch(a -> a.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.AUTO_VENT_FILTER_MANGLENDE_FUNKSJONALITET));
    }

    @Test
    public void skal_ikke_returnere_aksjonspunkt_hvis_ikke_overlapp_ytelse_periode() throws Exception {
        var fom = LocalDate.now();
        var tom = fom.plusDays(10);

        var vilkårTimeline = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);

        var builder = byggYtelse(fom.minusDays(10), fom.minusDays(1));

        var resultat = steg.filtrerBehandlinger(vilkårTimeline, builder.build());

        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
    }

    @Test
    public void skal_returnere_aksjonspunkt_hvis_eq_ytelse_anvist_periode() throws Exception {
        var fom = LocalDate.now();
        var tom = fom.plusDays(10);

        var vilkårTimeline = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var builder = AktørYtelseBuilder.oppdatere(Optional.empty());

        var yt = YtelseBuilder.oppdatere(Optional.empty())
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)).medYtelseType(YTELSE_TYPE);
        builder.leggTilYtelse(yt);

        var ytelseAnvist = yt.getAnvistBuilder().medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusDays(5), tom.minusDays(5)));
        yt.medYtelseAnvist(ytelseAnvist.build());

        var resultat = steg.filtrerBehandlinger(vilkårTimeline, builder.build());

        assertThat(resultat.getAksjonspunktResultater()).isNotNull()
            .allMatch(a -> a.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.AUTO_VENT_FILTER_MANGLENDE_FUNKSJONALITET));
    }

    private AktørYtelseBuilder byggYtelse(LocalDate fom, LocalDate tom) {
        var builder = AktørYtelseBuilder.oppdatere(Optional.empty());
        var yt = YtelseBuilder.oppdatere(Optional.empty()).medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medYtelseType(YTELSE_TYPE);

        builder.leggTilYtelse(yt);
        return builder;
    }
}
