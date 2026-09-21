package no.nav.ung.ytelse.aktivitetspenger.del1.steg.andrelivsoppholdsytelser;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.*;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandingprosessSporingRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.Vilkårsutfallsporing;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AndreLivsoppholdsytelserResultatHolder;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AndreLivsoppholdsytelserResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
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
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfallUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderAndreLivsoppholdsytelserStegTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);
    private static final VilkårType VILKÅR = VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;
    private static final AndreLivsoppholdsytelserIkkeOppfyltÅrsak AUTOMATISERBAR_ÅRSAK = AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER;

    @Inject
    private EntityManager entityManager;

    @Inject
    private @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    @Inject
    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;

    @Inject
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private StartdatoRepository startdatoRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private Vilkårsutfallsporing vilkårsutfallsporing;
    private VurderAndreLivsoppholdsytelserSteg steg;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        vilkårsavklaringGrunnlagRepository = new VilkårsavklaringGrunnlagRepository(entityManager);
        startdatoRepository = new StartdatoRepository(entityManager);
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        inngangsvilkårVurderingTjeneste = new InngangsvilkårVurderingTjeneste(inngangsvilkårVurderingRepository, behandlingRepository, vilkårResultatRepository);
        vilkårsutfallsporing = new Vilkårsutfallsporing(new BehandingprosessSporingRepository(entityManager));

        steg = lagSteg(List.of());
    }

    @Test
    void skal_gi_manuelt_aksjonspunkt_nar_det_ikke_finnes_avklaring() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_ANDRE_LIVSOPPHOLDSYTELSER);
        var vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VILKÅR).orElseThrow().getPerioder();
        assertThat(vilkår).allMatch(it -> it.getGjeldendeUtfall() == Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_avslå_automatisk_nar_ytelsen_er_navngitt_og_bruker_ikke_har_uttalelse() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        var avklaring = lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, AUTOMATISERBAR_ÅRSAK, true);

        var etterlysningUtenUttalelse = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(false, null, new JournalpostId("jp-uttalelse-1"))
        );
        steg = lagSteg(List.of(etterlysningUtenUttalelse));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var vurdering = hentVurderinger(behandling).stream().findFirst().orElseThrow();
        assertThat(vurdering.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        assertThat(vurdering.isGodkjent()).isFalse();
        assertThat(vurdering.getIkkeOppfyltÅrsak()).isEqualTo(AUTOMATISERBAR_ÅRSAK);
        assertThat(vurdering.isManuellVurdering()).isFalse();

        var vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VILKÅR).orElseThrow().getPerioder();
        assertThat(vilkår).allMatch(it -> it.getGjeldendeUtfall() == Utfall.IKKE_OPPFYLT);
        assertThat(vilkår).allMatch(it -> it.getAvslagsårsak() == Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE);
    }

    @Test
    void skal_ikke_avslå_automatisk_nar_arsaken_krever_fritekst() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        var avklaring = lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ANNEN_YTELSE, true);

        var etterlysningUtenUttalelse = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(false, null, new JournalpostId("jp-uttalelse-1"))
        );
        steg = lagSteg(List.of(etterlysningUtenUttalelse));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .as("ytelsen står kun i fritekst, og saksbehandler må derfor vurdere den")
            .containsExactly(AksjonspunktDefinisjon.VURDER_ANDRE_LIVSOPPHOLDSYTELSER_OPPHØR);
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    @Test
    void skal_ikke_avslå_automatisk_nar_bruker_har_uttalelse() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        var avklaring = lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, AUTOMATISERBAR_ÅRSAK, true);

        var etterlysningMedUttalelse = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(true, "Jeg mottar ikke dagpenger", new JournalpostId("jp-uttalelse-1"))
        );
        steg = lagSteg(List.of(etterlysningMedUttalelse));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_ANDRE_LIVSOPPHOLDSYTELSER_OPPHØR);
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    @Test
    void skal_ikke_avslå_automatisk_nar_det_er_valgt_a_ikke_varsle() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, AUTOMATISERBAR_ÅRSAK, false);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_ANDRE_LIVSOPPHOLDSYTELSER_OPPHØR);
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    @Test
    void skal_sette_pa_vent_nar_periode_venter_pa_etterlysning() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        var avklaring = lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, AUTOMATISERBAR_ÅRSAK, true);

        var frist = LocalDateTime.of(2026, 2, 15, 12, 0);
        var ventendeEtterlysning = EtterlysningData.utenUttalelse(
            EtterlysningStatus.VENTER,
            frist,
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0)
        );
        steg = lagSteg(List.of(ventendeEtterlysning));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .containsExactly(EtterlysningType.UTTALELSE_ANDRE_LIVSOPPHOLDSYTELSER.tilAutopunktDefinisjon());
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().getFirst().getFrist()).isEqualTo(frist);
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    @Test
    void skal_kaste_nar_etterlysning_peker_pa_en_annen_avklaring() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, AUTOMATISERBAR_ÅRSAK, true);

        var etterlysningMedFeilReferanse = EtterlysningData.utenUttalelse(
            EtterlysningStatus.UTLØPT,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0)
        );
        steg = lagSteg(List.of(etterlysningMedFeilReferanse));

        assertThatThrownBy(() -> utførSteg(behandling))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ulik grunnlagsreferanse");
    }

    @Test
    void skal_utfores_uten_aksjonspunkt_nar_ingen_perioder_til_vurdering() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    @Test
    void skal_gi_opphørsaksjonspunkt_nar_hele_den_manuelle_tidslinjen_har_foreslatt_avklaring() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VILKÅR, Set.of(
            lagAvklaring(FOM, TOM, AUTOMATISERBAR_ÅRSAK, false),
            lagAvklaring(fom2, tom2, AUTOMATISERBAR_ÅRSAK, false)
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_ANDRE_LIVSOPPHOLDSYTELSER_OPPHØR);
    }

    @Test
    void skal_ikke_regne_delvis_dekket_tidslinje_som_dekket_av_foreslatt_avklaring() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var avklaring = lagAvklaring(FOM, TOM, AUTOMATISERBAR_ÅRSAK, true);

        var blandetTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(FOM, TOM, new VilkårsavklaringUtfallUtleder(VILKÅR, avklaring)),
            new LocalDateSegment<>(fom2, tom2, new VilkårsavklaringUtfallUtleder(VILKÅR, null))));

        assertThat(VurderAndreLivsoppholdsytelserSteg.erDekketAvForeslåttAvklaring(blandetTidslinje)).isFalse();
        assertThat(VurderAndreLivsoppholdsytelserSteg.erDekketAvForeslåttAvklaring(
            blandetTidslinje.intersection(new LocalDateTimeline<>(FOM, TOM, Boolean.TRUE)))).isTrue();
    }

    private Set<AndreLivsoppholdsytelserResultatPeriode> hentVurderinger(Behandling behandling) {
        return inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId())
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getAndreLivsoppholdsytelserResultatHolder)
            .map(AndreLivsoppholdsytelserResultatHolder::getVurderinger)
            .orElse(Set.of());
    }

    private VilkårPeriodeAvklaring lagreForeslåttAvklaring(long behandlingId, LocalDate fom, LocalDate tom, AndreLivsoppholdsytelserIkkeOppfyltÅrsak årsak, boolean skalSendeVarsel) {
        var lagret = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, VILKÅR,
            Set.of(lagAvklaring(fom, tom, årsak, skalSendeVarsel)));
        return lagret.stream().findFirst().orElseThrow();
    }

    private VilkårPeriodeAvklaringForeslått lagAvklaring(LocalDate fom, LocalDate tom, AndreLivsoppholdsytelserIkkeOppfyltÅrsak årsak, boolean skalSendeVarsel) {
        return new VilkårPeriodeAvklaringForeslått(
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            årsak.getKode(),
            "Begrunnelse for relevante fakta lagt til grunn i avklaring",
            skalSendeVarsel,
            skalSendeVarsel ? "Fritekst til varselet" : null,
            skalSendeVarsel ? null : "Begrunnelse for ikke varsling",
            AndreLivsoppholdsytelserAvklaringKildeType.NAV,
            null,
            "A12345",
            LocalDateTime.now(),
            Avklaringtype.AVSLAG
        );
    }

    private Behandling opprettBehandlingMedVilkårOgPeriode() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        var søktStartdato = new SøktStartdato(FOM, new JournalpostId("jp-vilkår"));
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato)));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode)));
        return behandling;
    }

    private Behandling opprettBehandlingMedToVilkårsperioder(LocalDate fom2, LocalDate tom2) {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VILKÅR, Utfall.IKKE_VURDERT, new Periode(fom2, tom2))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(fom2, tom2))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(fom2, tom2))
            .lagre(entityManager);

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2);
        var søktStartdato1 = new SøktStartdato(FOM, new JournalpostId("jp-vilkår-1"));
        var søktStartdato2 = new SøktStartdato(fom2, new JournalpostId("jp-vilkår-2"));
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato1, søktStartdato2));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato1, søktStartdato2)));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode1),
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode2)));
        return behandling;
    }

    private VurderAndreLivsoppholdsytelserSteg lagSteg(List<EtterlysningData> etterlysninger) {
        var vilkårTjeneste = new VilkårTjeneste(behandlingRepository, vilkårsPerioderTilVurderingTjenester, vilkårResultatRepository);
        var etterlysningTjeneste = new EtterlysningTjeneste(null, null) {
            @Override
            public List<EtterlysningData> hentGjeldendeEtterlysninger(Long behandlingId, Long fagsakId, EtterlysningType type) {
                return etterlysninger;
            }
        };

        return new VurderAndreLivsoppholdsytelserSteg(
            manuelleVilkårRekkefølgeTjeneste,
            vilkårResultatRepository,
            vilkårTjeneste,
            behandlingRepository,
            vilkårsPerioderTilVurderingTjenester,
            etterlysningTjeneste,
            vilkårsavklaringGrunnlagRepository,
            inngangsvilkårVurderingRepository,
            inngangsvilkårVurderingTjeneste,
            vilkårsutfallsporing,
            vilkårsvurderingHistorikkinnslagTjeneste
        );
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }
}
