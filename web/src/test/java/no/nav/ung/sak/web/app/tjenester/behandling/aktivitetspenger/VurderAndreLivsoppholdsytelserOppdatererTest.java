package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.AktivitetspengerVilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.avkort.AvkortTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold.VilkårLivsoppholdsytelserPeriodeVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold.VurderAndreLivsoppholdsytelserDto;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
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
class VurderAndreLivsoppholdsytelserOppdatererTest {

    private static final Periode PERIODE = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
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

    private VurderAndreLivsoppholdsytelserOppdaterer oppdaterer;
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
        var inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        var inngangsvilkårVurderingTjeneste = new InngangsvilkårVurderingTjeneste(inngangsvilkårVurderingRepository, behandlingRepository, vilkårResultatRepository);

        oppdaterer = new VurderAndreLivsoppholdsytelserOppdaterer(
            vilkårResultatRepository,
            inngangsvilkårVurderingRepository,
            inngangsvilkårVurderingTjeneste,
            avkortTjeneste,
            vilkårsvurderingHistorikkinnslagTjeneste,
            vilkårsPerioderTilVurderingTjeneste);
    }

    @Test
    void skal_bruke_ytelseNavn_når_det_er_satt() {
        var dto = new VilkårLivsoppholdsytelserPeriodeVurderingDto(PERIODE, false, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ANNEN_YTELSE, "begrunnelse", "fritekst", "sykepenger");

        assertThat(VurderAndreLivsoppholdsytelserOppdaterer.utledYtelseNavn(dto)).isEqualTo("sykepenger");
    }

    @Test
    void skal_falle_tilbake_til_fritekstVurderingBrev_for_annen_ytelse_når_ytelseNavn_mangler() {
        var dto = new VilkårLivsoppholdsytelserPeriodeVurderingDto(PERIODE, false, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ANNEN_YTELSE, "begrunnelse", "sykepenger");

        assertThat(VurderAndreLivsoppholdsytelserOppdaterer.utledYtelseNavn(dto)).isEqualTo("sykepenger");
    }

    @Test
    void skal_ikke_falle_tilbake_til_fritekstVurderingBrev_for_andre_årsaker() {
        var dto = new VilkårLivsoppholdsytelserPeriodeVurderingDto(PERIODE, false, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER, "begrunnelse", "fritekst");

        assertThat(VurderAndreLivsoppholdsytelserOppdaterer.utledYtelseNavn(dto)).isNull();
    }

    @Test
    void skal_kreve_vurdering_av_alle_nye_søknadsperioder() {
        var behandling = opprettBehandling(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, tilDatoIntervallEntitet(PERIODE)),
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, tilDatoIntervallEntitet(PERIODE_2)));

        assertThatThrownBy(() -> oppdater(behandling, dto(oppfylt(PERIODE))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Forventer at alle perioder til vurdering vurderes");
    }

    @Test
    void skal_avvise_vurdering_av_avklart_periode() {
        var behandling = opprettBehandling(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, tilDatoIntervallEntitet(PERIODE)),
            new Trigger(BehandlingÅrsakType.ENDRET_LIVSOPPHOLDSYTELSE, tilDatoIntervallEntitet(PERIODE_2)));

        assertThatThrownBy(() -> oppdater(behandling, dto(oppfylt(PERIODE), oppfylt(PERIODE_2))))
            .as("avklarte perioder vurderes i aksjonspunktet for vurdering etter avklaring")
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Forsøker å vurdere perioder som ikke er til vurdering");
    }

    @Test
    void skal_avvise_vurdering_av_avkortet_periode() {
        var behandling = opprettBehandling(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, tilDatoIntervallEntitet(PERIODE)),
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, tilDatoIntervallEntitet(PERIODE_2)));
        settAvkortet(behandling, PERIODE_2);

        assertThatThrownBy(() -> oppdater(behandling, dto(oppfylt(PERIODE), oppfylt(PERIODE_2))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Forsøker å vurdere perioder som ikke er til vurdering");
    }

    private void settAvkortet(Behandling behandling, Periode periode) {
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandling.getId()));
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFom(), periode.getTom())
            .medUtfall(Utfall.IKKE_OPPFYLT)
            .medAvslagsårsak(Avslagsårsak.AVKORTET));
        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());
    }

    private Behandling opprettBehandling(Trigger... triggere) {
        var behandling = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.AKTIVITETSPENGER)
            .leggTilVilkår(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, Utfall.IKKE_VURDERT, PERIODE)
            .leggTilVilkår(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, Utfall.IKKE_VURDERT, PERIODE_2)
            .lagre(entityManager);
        new ProsessTriggereRepository(entityManager).leggTil(behandling.getId(), Set.of(triggere));
        return behandling;
    }

    private void oppdater(Behandling behandling, VurderAndreLivsoppholdsytelserDto dto) {
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
    }

    private static VurderAndreLivsoppholdsytelserDto dto(VilkårLivsoppholdsytelserPeriodeVurderingDto... vurderinger) {
        return new VurderAndreLivsoppholdsytelserDto(List.of(vurderinger), "begrunnelse");
    }

    private static VilkårLivsoppholdsytelserPeriodeVurderingDto oppfylt(Periode periode) {
        return new VilkårLivsoppholdsytelserPeriodeVurderingDto(periode, true, null, "mottar ingen andre ytelser", null);
    }

    private static DatoIntervallEntitet tilDatoIntervallEntitet(Periode periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }
}
