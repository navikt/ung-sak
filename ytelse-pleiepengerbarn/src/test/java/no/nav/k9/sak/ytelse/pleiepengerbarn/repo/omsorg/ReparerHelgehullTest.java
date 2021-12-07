package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.omsorg.BarnRelasjon;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class ReparerHelgehullTest {
    @Inject
    private OmsorgenForGrunnlagRepository repo;

    @Test
    void BaseCase() {
        List<OmsorgenForPeriode> perioder = new ArrayList<>();
        var stp = mandagenFør(LocalDate.now().minusWeeks(8));
        var søknadsperiode1 = DatoIntervallEntitet.fra(stp, stp.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode1, BarnRelasjon.MOR, "", "", Resultat.OPPFYLT));

        var stp2 = mandagenFør(søknadsperiode1.getTomDato().plusDays(3));
        var søknadsperiode2 = DatoIntervallEntitet.fra(stp2, stp2.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode2, BarnRelasjon.MOR, "", "", Resultat.IKKE_VURDERT));

        boolean endret = repo.reparerHelgehull(perioder);
        assertThat(endret).isTrue();

        OmsorgenForPeriode omsorgenForPeriode = perioder.get(1);
        assertThat(omsorgenForPeriode.getPeriode().getFomDato()).isEqualTo(søknadsperiode1.getTomDato().plusDays(1));
    }

    @Test
    void hvis_naboperiode_er_avslått_gjør_ingenting() {
        List<OmsorgenForPeriode> perioder = new ArrayList<>();
        var stp = mandagenFør(LocalDate.now().minusWeeks(8));
        var søknadsperiode1 = DatoIntervallEntitet.fra(stp, stp.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode1, BarnRelasjon.MOR, "", "", Resultat.IKKE_OPPFYLT));

        var stp2 = mandagenFør(søknadsperiode1.getTomDato().plusDays(3));
        var søknadsperiode2 = DatoIntervallEntitet.fra(stp2, stp2.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode2, BarnRelasjon.MOR, "", "", Resultat.IKKE_VURDERT));

        boolean endret = repo.reparerHelgehull(perioder);
        assertThat(endret).isFalse();

        OmsorgenForPeriode omsorgenForPeriode = perioder.get(1);
        assertThat(omsorgenForPeriode.getPeriode().getFomDato()).isEqualTo(søknadsperiode2.getFomDato());
    }

    @Test
    void nyPeriodeForanMåVokseFremover() {
        List<OmsorgenForPeriode> perioder = new ArrayList<>();
        var stp = mandagenFør(LocalDate.now().minusWeeks(8));
        var søknadsperiode1 = DatoIntervallEntitet.fra(stp, stp.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode1, BarnRelasjon.MOR, "", "", Resultat.OPPFYLT));

        var stp2 = mandagenFør(søknadsperiode1.getFomDato().minusWeeks(1));
        var søknadsperiode2 = DatoIntervallEntitet.fra(stp2, stp2.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode2, BarnRelasjon.MOR, "", "", Resultat.IKKE_VURDERT));

        boolean endret = repo.reparerHelgehull(perioder);
        assertThat(endret).isTrue();

        OmsorgenForPeriode omsorgenForPeriode = perioder.get(0);
        assertThat(omsorgenForPeriode.getPeriode().getTomDato()).isEqualTo(søknadsperiode1.getFomDato().minusDays(1));
    }

    @Test
    void to_oppfylte_perioder_må_repareres_med_ny_periode() {
        List<OmsorgenForPeriode> perioder = new ArrayList<>();
        var stp = mandagenFør(LocalDate.now().minusWeeks(8));
        var søknadsperiode1 = DatoIntervallEntitet.fra(stp, stp.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode1, BarnRelasjon.MOR, "", "", Resultat.OPPFYLT));

        var stp2 = mandagenFør(søknadsperiode1.getTomDato().plusDays(3));
        var søknadsperiode2 = DatoIntervallEntitet.fra(stp2, stp2.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode2, BarnRelasjon.MOR, "", "", Resultat.OPPFYLT));

        boolean endret = repo.reparerHelgehull(perioder);
        assertThat(endret).isTrue();

        assertThat(perioder.size()).isEqualTo(3);
        OmsorgenForPeriode omsorgenForPeriode = perioder.get(1);
        assertThat(omsorgenForPeriode.getPeriode().getFomDato()).isEqualTo(søknadsperiode1.getTomDato().plusDays(1));
        assertThat(omsorgenForPeriode.getPeriode().getTomDato()).isEqualTo(søknadsperiode2.getFomDato().minusDays(1));
    }

    @Test
    void ikke_reparere_hull_i_hverdager() {
        List<OmsorgenForPeriode> perioder = new ArrayList<>();
        var stp = mandagenFør(LocalDate.now().minusWeeks(8));
        var søknadsperiode1 = DatoIntervallEntitet.fra(stp, stp.plusDays(1));
        perioder.add(new OmsorgenForPeriode(søknadsperiode1, BarnRelasjon.MOR, "", "", Resultat.OPPFYLT));

        var stp2 = stp.plusDays(3);
        var søknadsperiode2 = DatoIntervallEntitet.fra(stp2, stp2.plusDays(1));
        perioder.add(new OmsorgenForPeriode(søknadsperiode2, BarnRelasjon.MOR, "", "", Resultat.IKKE_VURDERT));

        boolean endret = repo.reparerHelgehull(perioder);
        assertThat(endret).isFalse();

        assertThat(perioder.size()).isEqualTo(2);
        OmsorgenForPeriode omsorgenForPeriode = perioder.get(0);
        assertThat(omsorgenForPeriode.getPeriode().getFomDato()).isEqualTo(søknadsperiode1.getFomDato());
        assertThat(omsorgenForPeriode.getPeriode().getTomDato()).isEqualTo(søknadsperiode1.getTomDato());
        omsorgenForPeriode = perioder.get(1);
        assertThat(omsorgenForPeriode.getPeriode().getFomDato()).isEqualTo(søknadsperiode2.getFomDato());
        assertThat(omsorgenForPeriode.getPeriode().getTomDato()).isEqualTo(søknadsperiode2.getTomDato());
    }

    @Test
    void flere_perioder_ikke_vurdert_voks_fremover_uten_overlapp() {
        List<OmsorgenForPeriode> perioder = new ArrayList<>();
        var stp = mandagenFør(LocalDate.now().minusWeeks(8));
        var søknadsperiode1 = DatoIntervallEntitet.fra(stp, stp.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode1, BarnRelasjon.MOR, "", "", Resultat.OPPFYLT));

        var stp2 = mandagenFør(søknadsperiode1.getFomDato().plusWeeks(1));
        var søknadsperiode2 = DatoIntervallEntitet.fra(stp2, stp2.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode2, BarnRelasjon.MOR, "", "", Resultat.IKKE_VURDERT));

        var stp3 = mandagenFør(søknadsperiode2.getFomDato().plusWeeks(1));
        var søknadsperiode3 = DatoIntervallEntitet.fra(stp3, stp3.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode3, BarnRelasjon.MOR, "", "", Resultat.IKKE_VURDERT));

        boolean endret = repo.reparerHelgehull(perioder);
        assertThat(endret).isTrue();

        assertThat(perioder.size()).isEqualTo(3);
        OmsorgenForPeriode omsorgenForPeriode = perioder.get(1);
        assertThat(omsorgenForPeriode.getPeriode().getFomDato()).isEqualTo(søknadsperiode1.getTomDato().plusDays(1));
        assertThat(omsorgenForPeriode.getPeriode().getTomDato()).isEqualTo(søknadsperiode3.getFomDato().minusDays(1));

        omsorgenForPeriode = perioder.get(2);
        assertThat(omsorgenForPeriode.getPeriode().getFomDato()).isEqualTo(søknadsperiode3.getFomDato());
        assertThat(omsorgenForPeriode.getPeriode().getTomDato()).isEqualTo(søknadsperiode3.getTomDato());
    }

    @Test
    void ingen_helgehull_gjør_ingenting() {
        List<OmsorgenForPeriode> perioder = new ArrayList<>();
        var stp = mandagenFør(LocalDate.now().minusWeeks(8));
        var søknadsperiode1 = DatoIntervallEntitet.fra(stp, stp.plusDays(6));
        perioder.add(new OmsorgenForPeriode(søknadsperiode1, BarnRelasjon.MOR, "", "", Resultat.OPPFYLT));

        var stp2 = stp.plusWeeks(1);
        var søknadsperiode2 = DatoIntervallEntitet.fra(stp2, stp2.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode2, BarnRelasjon.MOR, "", "", Resultat.IKKE_VURDERT));

        boolean endret = repo.reparerHelgehull(perioder);
        assertThat(endret).isFalse();

        OmsorgenForPeriode omsorgenForPeriode = perioder.get(0);
        assertThat(omsorgenForPeriode.getPeriode().getFomDato()).isEqualTo(søknadsperiode1.getFomDato());
        assertThat(omsorgenForPeriode.getPeriode().getTomDato()).isEqualTo(søknadsperiode1.getTomDato());
        omsorgenForPeriode = perioder.get(1);
        assertThat(omsorgenForPeriode.getPeriode().getFomDato()).isEqualTo(søknadsperiode2.getFomDato());
        assertThat(omsorgenForPeriode.getPeriode().getTomDato()).isEqualTo(søknadsperiode2.getTomDato());
    }



    private LocalDate mandagenFør(LocalDate d) {
        return d.minusDays(d.getDayOfWeek().getValue() - 1);
    }
}
