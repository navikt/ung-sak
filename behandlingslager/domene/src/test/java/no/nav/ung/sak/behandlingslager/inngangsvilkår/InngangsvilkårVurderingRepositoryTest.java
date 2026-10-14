package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@CdiDbAwareTest
class InngangsvilkårVurderingRepositoryTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private InngangsvilkårVurderingRepository repository;

    private Behandling behandling;

    private static final DatoIntervallEntitet PERIODE_1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));
    private static final DatoIntervallEntitet PERIODE_2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 12, 31));
    private static final String VURDERT_AV = "saksbehandler1";
    private static final LocalDateTime VURDERT_TIDSPUNKT = LocalDateTime.of(2024, 6, 1, 10, 0);

    @BeforeEach
    void setUp() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("1"), new Saksnummer("SAK1"), LocalDate.now(), LocalDate.now().plusYears(1).minusDays(1));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, new BehandlingLås(null));
    }

    @Test
    void skal_lagre_og_hente_bistandsvurdering() {
        var vurdering = new BistandsvilkårResultatPeriode(PERIODE_1, true, null, true, null, null, VURDERT_AV, VURDERT_TIDSPUNKT, null);
        repository.lagreBistandsVurderinger(behandling.getId(), List.of(vurdering));

        var grunnlag = repository.hentEksisterendeGrunnlag(behandling.getId());

        assertThat(grunnlag).isPresent();
        var holder = grunnlag.get().getBistandsvilkårResultatHolder();
        assertThat(holder).isPresent();
        assertThat(holder.get().getVurderinger()).hasSize(1);
        var lagretVurdering = holder.get().getVurderinger().iterator().next();
        assertThat(lagretVurdering.getPeriode()).isEqualTo(PERIODE_1);
        assertThat(lagretVurdering.isGodkjent()).isTrue();
        assertThat(lagretVurdering.getIkkeOppfyltÅrsak()).isNull();
        assertThat(lagretVurdering.getVurdertAv()).isEqualTo(VURDERT_AV);
        assertThat(lagretVurdering.getVurdertTidspunkt()).isEqualTo(VURDERT_TIDSPUNKT);
    }

    @Test
    void skal_lagre_bistandsvurdering_med_avslagsårsak() {
        var vurdering = new BistandsvilkårResultatPeriode(PERIODE_1, false, BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID, true, null, null, VURDERT_AV, VURDERT_TIDSPUNKT, null);
        repository.lagreBistandsVurderinger(behandling.getId(), List.of(vurdering));

        var holder = repository.hentEksisterendeGrunnlag(behandling.getId())
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBistandsvilkårResultatHolder)
            .orElseThrow();

        assertThat(holder.getVurderinger()).hasSize(1);
        var lagretVurdering = holder.getVurderinger().iterator().next();
        assertThat(lagretVurdering.isGodkjent()).isFalse();
        assertThat(lagretVurdering.getIkkeOppfyltÅrsak()).isEqualTo(BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID);
    }

    @Test
    void skal_lagre_og_hente_livsoppholdsytelsevurdering() {
        var vurdering = new AndreLivsoppholdsytelserResultatPeriode(PERIODE_1, false, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER, true, null, null, null, VURDERT_AV, VURDERT_TIDSPUNKT);
        repository.lagreYtelseVurderinger(behandling.getId(), List.of(vurdering));

        var grunnlag = repository.hentEksisterendeGrunnlag(behandling.getId());

        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getBistandsvilkårResultatHolder()).as("bistandsholder skal være tom").isEmpty();
        var holder = grunnlag.get().getAndreLivsoppholdsytelserResultatHolder();
        assertThat(holder).isPresent();
        assertThat(holder.get().getVurderinger()).hasSize(1);
        var lagretVurdering = holder.get().getVurderinger().iterator().next();
        assertThat(lagretVurdering.isGodkjent()).isFalse();
        assertThat(lagretVurdering.getIkkeOppfyltÅrsak()).isEqualTo(AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER);
    }

    @Test
    void oppdatering_av_bistand_bevarer_livsopphold_holder() {
        var livsoppholdVurdering = new AndreLivsoppholdsytelserResultatPeriode(PERIODE_1, true, null, true, null, null, null, VURDERT_AV, VURDERT_TIDSPUNKT);
        repository.lagreYtelseVurderinger(behandling.getId(), List.of(livsoppholdVurdering));

        var bistandVurdering = new BistandsvilkårResultatPeriode(PERIODE_1, true, null, true, null, null, VURDERT_AV, VURDERT_TIDSPUNKT, null);
        repository.lagreBistandsVurderinger(behandling.getId(), List.of(bistandVurdering));

        var grunnlag = repository.hentEksisterendeGrunnlag(behandling.getId()).orElseThrow();
        assertThat(grunnlag.getBistandsvilkårResultatHolder()).isPresent();
        assertThat(grunnlag.getAndreLivsoppholdsytelserResultatHolder())
            .as("livsopphold-holder skal bevares ved oppdatering av bistand")
            .isPresent();
    }

    @Test
    void oppdatering_av_livsopphold_bevarer_bistand_holder() {
        var bistandVurdering = new BistandsvilkårResultatPeriode(PERIODE_1, true, null, true, null, null, VURDERT_AV, VURDERT_TIDSPUNKT, null);
        repository.lagreBistandsVurderinger(behandling.getId(), List.of(bistandVurdering));

        var livsoppholdVurdering = new AndreLivsoppholdsytelserResultatPeriode(PERIODE_2, false, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER, true, null, null, null, VURDERT_AV, VURDERT_TIDSPUNKT);
        repository.lagreYtelseVurderinger(behandling.getId(), List.of(livsoppholdVurdering));

        var grunnlag = repository.hentEksisterendeGrunnlag(behandling.getId()).orElseThrow();
        assertThat(grunnlag.getAndreLivsoppholdsytelserResultatHolder()).isPresent();
        assertThat(grunnlag.getBistandsvilkårResultatHolder())
            .as("bistand-holder skal bevares ved oppdatering av livsopphold")
            .isPresent();
    }

    @Test
    void oppdatering_erstatter_tidligere_bistandsvurdering() {
        repository.lagreBistandsVurderinger(behandling.getId(),
            List.of(new BistandsvilkårResultatPeriode(PERIODE_1, true, null, true, null, null, VURDERT_AV, VURDERT_TIDSPUNKT, null)));

        repository.lagreBistandsVurderinger(behandling.getId(),
            List.of(
                new BistandsvilkårResultatPeriode(PERIODE_1, false, BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID, true, null, null, "saksbehandler2", VURDERT_TIDSPUNKT.plusHours(1), null),
                new BistandsvilkårResultatPeriode(PERIODE_2, true, null, true, null, null, "saksbehandler2", VURDERT_TIDSPUNKT.plusHours(1), null)
            ));

        var holder = repository.hentEksisterendeGrunnlag(behandling.getId())
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBistandsvilkårResultatHolder)
            .orElseThrow();

        assertThat(holder.getVurderinger()).hasSize(2);
    }

    @Test
    void oppdatering_av_ny_periode_beholder_eksisterende_uberørt_periode() {
        repository.lagreBistandsVurderinger(behandling.getId(),
            List.of(
                new BistandsvilkårResultatPeriode(PERIODE_1, true, null, true, null, null, VURDERT_AV, VURDERT_TIDSPUNKT, null),
                new BistandsvilkårResultatPeriode(PERIODE_2, true, null, true, null, null, VURDERT_AV, VURDERT_TIDSPUNKT, null)
            ));
        var opprinneligHolder = repository.hentEksisterendeGrunnlag(behandling.getId())
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBistandsvilkårResultatHolder)
            .orElseThrow();
        entityManager.flush();
        entityManager.clear();


        // Kun PERIODE_2 oppdateres — PERIODE_1 skal beholdes fra eksisterende
        repository.lagreBistandsVurderinger(behandling.getId(),
            List.of(new BistandsvilkårResultatPeriode(PERIODE_2, false, BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID, true, null, null, "saksbehandler2", VURDERT_TIDSPUNKT.plusHours(1), null)));

        BistandsvilkårResultatHolder nyHolder = repository.hentEksisterendeGrunnlag(behandling.getId())
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBistandsvilkårResultatHolder)
            .orElseThrow();
        var vurderinger = nyHolder
            .getVurderinger();

        assertThat(vurderinger).hasSize(2);
        var periode2Vurdering = vurderinger.stream().filter(v -> v.getPeriode().equals(PERIODE_2)).findFirst().orElseThrow();
        assertThat(periode2Vurdering.isGodkjent()).isFalse();
        assertThat(periode2Vurdering.getVurdertAv()).isEqualTo("saksbehandler2");

        var periode1Vurdering = vurderinger.stream().filter(v -> v.getPeriode().equals(PERIODE_1)).findFirst().orElseThrow();
        assertThat(periode1Vurdering.isGodkjent()).isTrue();
        assertThat(periode1Vurdering.getVurdertAv()).isEqualTo(VURDERT_AV);

        entityManager.flush();
        entityManager.clear();

        //sikrer at det ikke er endret på opprinnelig holder. Endring på opprinnelig holder kan i verste fall endre på
        //grunnlag for tidligere behandling, siden holdere
        opprinneligHolder = entityManager.find(BistandsvilkårResultatHolder.class, opprinneligHolder.getId());
        assertThat(opprinneligHolder.getVurderinger()).hasSize(2);


    }

    @Test
    void skal_kopiere_grunnlag_til_ny_behandling() {
        repository.lagreBistandsVurderinger(behandling.getId(),
            List.of(new BistandsvilkårResultatPeriode(PERIODE_1, true, null, true, null, null, VURDERT_AV, VURDERT_TIDSPUNKT, null)));
        repository.lagreYtelseVurderinger(behandling.getId(),
            List.of(new AndreLivsoppholdsytelserResultatPeriode(PERIODE_1, false, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER, true, null, null, null, VURDERT_AV, VURDERT_TIDSPUNKT)));

        var revurdering = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurdering, new BehandlingLås(null));
        repository.kopier(behandling.getId(), revurdering.getId());

        var kopiert = repository.hentEksisterendeGrunnlag(revurdering.getId()).orElseThrow();
        assertThat(kopiert.getBistandsvilkårResultatHolder()).isPresent();
        assertThat(kopiert.getAndreLivsoppholdsytelserResultatHolder()).isPresent();

        var original = repository.hentEksisterendeGrunnlag(behandling.getId()).orElseThrow();
        assertThat(kopiert.getBistandsvilkårResultatHolder().get().getId())
            .as("Kopiert grunnlag skal dele samme bistand-holder")
            .isEqualTo(original.getBistandsvilkårResultatHolder().get().getId());
    }

    @Test
    void skal_lagre_og_hente_regelsporing_på_bistandsvurdering() {
        var regelsporing = new Regelsporing("{\"a\":1}", "{\"b\":2}");
        var vurdering = new BistandsvilkårResultatPeriode(PERIODE_1, true, null, false, null, null, VURDERT_AV, VURDERT_TIDSPUNKT, regelsporing);
        repository.lagreBistandsVurderinger(behandling.getId(), List.of(vurdering));

        var holder = repository.hentEksisterendeGrunnlag(behandling.getId())
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBistandsvilkårResultatHolder)
            .orElseThrow();
        var lagretVurdering = holder.getVurderinger().iterator().next();

        assertThat(lagretVurdering.getRegelsporing()).contains(regelsporing);
    }

    @Test
    void skal_beholde_regelsporing_når_perioden_deles() {
        var regelsporing = new Regelsporing("{\"a\":1}", "{\"b\":2}");
        repository.lagreBistandsVurderinger(behandling.getId(),
            List.of(new BistandsvilkårResultatPeriode(PERIODE_1, true, null, false, null, null, VURDERT_AV, VURDERT_TIDSPUNKT, regelsporing)));

        var førsteHalvdel = DatoIntervallEntitet.fraOgMedTilOgMed(PERIODE_1.getFomDato(), PERIODE_1.getFomDato().plusMonths(1).minusDays(1));
        var andreHalvdel = DatoIntervallEntitet.fraOgMedTilOgMed(PERIODE_1.getFomDato().plusMonths(1), PERIODE_1.getTomDato());
        repository.fjernResultatForTidslinjer(behandling.getId(), Map.of(
            VilkårType.BISTANDSVILKÅR, new LocalDateTimeline<>(andreHalvdel.getFomDato(), andreHalvdel.getTomDato(), true)));

        var holder = repository.hentEksisterendeGrunnlag(behandling.getId())
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBistandsvilkårResultatHolder)
            .orElseThrow();
        var gjenværendeVurdering = holder.getVurderinger().stream().filter(v -> v.getPeriode().equals(førsteHalvdel)).findFirst().orElseThrow();

        assertThat(gjenværendeVurdering.getRegelsporing()).contains(regelsporing);
    }

    @Test
    void skal_lagre_nytt_grunnlag_når_bare_regelsporingen_er_endret() {
        repository.lagreBistandsVurderinger(behandling.getId(),
            List.of(new BistandsvilkårResultatPeriode(PERIODE_1, true, null, false, null, null, VURDERT_AV, VURDERT_TIDSPUNKT, new Regelsporing("{\"a\":1}", "{\"b\":1}"))));
        repository.lagreBistandsVurderinger(behandling.getId(),
            List.of(new BistandsvilkårResultatPeriode(PERIODE_1, true, null, false, null, null, VURDERT_AV, VURDERT_TIDSPUNKT, new Regelsporing("{\"a\":2}", "{\"b\":2}"))));

        var holder = repository.hentEksisterendeGrunnlag(behandling.getId())
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBistandsvilkårResultatHolder)
            .orElseThrow();
        var lagretVurdering = holder.getVurderinger().iterator().next();

        assertThat(lagretVurdering.getRegelsporing()).contains(new Regelsporing("{\"a\":2}", "{\"b\":2}"));
    }

    @Test
    void livsoppholdtidslinjen_skal_kunne_gjenopprettes_som_nye_resultatperioder() {
        var regelsporing = new Regelsporing("{\"a\":1}", "{\"b\":2}");
        repository.lagreYtelseVurderinger(behandling.getId(),
            List.of(new AndreLivsoppholdsytelserResultatPeriode(PERIODE_1, false, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER, false, "avslått automatisk", null, null, VURDERT_AV, VURDERT_TIDSPUNKT, regelsporing)));

        var tidslinje = repository.hentEksisterendeGrunnlag(behandling.getId()).orElseThrow().hentLivsoppholdResultatPeriodeTidslinje();

        // Dette er gjenopprettingen InngangsvilkårVurderingTjeneste gjør når en avklaringsperiode krymper
        var gjenopprettet = tidslinje.segmenter().stream()
            .map(it -> new AndreLivsoppholdsytelserResultatPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()), it.getValue()))
            .toList();

        assertThat(gjenopprettet).hasSize(1);
        var vurdering = gjenopprettet.getFirst();
        assertThat(vurdering.getPeriode()).isEqualTo(PERIODE_1);
        assertThat(vurdering.isGodkjent()).isFalse();
        assertThat(vurdering.getIkkeOppfyltÅrsak()).isEqualTo(AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER);
        assertThat(vurdering.isManuellVurdering()).isFalse();
        assertThat(vurdering.getBegrunnelse()).isEqualTo("avslått automatisk");
        assertThat(vurdering.getVurdertAv()).isEqualTo(VURDERT_AV);
        assertThat(vurdering.getRegelsporing()).contains(regelsporing);
    }

    @Test
    void bistandstidslinjen_skal_kunne_gjenopprettes_som_nye_resultatperioder() {
        var regelsporing = new Regelsporing("{\"a\":1}", "{\"b\":2}");
        repository.lagreBistandsVurderinger(behandling.getId(),
            List.of(new BistandsvilkårResultatPeriode(PERIODE_1, false, BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID, false, "avslått automatisk", null, VURDERT_AV, VURDERT_TIDSPUNKT, regelsporing)));

        var tidslinje = repository.hentEksisterendeGrunnlag(behandling.getId()).orElseThrow().hentBistandResultatPeriodeTidslinje();

        var gjenopprettet = tidslinje.segmenter().stream()
            .map(it -> new BistandsvilkårResultatPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()), it.getValue()))
            .toList();

        assertThat(gjenopprettet).hasSize(1);
        var vurdering = gjenopprettet.getFirst();
        assertThat(vurdering.getPeriode()).isEqualTo(PERIODE_1);
        assertThat(vurdering.isGodkjent()).isFalse();
        assertThat(vurdering.getIkkeOppfyltÅrsak()).isEqualTo(BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID);
        assertThat(vurdering.erManuellVurdering()).isFalse();
        assertThat(vurdering.getBegrunnelse()).isEqualTo("avslått automatisk");
        assertThat(vurdering.getVurdertAv()).isEqualTo(VURDERT_AV);
        assertThat(vurdering.getRegelsporing()).contains(regelsporing);
    }

    @Test
    void automatisk_vurdering_skal_ikke_kunne_lagres_via_vilkårsvurderingsresultatet() {
        var automatiskVurdering = new GenereltVilkårsvurderingResultat(VilkårType.BISTANDSVILKÅR, false, BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID,
            false, "avslått automatisk", null, VURDERT_AV, VURDERT_TIDSPUNKT);

        assertThatThrownBy(() -> new BistandsvilkårResultatPeriode(PERIODE_1, automatiskVurdering, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("regelsporing");
    }

    @Test
    void skal_ikke_kopiere_naar_ingen_grunnlag_eksisterer() {
        var revurdering = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurdering, new BehandlingLås(null));
        repository.kopier(behandling.getId(), revurdering.getId());

        assertThat(repository.hentEksisterendeGrunnlag(revurdering.getId())).isEmpty();
    }
}
