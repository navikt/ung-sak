package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.BistandsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseV2;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring.UttalelseDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring.VilkårsavklaringDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring.VilkårsvurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring.VilkårsvurderingRadDto;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VilkårsavklaringRestTjenesteTest {

    private static final String SAKSBEHANDLER = "A111111";

    private static final Periode JAN = periode(1, 1, 1, 31);
    private static final Periode FEB = periode(2, 1, 2, 28);
    private static final Periode MAR = periode(3, 1, 3, 31);
    private static final Periode JAN_MAR = periode(1, 1, 3, 31);
    private static final Periode JAN_JUN = periode(1, 1, 6, 30);
    private static final Periode JAN_FEB = periode(1, 1, 2, 28);
    private static final Periode JAN_DES = periode(1, 1, 12, 31);
    private static final Periode MAR_DES = periode(3, 1, 12, 31);
    private static final Periode APR_JUN = periode(4, 1, 6, 30);
    private static final Periode JUL_DES = periode(7, 1, 12, 31);

    @Inject
    private EntityManager entityManager;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private UttalelseRepository uttalelseRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private VilkårsavklaringRestTjeneste tjeneste;

    private Fagsak fagsak;

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        FagsakRepository fagsakRepository = repositoryProvider.getFagsakRepository();
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        vilkårsavklaringGrunnlagRepository = new VilkårsavklaringGrunnlagRepository(entityManager);
        uttalelseRepository = new UttalelseRepository(entityManager);
        mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);

        tjeneste = new VilkårsavklaringRestTjeneste(
            behandlingRepository,
            vilkårsavklaringGrunnlagRepository,
            inngangsvilkårVurderingRepository,
            vilkårResultatRepository,
            uttalelseRepository,
            mottatteDokumentRepository);

        fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("11223344"), new Saksnummer("AVKLOVERSIKT1"),
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        fagsakRepository.opprettNy(fagsak);
    }

    @Test
    void vilkårtype_uten_vilkårsavklaring_avvises() {
        var behandling = opprettFørstegangsbehandling(JAN_MAR);

        assertThatThrownBy(() -> tjeneste.hentAvklaringer(behandling, VilkårType.ALDERSVILKÅR))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tjeneste.hentVurderinger(behandling, VilkårType.ALDERSVILKÅR))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void foreslått_avklaring_som_overlapper_ferdigstilt_leveres_sammen_med_den() {
        var behandling = opprettFørstegangsbehandling(JAN_DES);
        var a1 = UUID.randomUUID();
        var a2 = UUID.randomUUID();
        lagreForeslåttAvklaring(behandling, a1, JAN_JUN);
        ferdigstill(behandling);
        lagreForeslåttAvklaring(behandling, a2, MAR_DES);

        assertThat(tjeneste.hentAvklaringer(behandling, VilkårType.BISTANDSVILKÅR).avklaringer())
            .extracting(VilkårsavklaringDto::referanse, VilkårsavklaringDto::periode, VilkårsavklaringDto::foreslåttIBehandlingen)
            .containsExactly(
                tuple(a2, MAR_DES, true),
                tuple(a1, JAN_JUN, false));
    }

    @Test
    void avsluttet_behandling_viser_ikke_forslaget_som_ferdigstilt_i_tillegg() {
        var behandling = opprettFørstegangsbehandling(JAN_DES);
        var a1 = UUID.randomUUID();
        lagreForeslåttAvklaring(behandling, a1, JAN_DES);
        ferdigstill(behandling);

        assertThat(tjeneste.hentAvklaringer(behandling, VilkårType.BISTANDSVILKÅR).avklaringer())
            .extracting(VilkårsavklaringDto::referanse, VilkårsavklaringDto::periode, VilkårsavklaringDto::foreslåttIBehandlingen)
            .containsExactly(tuple(a1, JAN_DES, true));
    }

    @Test
    void delvis_overstyrt_ferdigstilt_avklaring_gir_en_rad_per_gjenværende_del() {
        var behandling = opprettFørstegangsbehandling(JAN_DES);
        var a1 = UUID.randomUUID();
        var a2 = UUID.randomUUID();
        lagreForeslåttAvklaring(behandling, a1, JAN_DES);
        ferdigstill(behandling);
        lagreForeslåttAvklaring(behandling, a2, APR_JUN);
        ferdigstill(behandling);

        assertThat(tjeneste.hentAvklaringer(behandling, VilkårType.BISTANDSVILKÅR).avklaringer())
            .extracting(VilkårsavklaringDto::referanse, VilkårsavklaringDto::periode, VilkårsavklaringDto::foreslåttIBehandlingen)
            .containsExactly(
                tuple(a2, APR_JUN, true),
                tuple(a1, JUL_DES, false),
                tuple(a1, JAN_MAR, false));
    }

    @Test
    void vurdering_lagret_over_hull_i_vilkårsperiodene_gir_én_rad() {
        var behandling = opprettFørstegangsbehandling(JAN, MAR);
        var a = UUID.randomUUID();
        lagreForeslåttAvklaring(behandling, a, JAN_MAR);
        var vurdertTidspunkt = LocalDateTime.of(2026, 4, 1, 12, 0);
        inngangsvilkårVurderingRepository.lagreBistandsVurderinger(behandling.getId(), List.of(
            bistandsvurdering(JAN, vurdertTidspunkt),
            bistandsvurdering(MAR, vurdertTidspunkt)));

        var rader = tjeneste.hentVurderinger(behandling, VilkårType.BISTANDSVILKÅR).perioder();

        assertThat(rader).hasSize(1);
        var rad = rader.getFirst();
        assertThat(rad.periode()).isEqualTo(JAN_MAR);
        assertThat(rad.utfall()).isEqualTo(Utfall.IKKE_VURDERT);
        assertThat(rad.avklaringOgVurdering().avklaring().referanse()).isEqualTo(a);
        assertThat(rad.avklaringOgVurdering().vurdering()).isEqualTo(new VilkårsvurderingDto(
            false,
            BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID.getKode(),
            "begrunnelse for vurdering",
            "fritekst til brev",
            true,
            SAKSBEHANDLER,
            vurdertTidspunkt));
    }

    @Test
    void vilkårsperiodene_rundt_avklaringen_blir_egne_rader() {
        var behandling = opprettFørstegangsbehandling(
            new Vilkårsperiode(JAN_MAR, Utfall.OPPFYLT, null),
            new Vilkårsperiode(APR_JUN, Utfall.IKKE_VURDERT, null),
            new Vilkårsperiode(JUL_DES, Utfall.OPPFYLT, null));
        var a = UUID.randomUUID();
        lagreForeslåttAvklaring(behandling, a, APR_JUN);

        assertThat(tjeneste.hentVurderinger(behandling, VilkårType.BISTANDSVILKÅR).perioder())
            .extracting(VilkårsvurderingRadDto::periode, VilkårsvurderingRadDto::utfall, VilkårsavklaringRestTjenesteTest::avklaringsreferanse)
            .containsExactly(
                tuple(JUL_DES, Utfall.OPPFYLT, null),
                tuple(APR_JUN, Utfall.IKKE_VURDERT, a),
                tuple(JAN_MAR, Utfall.OPPFYLT, null));
    }

    @Test
    void vilkårsperiode_som_overlapper_avklaringen_delvis_kuttes() {
        var behandling = opprettFørstegangsbehandling(new Vilkårsperiode(JAN_JUN, Utfall.OPPFYLT, null));
        var a = UUID.randomUUID();
        lagreForeslåttAvklaring(behandling, a, APR_JUN);

        assertThat(tjeneste.hentVurderinger(behandling, VilkårType.BISTANDSVILKÅR).perioder())
            .extracting(VilkårsvurderingRadDto::periode, VilkårsvurderingRadDto::utfall, VilkårsavklaringRestTjenesteTest::avklaringsreferanse)
            .containsExactly(
                tuple(APR_JUN, Utfall.OPPFYLT, a),
                tuple(JAN_MAR, Utfall.OPPFYLT, null));
    }

    @Test
    void første_vilkårsperiode_i_avklaringen_gir_utfallet() {
        var behandling = opprettFørstegangsbehandling(
            new Vilkårsperiode(JAN, Utfall.IKKE_VURDERT, null),
            new Vilkårsperiode(FEB, Utfall.OPPFYLT, null));
        var a = UUID.randomUUID();
        lagreForeslåttAvklaring(behandling, a, JAN_FEB);

        assertThat(tjeneste.hentVurderinger(behandling, VilkårType.BISTANDSVILKÅR).perioder())
            .extracting(VilkårsvurderingRadDto::periode, VilkårsvurderingRadDto::utfall, VilkårsavklaringRestTjenesteTest::avklaringsreferanse)
            .containsExactly(tuple(JAN_FEB, Utfall.IKKE_VURDERT, a));
    }

    @Test
    void ikke_relevante_og_avkortede_vilkårsperioder_utelates() {
        var behandling = opprettFørstegangsbehandling(
            new Vilkårsperiode(JAN_MAR, Utfall.OPPFYLT, null),
            new Vilkårsperiode(APR_JUN, Utfall.IKKE_OPPFYLT, Avslagsårsak.AVKORTET),
            new Vilkårsperiode(JUL_DES, Utfall.IKKE_RELEVANT, null));

        assertThat(tjeneste.hentVurderinger(behandling, VilkårType.BISTANDSVILKÅR).perioder())
            .extracting(VilkårsvurderingRadDto::periode, VilkårsvurderingRadDto::utfall, VilkårsvurderingRadDto::avklaringOgVurdering)
            .containsExactly(tuple(JAN_MAR, Utfall.OPPFYLT, null));
    }

    @Test
    void uttalelse_kobles_på_referansen_med_mottatt_tidspunkt_fra_journalposten() {
        var behandling = opprettFørstegangsbehandling(JAN_DES);
        var a = UUID.randomUUID();
        lagreForeslåttAvklaring(behandling, a, JAN_DES);
        var mottattTidspunkt = LocalDateTime.of(2026, 2, 3, 10, 15);
        lagreUttalelse(behandling, a, JAN_DES, mottattTidspunkt);
        var forventet = new UttalelseDto(true, "tekst", mottattTidspunkt);

        assertThat(tjeneste.hentAvklaringer(behandling, VilkårType.BISTANDSVILKÅR).avklaringer())
            .extracting(VilkårsavklaringDto::uttalelse)
            .containsExactly(forventet);
        assertThat(tjeneste.hentVurderinger(behandling, VilkårType.BISTANDSVILKÅR).perioder())
            .extracting(rad -> rad.avklaringOgVurdering().avklaring().uttalelse())
            .containsExactly(forventet);
    }

    @Test
    void ferdigstilt_avklaring_beholder_uttalelsen_sin() {
        var behandling = opprettFørstegangsbehandling(JAN_DES);
        var a = UUID.randomUUID();
        var a2 = UUID.randomUUID();
        lagreForeslåttAvklaring(behandling, a, JAN_JUN);
        var mottattTidspunkt = LocalDateTime.of(2026, 2, 3, 10, 15);
        lagreUttalelse(behandling, a, JAN_JUN, mottattTidspunkt);
        ferdigstill(behandling);
        lagreForeslåttAvklaring(behandling, a2, JUL_DES);

        assertThat(tjeneste.hentAvklaringer(behandling, VilkårType.BISTANDSVILKÅR).avklaringer())
            .extracting(VilkårsavklaringDto::referanse, VilkårsavklaringDto::foreslåttIBehandlingen, VilkårsavklaringDto::uttalelse)
            .containsExactly(
                tuple(a2, true, null),
                tuple(a, false, new UttalelseDto(true, "tekst", mottattTidspunkt)));
    }

    private record Vilkårsperiode(Periode periode, Utfall utfall, Avslagsårsak avslagsårsak) {
    }

    private Behandling opprettFørstegangsbehandling(Periode... vilkårsperioder) {
        return opprettFørstegangsbehandling(Arrays.stream(vilkårsperioder)
            .map(p -> new Vilkårsperiode(p, Utfall.IKKE_VURDERT, null))
            .toArray(Vilkårsperiode[]::new));
    }

    private Behandling opprettFørstegangsbehandling(Vilkårsperiode... vilkårsperioder) {
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var vilkårResultatBuilder = Vilkårene.builder();
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.BISTANDSVILKÅR);
        for (var vilkårsperiode : vilkårsperioder) {
            var periodeBuilder = new VilkårPeriodeBuilder()
                .medPeriode(tilDatoIntervallEntitet(vilkårsperiode.periode()))
                .medUtfall(vilkårsperiode.utfall());
            if (vilkårsperiode.avslagsårsak() != null) {
                periodeBuilder.medAvslagsårsak(vilkårsperiode.avslagsårsak());
            }
            vilkårBuilder.leggTil(periodeBuilder);
        }
        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());
        return behandling;
    }

    private void lagreForeslåttAvklaring(Behandling behandling, UUID referanse, Periode periode) {
        var årsak = BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID;
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BISTANDSVILKÅR, Set.of(
            new VilkårPeriodeAvklaringForeslått(
                referanse,
                tilDatoIntervallEntitet(periode),
                årsak.getKode(),
                "begrunnelse for avklaring",
                true,
                årsak.kreverFritekst() ? "fritekst til varsel" : null,
                null,
                BistandsavklaringKildeType.BRUKER,
                null,
                SAKSBEHANDLER,
                LocalDateTime.now(),
                Avklaringtype.OPPHØR)));
    }

    private void ferdigstill(Behandling behandling) {
        vilkårsavklaringGrunnlagRepository.ferdigstillForeslåtteAvklaringer(behandling.getId(), VilkårType.BISTANDSVILKÅR);
    }

    private void lagreUttalelse(Behandling behandling, UUID referanse, Periode periode, LocalDateTime mottattTidspunkt) {
        var journalpostId = new JournalpostId("123456789");
        var dokument = new MottattDokument.Builder()
            .medJournalPostId(journalpostId)
            .medType(Brevkode.AKTIVITETSPENGER_VARSEL_UTTALELSE)
            .medMottattDato(mottattTidspunkt.toLocalDate())
            .medMottattTidspunkt(mottattTidspunkt)
            .medFagsakId(fagsak.getId())
            .medBehandlingId(behandling.getId())
            .build();
        mottatteDokumentRepository.lagre(dokument, DokumentStatus.GYLDIG);
        uttalelseRepository.lagre(behandling.getId(), new UttalelseV2(
            true, "tekst", tilDatoIntervallEntitet(periode), journalpostId, EndringType.AVKLAR_BISTAND, referanse));
    }

    private static BistandsvilkårResultatPeriode bistandsvurdering(Periode periode, LocalDateTime vurdertTidspunkt) {
        return new BistandsvilkårResultatPeriode(
            tilDatoIntervallEntitet(periode),
            false,
            BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID,
            true,
            "begrunnelse for vurdering",
            "fritekst til brev",
            SAKSBEHANDLER,
            vurdertTidspunkt);
    }

    private static UUID avklaringsreferanse(VilkårsvurderingRadDto rad) {
        return rad.avklaringOgVurdering() == null ? null : rad.avklaringOgVurdering().avklaring().referanse();
    }

    private static Periode periode(int fomMåned, int fomDag, int tomMåned, int tomDag) {
        return new Periode(LocalDate.of(2026, fomMåned, fomDag), LocalDate.of(2026, tomMåned, tomDag));
    }

    private static DatoIntervallEntitet tilDatoIntervallEntitet(Periode periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }
}
