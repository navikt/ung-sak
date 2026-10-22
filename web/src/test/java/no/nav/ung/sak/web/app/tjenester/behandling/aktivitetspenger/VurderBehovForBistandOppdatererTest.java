package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.VilkårBistandPeriodeVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.VurderBehovForBistandDto;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.AktivitetspengerVilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.avkort.AvkortTjeneste;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderBehovForBistandOppdatererTest {

    private static final Periode PERIODE_1 = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final Periode PERIODE_2 = new Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

    @Inject
    private EntityManager entityManager;

    @Inject
    private AvkortTjeneste avkortTjeneste;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
    private AktivitetspengerVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;

    @Inject
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;

    private VurderBehovForBistandOppdaterer oppdaterer;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    @BeforeAll
    static void beforeAll() {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker("A111111");
    }

    @AfterAll
    static void afterAll() {
        SubjectHandlerUtils.reset();
    }

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        var inngangsvilkårVurderingTjeneste = new InngangsvilkårVurderingTjeneste(inngangsvilkårVurderingRepository, behandlingRepository, vilkårResultatRepository);

        oppdaterer = new VurderBehovForBistandOppdaterer(
            vilkårResultatRepository,
            inngangsvilkårVurderingRepository,
            inngangsvilkårVurderingTjeneste,
            avkortTjeneste,
            vilkårsvurderingHistorikkinnslagTjeneste,
            vilkårsPerioderTilVurderingTjeneste);
    }

    @Test
    void skal_lagre_vurdering_av_alle_nye_søknadsperioder() {
        var behandling = opprettBehandling(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, tilDatoIntervallEntitet(PERIODE_1)),
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, tilDatoIntervallEntitet(PERIODE_2)));

        oppdater(behandling, dto(oppfylt(PERIODE_1), oppfylt(PERIODE_2)));

        assertThat(inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandling.getId(), VilkårType.BISTANDSVILKÅR).getLocalDateIntervals())
            .hasSize(2);
    }

    @Test
    void skal_kreve_vurdering_av_alle_nye_søknadsperioder() {
        var behandling = opprettBehandling(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, tilDatoIntervallEntitet(PERIODE_1)),
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, tilDatoIntervallEntitet(PERIODE_2)));

        assertThatThrownBy(() -> oppdater(behandling, dto(oppfylt(PERIODE_1))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Forventer at alle perioder til vurdering vurderes");
    }

    @Test
    void skal_avvise_vurdering_av_avklart_periode() {
        var behandling = opprettBehandling(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, tilDatoIntervallEntitet(PERIODE_1)),
            new Trigger(BehandlingÅrsakType.ENDRET_BISTANDSBEHOV, tilDatoIntervallEntitet(PERIODE_2)));

        assertThatThrownBy(() -> oppdater(behandling, dto(oppfylt(PERIODE_1), oppfylt(PERIODE_2))))
            .as("avklarte perioder vurderes i aksjonspunktet for vurdering etter avklaring")
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Forsøker å vurdere perioder som ikke er til vurdering");
    }

    @Test
    void skal_avvise_vurdering_av_avkortet_periode() {
        var behandling = opprettBehandling(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, tilDatoIntervallEntitet(PERIODE_1)),
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, tilDatoIntervallEntitet(PERIODE_2)));
        settAvkortet(behandling, PERIODE_2);

        assertThatThrownBy(() -> oppdater(behandling, dto(oppfylt(PERIODE_1), oppfylt(PERIODE_2))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Forsøker å vurdere perioder som ikke er til vurdering");

        oppdater(behandling, dto(oppfylt(PERIODE_1)));
        assertThat(inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandling.getId(), VilkårType.BISTANDSVILKÅR).getLocalDateIntervals())
            .as("avkortet periode skal ikke kreves vurdert")
            .hasSize(1);
    }

    private void settAvkortet(Behandling behandling, Periode periode) {
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandling.getId()));
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.BISTANDSVILKÅR);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFom(), periode.getTom())
            .medUtfall(Utfall.IKKE_OPPFYLT)
            .medAvslagsårsak(Avslagsårsak.AVKORTET));
        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());
    }

    private Behandling opprettBehandling(Trigger... triggere) {
        var behandling = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.AKTIVITETSPENGER)
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.IKKE_VURDERT, PERIODE_1)
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.IKKE_VURDERT, PERIODE_2)
            .lagre(entityManager);
        new ProsessTriggereRepository(entityManager).leggTil(behandling.getId(), Set.of(triggere));
        return behandling;
    }

    private void oppdater(Behandling behandling, VurderBehovForBistandDto dto) {
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
    }

    private static VurderBehovForBistandDto dto(VilkårBistandPeriodeVurderingDto... vurderinger) {
        return new VurderBehovForBistandDto(List.of(vurderinger), "begrunnelse");
    }

    private static VilkårBistandPeriodeVurderingDto oppfylt(Periode periode) {
        return new VilkårBistandPeriodeVurderingDto(periode, true, null, "har behov for bistand", null);
    }

    private static DatoIntervallEntitet tilDatoIntervallEntitet(Periode periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }
}
