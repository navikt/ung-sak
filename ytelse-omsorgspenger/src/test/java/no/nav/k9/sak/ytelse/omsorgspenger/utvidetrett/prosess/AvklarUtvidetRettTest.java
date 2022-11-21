package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.omsorgspenger.AvklarUtvidetRettDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.Periode;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class AvklarUtvidetRettTest {

    private static final AksjonspunktDefinisjon APDEF = AksjonspunktDefinisjon.VURDER_OMS_UTVIDET_RETT;

    private static final Avslagsårsak AVSLAG = Avslagsårsak.IKKE_UTVIDETRETT;

    private static final VilkårType VT = VilkårType.UTVIDETRETT;

    @Inject
    @Any
    private AvklarUtvidetRett avklarUtvidetRett;

    @Inject
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    private EntityManager entityManager;

    @Test
    void test_reset_vilkår_perioder() throws Exception {
        LocalDate søknadFom = date(LocalDate.now().getYear() + "-04-09");
        LocalDate søknadTom = date(LocalDate.now().getYear() + "-11-16");
        Periode p0 = new Periode(søknadFom, søknadTom);
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OMSORGSPENGER_AO);
        scenario.medSøknad().medSøknadsperiode(søknadFom, søknadTom);
        scenario.leggTilVilkår(VT, Utfall.IKKE_VURDERT, new Periode(søknadFom, søknadTom));
        scenario.leggTilVilkår(VilkårType.OMSORGEN_FOR, Utfall.OPPFYLT, new Periode(søknadFom, null));
        scenario.leggTilAksjonspunkt(APDEF, null);

        var behandling = scenario.lagre(repositoryProvider);
        var ap = behandling.getAksjonspunktFor(APDEF);

        Periode p1 = periode(LocalDate.now().getYear() + "-11-18",LocalDate.now().getYear() + "-12-31");
        var v1 = simulerAksjonspunktOppdatering(behandling, ap, p1, null);
        assertThat(v1.getVilkårTimeline(VT).toSegments()).allSatisfy(s -> assertThat(s.getValue().getUtfall()).isEqualTo(Utfall.OPPFYLT));
        assertThat(v1.getVilkårTimeline(VT).getLocalDateIntervals()).containsOnly(DatoIntervallEntitet.fra(p1).toLocalDateInterval());

        // <-- vanligvis skjer et tilbakehopp her før overskriving med ny periode

        var v2 = simulerAksjonspunktOppdatering(behandling, ap, null, AVSLAG);
        assertThat(v2.getVilkårTimeline(VT).toSegments()).allSatisfy(s -> assertThat(s.getValue().getUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT));
        assertThat(v2.getVilkårTimeline(VT).getLocalDateIntervals()).containsOnly(DatoIntervallEntitet.fra(p0).toLocalDateInterval());

        // <-- vanligvis skjer et tilbakehopp her før overskriving med ny periode

        Periode p3 = periode(LocalDate.now().getYear() + "-11-19", LocalDate.now().getYear() + "-12-31");
        var v3 = simulerAksjonspunktOppdatering(behandling, ap, p3, null);
        assertThat(v3.getVilkårTimeline(VT).toSegments()).allSatisfy(s -> assertThat(s.getValue().getUtfall()).isEqualTo(Utfall.OPPFYLT));
        assertThat(v3.getVilkårTimeline(VT).getLocalDateIntervals()).containsOnly(DatoIntervallEntitet.fra(p3).toLocalDateInterval());

    }

    private Vilkårene simulerAksjonspunktOppdatering(Behandling behandling,
                                                     Aksjonspunkt ap,
                                                     Periode angittPeriode,
                                                     Avslagsårsak avslag) {
        var vilkårene = vilkårResultatRepository.hent(behandling.getId());

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);

        var dto = new AvklarUtvidetRettDto("en begrunnelse", avslag == null, angittPeriode, avslag);
        var param = new AksjonspunktOppdaterParameter(behandling, Optional.of(ap), null, vilkårResultatBuilder, dto);

        avklarUtvidetRett.oppdater(dto, param);

        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());

        return vilkårResultatRepository.hent(behandling.getId());
    }

    private static LocalDate date(String str) {
        return str == null ? null : LocalDate.parse(str);
    }

    private static Periode periode(String fom, String tom) {
        return new Periode(date(fom), date(tom));
    }

}
