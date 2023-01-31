package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultatBuilder;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.VurderStatusInput;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class OpptjeningAktivitetResultatVurderingTest {

    @Test
    void vurderStatus() {


        var input = new VurderStatusInput(OpptjeningAktivitetType.SYKEPENGER_AV_DAGPENGER, null);
        var opptjeningPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 5, 4), LocalDate.of(2022, 5, 31));

        var aktivitetPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 2, 17), LocalDate.of(2022, 5, 31));
        input.setAktivitetPeriode(aktivitetPeriode);
        input.setRegisterAktivitet(null);
        var vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 6, 1), LocalDate.of(2022, 6, 1));
        input.setVilkårsperiode(vilkårsperiode);


        var opptjeningResultatBuilder = new OpptjeningResultatBuilder(null);
        var opptjening = new Opptjening(opptjeningPeriode.getFomDato(), opptjeningPeriode.getTomDato());
        opptjening.setOpptjeningAktivitet(List.of(new OpptjeningAktivitet(opptjeningPeriode.getFomDato(), opptjeningPeriode.getTomDato(), OpptjeningAktivitetType.SYKEPENGER_AV_DAGPENGER, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT)));
        opptjeningResultatBuilder.leggTil(opptjening);
        var vurderingsStatus = new OpptjeningAktivitetResultatVurdering(opptjeningResultatBuilder.build()).vurderStatus(input);

        assertThat(vurderingsStatus).isEqualTo(VurderingsStatus.GODKJENT);

    }
}
