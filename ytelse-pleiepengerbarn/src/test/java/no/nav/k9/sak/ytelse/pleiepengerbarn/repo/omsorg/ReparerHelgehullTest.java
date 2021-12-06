package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.omsorg.BarnRelasjon;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.søknad.felles.type.Periode;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class ReparerHelgehullTest {
    @Inject
    private OmsorgenForGrunnlagRepository repo;

    @Test
    void BaseCase() {
        List<OmsorgenForPeriode> perioder = new ArrayList<>();
        OmsorgenFor omsorg = new OmsorgenFor();
        var stp = mandagenFør(LocalDate.now().minusWeeks(8));
        var søknadsperiode1 = DatoIntervallEntitet.fra(stp, stp.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode1, BarnRelasjon.MOR, "", "", Resultat.OPPFYLT));

        var stp2 = mandagenFør(søknadsperiode1.getTomDato().plusDays(3));
        var søknadsperiode2 = DatoIntervallEntitet.fra(stp2, stp2.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode2, BarnRelasjon.MOR, "", "", Resultat.IKKE_VURDERT));

        List<OmsorgenForPeriode> reparertePerioder = repo.reparerHelgehull(perioder);

        OmsorgenForPeriode omsorgenForPeriode = reparertePerioder.get(1);
        assertThat(omsorgenForPeriode.getPeriode().getFomDato()).isEqualTo(søknadsperiode1.getTomDato().plusDays(1));
    }

    @Test
    void hvis_naboperiode_er_avslått_gjør_ingenting() {
        List<OmsorgenForPeriode> perioder = new ArrayList<>();
        OmsorgenFor omsorg = new OmsorgenFor();
        var stp = mandagenFør(LocalDate.now().minusWeeks(8));
        var søknadsperiode1 = DatoIntervallEntitet.fra(stp, stp.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode1, BarnRelasjon.MOR, "", "", Resultat.IKKE_OPPFYLT));

        var stp2 = mandagenFør(søknadsperiode1.getTomDato().plusDays(3));
        var søknadsperiode2 = DatoIntervallEntitet.fra(stp2, stp2.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode2, BarnRelasjon.MOR, "", "", Resultat.IKKE_VURDERT));

        List<OmsorgenForPeriode> reparertePerioder = repo.reparerHelgehull(perioder);

        OmsorgenForPeriode omsorgenForPeriode = reparertePerioder.get(1);
        assertThat(omsorgenForPeriode.getPeriode().getFomDato()).isEqualTo(søknadsperiode2.getFomDato());
    }

    @Test
    void reparerHelgehullTestBaseCase() {
        List<OmsorgenForPeriode> perioder = new ArrayList<>();
        OmsorgenFor omsorg = new OmsorgenFor();
        var stp = mandagenFør(LocalDate.now().minusWeeks(8));
        var søknadsperiode1 = DatoIntervallEntitet.fra(stp, stp.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode1, BarnRelasjon.MOR, "", "", Resultat.OPPFYLT));

        var stp2 = mandagenFør(søknadsperiode1.getTomDato().plusDays(3));
        var søknadsperiode2 = DatoIntervallEntitet.fra(stp2, stp2.plusDays(4));
        perioder.add(new OmsorgenForPeriode(søknadsperiode2, BarnRelasjon.MOR, "", "", Resultat.IKKE_VURDERT));

        List<OmsorgenForPeriode> reparertePerioder = repo.reparerHelgehull(perioder);

        OmsorgenForPeriode omsorgenForPeriode = reparertePerioder.get(1);
        assertThat(omsorgenForPeriode.getPeriode().getFomDato()).isEqualTo(søknadsperiode1.getTomDato().plusDays(1));
    }





    private LocalDate mandagenFør(LocalDate d) {
        return d.minusDays(d.getDayOfWeek().getValue() - 1);
    }
}
