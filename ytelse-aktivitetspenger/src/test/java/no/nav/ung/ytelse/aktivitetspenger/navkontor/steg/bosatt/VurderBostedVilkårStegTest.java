package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bosatt;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.*;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinje;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandingprosessSporingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedSøknadsfaktaGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
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
import no.nav.ung.ytelse.aktivitetspenger.navkontor.AktivitetspengerVilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfall;
import no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfallUtleder;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderBostedVilkårStegTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);

    @Inject
    private EntityManager entityManager;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
    private AktivitetspengerVilkårsPerioderTilVurderingTjeneste aktivitetspengerVilkårsPerioderTilVurderingTjeneste;

    @Inject
    private @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    @Inject
    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;

    @Inject
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;
    @Inject
    private HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private VilkårResultatRepository vilkårResultatRepository;
    @Inject
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    @Inject
    private BostedSøknadsfaktaGrunnlagRepository bostedSøknadsfaktaGrunnlagRepository;
    @Inject
    private StartdatoRepository startdatoRepository;
    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;
    @Inject
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    @Inject
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    @Inject
    private BehandingprosessSporingRepository behandlingprosessSporingRepository;

    private VurderBostedVilkårSteg steg;

    @BeforeEach
    void setUp() {
        steg = lagSteg(List.of());
    }

    @Test
    void skal_ikke_utføre_vilkårsvurdering_automatisk_for_nye_perioder() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);

        steg = lagSteg(List.of());
        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_BOSTEDVILKÅR);
        var vilkårVurderingResultat = inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId());
        var inngangsvilkårVurderinger = vilkårVurderingResultat.map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBostedsvilkårResultatPerioder);
        assertThat(inngangsvilkårVurderinger.get()).isEmpty();

        var vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.BOSTEDSVILKÅR).get().getPerioder().stream();
        assertThat(vilkår).allMatch(it -> it.getGjeldendeUtfall() == Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_utføre_vilkårsvurdering_automatisk_når_standardkode_og_bruker_ikke_har_uttalelse() {
        var behandling = opprettBehandlingMedVilkårOgPeriode(BehandlingÅrsakType.ENDRET_BOSTED);
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);

        var avklaring = lagBostedsPeriodeAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Set.of(avklaring));

        var frist = LocalDateTime.of(2026, 2, 15, 12, 0);
        var ventendeEtterlysning = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            frist,
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(false, null, new JournalpostId("jp-uttalelse-1"))
        );
        steg = lagSteg(List.of(ventendeEtterlysning));
        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var vilkårVurderingResultat = inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId());
        var bostedsvurdering = vilkårVurderingResultat.map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBostedsvilkårResultatPerioder).map(Collection::stream).orElseThrow().findFirst().orElseThrow();
        assertThat(bostedsvurdering.getPeriode().getFomDato()).isEqualTo(FOM);
        assertThat(bostedsvurdering.getPeriode().getTomDato()).isEqualTo(TOM);
        assertThat(bostedsvurdering.getIkkeOppfyltÅrsak()).isEqualTo(BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM);

        List<Historikkinnslag> historikkinnslag = historikkinnslagRepository.hent(behandling.getId());
        assertThat(historikkinnslag).hasSize(1);
        Historikkinnslag innslaget = historikkinnslag.getFirst();
        assertThat(innslaget.getAktør()).isEqualByComparingTo(HistorikkAktør.VEDTAKSLØSNINGEN);
        assertThat(innslaget.getLinjer()).containsOnly(HistorikkinnslagLinje.tekst("Perioden 01.01.2026 - 31.01.2026 ble vurdert til Ikke oppfylt. Søker har ikke bostedsadresse i Trondheim kommune.", 0));
    }

    @Test
    void skal_ikke_utføre_vilkårsvurdering_automatisk_når_bruker_har_uttalelse() {
        var behandling = opprettBehandlingMedVilkårOgPeriode(BehandlingÅrsakType.ENDRET_BOSTED);
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);

        var avklaring = lagBostedsPeriodeAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Set.of(avklaring));

        var frist = LocalDateTime.of(2026, 2, 15, 12, 0);
        var ventendeEtterlysning = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            frist,
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(true, "Jeg bor i Trondheim", new JournalpostId("jp-uttalelse-1"))
        );
        steg = lagSteg(List.of(ventendeEtterlysning));
        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_BOSTEDSVILKÅR_OPPHØR);
        var vilkårVurderingResultat = inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId());
        var inngangsvilkårVurderinger = vilkårVurderingResultat.map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBostedsvilkårResultatPerioder);
        assertThat(inngangsvilkårVurderinger.get()).isEmpty();

        var vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.BOSTEDSVILKÅR).get().getPerioder().stream();
        assertThat(vilkår).allMatch(it -> it.getGjeldendeUtfall() == Utfall.IKKE_VURDERT);

        assertThat(historikkinnslagRepository.hent(behandling.getId())).isEmpty();
    }

    @Test
    void skal_ikke_utføre_vilkårsvurdering_automatisk_når_det_er_valgt_å_ikke_varsle() {
        var behandling = opprettBehandlingMedVilkårOgPeriode(BehandlingÅrsakType.ENDRET_BOSTED);
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);

        var avklaring = lagBostedsPeriodeAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, false);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Set.of(avklaring));

        var frist = LocalDateTime.of(2026, 2, 15, 12, 0);
        var ventendeEtterlysning = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            frist,
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(false, null, new JournalpostId("jp-uttalelse-1"))
        );
        steg = lagSteg(List.of(ventendeEtterlysning));
        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_BOSTEDSVILKÅR_OPPHØR);
        var vilkårVurderingResultat = inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId());
        var inngangsvilkårVurderinger = vilkårVurderingResultat.map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBostedsvilkårResultatPerioder);
        assertThat(inngangsvilkårVurderinger.get()).isEmpty();
    }

    @Test
    void skal_sette_pa_vent_nar_periode_venter_pa_etterlysning() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Set.of(
            lagBostedsPeriodeAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, false)
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
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Set.of(
            lagBostedsPeriodeAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true),
            lagBostedsPeriodeAvklaring(fom2, tom2, BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM, true)
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
            .doesNotContain(AksjonspunktDefinisjon.VURDER_BOSTEDVILKÅR, AksjonspunktDefinisjon.VURDER_BOSTEDSVILKÅR_OPPHØR);
    }

    @Test
    void skal_vurdere_ny_søknadsperiode_selv_om_annen_periode_har_foreslått_avklaring() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2);
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);

        // Kun første periode har en foreslått avklaring fra saksbehandler. Andre periode har bare søknadsfakta.
        var avklaring = lagBostedsPeriodeAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Set.of(avklaring));

        var frist = LocalDateTime.of(2026, 2, 15, 12, 0);
        var ventendeEtterlysning = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            frist,
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(false, null, new JournalpostId("jp-uttalelse-1"))
        );
        steg = lagSteg(List.of(ventendeEtterlysning));
        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_BOSTEDVILKÅR);

        var vilkårVurderingResultat = inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId());
        var inngangsvilkårVurderinger = vilkårVurderingResultat
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBostedsvilkårResultatPerioder)
            .orElseThrow();
        assertThat(inngangsvilkårVurderinger).hasSize(1);
        var vurdertPeriode = inngangsvilkårVurderinger.iterator().next();
        assertThat(vurdertPeriode.getPeriode().getFomDato()).isEqualTo(FOM);
        assertThat(vurdertPeriode.getPeriode().getTomDato()).isEqualTo(TOM);
        assertThat(vurdertPeriode.getIkkeOppfyltÅrsak()).isEqualTo(BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM);

        // Vilkårsresultatet settes først når den nye søknadsperioden er vurdert i 5140. Vilkårsperiodene ligger kant i kant
        // med likt utfall og er slått sammen, så de sjekkes samlet.
        var vilkårPerioder = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.BOSTEDSVILKÅR).get().getPerioder();
        assertThat(vilkårPerioder).allMatch(p -> p.getGjeldendeUtfall() == Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_gi_opphørsaksjonspunkt_når_hele_den_manuelle_tidslinjen_har_foreslått_avklaring() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2, BehandlingÅrsakType.ENDRET_BOSTED, BehandlingÅrsakType.ENDRET_BOSTED);
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Set.of(
            lagBostedsPeriodeAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.ANNET, true),
            lagBostedsPeriodeAvklaring(fom2, tom2, BostedsvilkårIkkeOppfyltÅrsak.ANNET, true)
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_BOSTEDSVILKÅR_OPPHØR);
    }

    @Test
    void skal_gi_avslagsaksjonspunkt_når_ingen_manuell_periode_har_foreslått_avklaring() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2);
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .as("saksbehandler kan ha avklart fakta uten å foreslå en vurdering; da må avslagsårsaken oppgis i 5140")
            .containsExactly(AksjonspunktDefinisjon.VURDER_BOSTEDVILKÅR);
    }

    @Test
    void skal_ikke_lagre_uttalelsestekst_i_sporing() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);
        var avklaring = lagBostedsPeriodeAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Set.of(avklaring));

        steg = lagSteg(List.of(new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(true, "Hemmelig uttalelsestekst", new JournalpostId("jp-uttalelse-1")))));
        utførSteg(behandling);

        var input = behandlingprosessSporingRepository.hentSporinger(behandling.getId(), "VURDER_BOSTEDVILKÅR").getFirst().getInput();
        assertThat(input)
            .doesNotContain("Hemmelig uttalelsestekst")
            .doesNotContain("jp-uttalelse-1")
            .contains("harUttalelse")
            .contains("etterlysningStatus");
    }

    @Test
    void skal_vurdere_avklart_periode_før_ny_søknadsperiode_når_begge_vurderes_manuelt() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2, BehandlingÅrsakType.ENDRET_BOSTED, BehandlingÅrsakType.NY_SØKT_PERIODE);
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);
        var avklaring = lagBostedsPeriodeAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Set.of(avklaring));
        steg = lagSteg(List.of(mottattUttalelse(avklaring.getReferanse())));

        assertThat(utførSteg(behandling).getAksjonspunktListe())
            .as("avklart periode vurderes først, alene")
            .containsExactly(AksjonspunktDefinisjon.VURDER_BOSTEDSVILKÅR_OPPHØR);
    }

    @Test
    void skal_gi_avslagsaksjonspunkt_når_automatisk_opphør_dekker_hele_ny_søknadsperiode() {
        var behandling = opprettBehandlingMedVilkårOgPeriode(BehandlingÅrsakType.NY_SØKT_PERIODE);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);
        var avklaring = lagBostedsPeriodeAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Set.of(avklaring));
        steg = lagSteg(List.of(new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(false, null, new JournalpostId("jp-uttalelse-1")))));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .as("den nye søknadsperioden skal vurderes selv om avklaringen ga automatisk opphør")
            .containsExactly(AksjonspunktDefinisjon.VURDER_BOSTEDVILKÅR);
        assertThat(hentBostedsvurderinger(behandling)).hasSize(1);
        var vurdering = hentBostedsvurderinger(behandling).stream().findFirst().orElseThrow();
        assertThat(vurdering.erManuellVurdering()).isFalse();
        var evaluering = new VilkårJsonObjectMapper().readValue(vurdering.getRegelsporing().orElseThrow().evaluering(), VilkårsavklaringUtfallUtleder.RegelEvaluering.class);
        assertThat(evaluering.utfall())
            .as("begge utfallene skal spores, uten at det ene overskriver det andre")
            .containsExactlyInAnyOrder(VilkårsavklaringUtfall.AVSLÅS_AUTOMATISK, VilkårsavklaringUtfall.VURDERES_MANUELT_NY_PERIODE);
    }

    @Test
    void skal_ikke_vurdere_periode_uten_avklaring_når_bare_avklaringsårsaken_er_trigger() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2, BehandlingÅrsakType.ENDRET_BOSTED, BehandlingÅrsakType.ENDRET_BOSTED);
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);
        var avklaring = lagBostedsPeriodeAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Set.of(avklaring));

        steg = lagSteg(List.of(new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(false, null, new JournalpostId("jp-uttalelse-1")))));
        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(hentBostedsvurderinger(behandling)).extracting(v -> v.getPeriode())
            .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
    }

    @Test
    void skal_vurdere_avklaring_utenfor_triggerperioden() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2, null, BehandlingÅrsakType.NY_SØKT_PERIODE);
        bostedSøknadsfaktaGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);
        var avklaring = lagBostedsPeriodeAvklaring(FOM, TOM, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Set.of(avklaring));

        steg = lagSteg(List.of(new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(false, null, new JournalpostId("jp-uttalelse-1")))));
        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_BOSTEDVILKÅR);
        assertThat(hentBostedsvurderinger(behandling)).extracting(v -> v.getPeriode())
            .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
    }

    private Behandling opprettBehandlingMedVilkårOgPeriode() {
        return opprettBehandlingMedVilkårOgPeriode(BehandlingÅrsakType.NY_SØKT_PERIODE);
    }

    private Behandling opprettBehandlingMedVilkårOgPeriode(BehandlingÅrsakType årsak) {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        var søktStartdato = new SøktStartdato(FOM, new JournalpostId("jp-vilkår"));
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato)));
        prosessTriggereRepository.leggTil(behandling.getId(), java.util.Set.of(
            new Trigger(årsak, periode)));
        return behandling;
    }

    private Collection<BostedsvilkårResultatPeriode> hentBostedsvurderinger(Behandling behandling) {
        return inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId())
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBostedsvilkårResultatPerioder)
            .orElse(Set.of());
    }

    private Behandling opprettBehandlingMedToVilkårsperioder(LocalDate fom2, LocalDate tom2) {
        return opprettBehandlingMedToVilkårsperioder(fom2, tom2, BehandlingÅrsakType.NY_SØKT_PERIODE, BehandlingÅrsakType.NY_SØKT_PERIODE);
    }

    // Årsak null gir perioden ingen prosesstrigger.
    private Behandling opprettBehandlingMedToVilkårsperioder(LocalDate fom2, LocalDate tom2, BehandlingÅrsakType årsakPeriode1, BehandlingÅrsakType årsakPeriode2) {
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
        var søktStartdato1 = new SøktStartdato(FOM, new JournalpostId("jp-vilkår-1"));
        var søktStartdato2 = new SøktStartdato(fom2, new JournalpostId("jp-vilkår-2"));
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato1, søktStartdato2));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato1, søktStartdato2)));
        var triggere = new HashSet<Trigger>();
        if (årsakPeriode1 != null) {
            triggere.add(new Trigger(årsakPeriode1, periode1));
        }
        if (årsakPeriode2 != null) {
            triggere.add(new Trigger(årsakPeriode2, periode2));
        }
        prosessTriggereRepository.leggTil(behandling.getId(), triggere);
        return behandling;
    }

    private static EtterlysningData mottattUttalelse(UUID avklaringReferanse) {
        return new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            avklaringReferanse,
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(true, "Jeg bor i Trondheim", new JournalpostId("jp-uttalelse-1")));
    }

    private VurderBostedVilkårSteg lagSteg(List<EtterlysningData> etterlysninger) {
        var vilkårTjeneste = new VilkårTjeneste(behandlingRepository, vilkårsPerioderTilVurderingTjenester, vilkårResultatRepository);
        var etterlysningTjeneste = new EtterlysningTjeneste(null, null) {
            @Override
            public List<EtterlysningData> hentGjeldendeEtterlysninger(Long behandlingId, Long fagsakId, EtterlysningType type) {
                return etterlysninger;
            }
        };

        return new VurderBostedVilkårSteg(
            manuelleVilkårRekkefølgeTjeneste,
            vilkårResultatRepository,
            vilkårTjeneste,
            behandlingRepository,
            vilkårsavklaringGrunnlagRepository,
            vilkårsPerioderTilVurderingTjenester,
            etterlysningTjeneste,
            inngangsvilkårVurderingRepository,
            inngangsvilkårVurderingTjeneste,
            behandlingprosessSporingRepository,
            vilkårsvurderingHistorikkinnslagTjeneste,
            aktivitetspengerVilkårsPerioderTilVurderingTjeneste
        );
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }

    private VilkårPeriodeAvklaringForeslått lagBostedsPeriodeAvklaring(
            LocalDate fom, LocalDate tom,
            BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
            boolean skalSendeVarsel) {

        return new VilkårPeriodeAvklaringForeslått(
                UUID.randomUUID(),
                DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
                ikkeOppfyltÅrsak.getKode(),
                "Begrunnelse for relevante fakta lagt til grunn i avklaring",
                skalSendeVarsel,
                skalSendeVarsel && BostedsvilkårIkkeOppfyltÅrsak.ANNET.equals(ikkeOppfyltÅrsak) ? "Fritekst til varselet" : null,
                skalSendeVarsel ? null : "Fritekst for ikke varsling",
                BostedsavklaringKildeType.BRUKER,
                null,
                "A12345",
                LocalDateTime.now(),
                Avklaringtype.AVSLAG
        );
    }
}
