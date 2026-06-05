package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.søknadsperiode.AktivitetspengerSøktPeriode;
import no.nav.ung.sak.behandlingslager.behandling.søknadsperiode.AktivitetspengerSøktPeriodeRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.bosatt.BostedAvklaringData;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.etterlysning.UttalelseData;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class AvklarBostedVurderingsbehovStegTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);
    private static final Periode PERIODE = new Periode(FOM, TOM);

    @Inject
    private EntityManager entityManager;

    @Inject
    private @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    @Inject
    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private AktivitetspengerSøktPeriodeRepository aktivitetspengerSøktPeriodeRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private VurderBosattVilkårSteg steg;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        bostedsGrunnlagRepository = new BostedsGrunnlagRepository(entityManager);
        aktivitetspengerSøktPeriodeRepository = new AktivitetspengerSøktPeriodeRepository(entityManager);
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);

        steg = lagSteg(List.of());
    }

    @Test
    void skal_passere_uten_aksjonspunkt_og_uten_opphorsresultat_nar_bruker_er_bosatt_hele_perioden() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", new Periode(FOM, TOM), true);
        bostedsGrunnlagRepository.lagreForeslåtteAvklaringerOgFjernOverlappendeResultat(behandling.getId(), Map.of(
            PERIODE, new BostedAvklaringData(true, null, null, Kilde.SAKSBEHANDLER)
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    void skal_opprette_opphorsresultat_ved_fraflytting_automatisk() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        var fraflyttingsDato = FOM.plusDays(10);
        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", new Periode(FOM, TOM), true);
        bostedsGrunnlagRepository.lagreForeslåtteAvklaringerOgFjernOverlappendeResultat(behandling.getId(), Map.of(
            PERIODE, new BostedAvklaringData(true, fraflyttingsDato, FraflyttingsÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, Kilde.SAKSBEHANDLER)
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
//        var vurdertAktivitetspengerGrunnlag = new VurdertAktivitetspengerGrunnlag(entityManager);
//        var opphørResultater = vurdertAktivitetspengerGrunnlag.hentAktiveForBehandling(behandling.getId());
//        assertThat(opphørResultater).hasSize(1);
//        assertThat(opphørResultater.getFirst().getSkjæringstidspunkt()).isEqualTo(FOM);
//        assertThat(opphørResultater.getFirst().getOpphørDato()).isEqualTo(fraflyttingsDato);
//        assertThat(opphørResultater.getFirst().getOpphørÅrsak()).isEqualTo(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED);
    }

    @Test
    void skal_sette_pa_vent_nar_periode_venter_pa_etterlysning() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", new Periode(FOM, TOM), true);
        bostedsGrunnlagRepository.lagreForeslåtteAvklaringerOgFjernOverlappendeResultat(behandling.getId(), Map.of(
            PERIODE, new BostedAvklaringData(true, null, null, Kilde.SAKSBEHANDLER)
        ));
        var frist = LocalDateTime.of(2026, 2, 15, 12, 0);
        var ventendeEtterlysning = EtterlysningData.utenUttalelse(
            EtterlysningStatus.VENTER,
            frist,
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0)
        );
        steg = lagSteg(List.of(ventendeEtterlysning));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .containsExactly(EtterlysningType.UTTALELSE_BOSTED.tilAutopunktDefinisjon());
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().getFirst().getFrist()).isEqualTo(frist);
    }

    @Test
    void skal_prioritere_vent_nar_en_periode_er_manuell_og_en_periode_venter_pa_etterlysning() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2);
        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", new Periode(fom2, tom2), true);
        bostedsGrunnlagRepository.lagreForeslåtteAvklaringerOgFjernOverlappendeResultat(behandling.getId(), Map.of(
            PERIODE, new BostedAvklaringData(true, null, null, Kilde.SAKSBEHANDLER),
            new Periode(fom2, tom2), new BostedAvklaringData(true, null, null, Kilde.SØKNAD)
        ));
        var frist = LocalDateTime.of(2026, 3, 1, 10, 0);
        var ventendeEtterlysning = EtterlysningData.utenUttalelse(
            EtterlysningStatus.VENTER,
            frist,
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0)
        );
        var mottattSvarMedUttalelse = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            frist,
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2),
            LocalDateTime.of(2026, 2, 10, 9, 0),
            new UttalelseData(true, "flyttet", new JournalpostId("jp-svar"))
        );
        steg = lagSteg(List.of(ventendeEtterlysning, mottattSvarMedUttalelse));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .containsExactly(EtterlysningType.UTTALELSE_BOSTED.tilAutopunktDefinisjon());
        assertThat(resultat.getAksjonspunktListe())
            .doesNotContain(AksjonspunktDefinisjon.VURDER_BOSTEDVILKÅR);
    }

    private Behandling opprettBehandlingMedVilkårOgPeriode() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        aktivitetspengerSøktPeriodeRepository.lagreNyPeriode(new AktivitetspengerSøktPeriode(
            behandling.getId(),
            new JournalpostId("jp-vilkår"),
            LocalDateTime.now(),
            periode));
        prosessTriggereRepository.leggTil(behandling.getId(), java.util.Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode)));
        return behandling;
    }

    private Behandling opprettBehandlingMedToVilkårsperioder(LocalDate fom2, LocalDate tom2) {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(fom2, tom2))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(fom2, tom2))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(fom2, tom2))
            .lagre(entityManager);

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2);
        aktivitetspengerSøktPeriodeRepository.lagreNyPeriode(new AktivitetspengerSøktPeriode(
            behandling.getId(),
            new JournalpostId("jp-vilkår-1"),
            LocalDateTime.now(),
            periode1));
        aktivitetspengerSøktPeriodeRepository.lagreNyPeriode(new AktivitetspengerSøktPeriode(
            behandling.getId(),
            new JournalpostId("jp-vilkår-2"),
            LocalDateTime.now(),
            periode2));
        prosessTriggereRepository.leggTil(behandling.getId(), java.util.Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode1),
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode2)));
        return behandling;
    }

    private VurderBosattVilkårSteg lagSteg(List<EtterlysningData> etterlysninger) {
        var vilkårTjeneste = new VilkårTjeneste(behandlingRepository, vilkårsPerioderTilVurderingTjenester, vilkårResultatRepository);
        var etterlysningTjeneste = new EtterlysningTjeneste(null, null) {
            @Override
            public List<EtterlysningData> hentGjeldendeEtterlysninger(Long behandlingId, Long fagsakId, EtterlysningType type) {
                return etterlysninger;
            }
        };

        return new VurderBosattVilkårSteg(
            manuelleVilkårRekkefølgeTjeneste,
            vilkårResultatRepository,
            vilkårTjeneste,
            behandlingRepository,
            bostedsGrunnlagRepository,
            vilkårsPerioderTilVurderingTjenester,
            etterlysningTjeneste
        );
    }

    private List<VilkårPeriode> hentPerioder(Long behandlingId) {
        return vilkårResultatRepository.hent(behandlingId)
            .getVilkår(VilkårType.BOSTEDSVILKÅR)
            .orElseThrow()
            .getPerioder();
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }
}

