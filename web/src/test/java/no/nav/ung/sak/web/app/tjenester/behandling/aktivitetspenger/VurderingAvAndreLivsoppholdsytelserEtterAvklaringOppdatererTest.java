package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserAvklaringKildeType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AndreLivsoppholdsytelserResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderingAvVilkårPeriodeEtterAvklaringDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold.VurderingAvAndreLivsoppholdsytelserEtterAvklaringDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderingAvAndreLivsoppholdsytelserEtterAvklaringOppdatererTest {

    private static final String SAKSBEHANDLER = "A111111";

    private static final Periode PERIODE_1 = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

    @Inject
    private EntityManager entityManager;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;

    @Inject
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;

    @Inject
    private HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;

    @Inject
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;

    private VurderingAvAndreLivsoppholdsytelserEtterAvklaringOppdaterer oppdaterer;

    private Fagsak fagsak;

    @BeforeAll
    static void beforeAll() {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(SAKSBEHANDLER);
    }

    @AfterAll
    static void afterAll() {
        SubjectHandlerUtils.reset();
    }

    @BeforeEach
    void setUp() {
        var vurderingAvVilkårEtterAvklaringTjeneste = new VurderingAvVilkårEtterAvklaringTjeneste(vilkårResultatRepository);

        oppdaterer = new VurderingAvAndreLivsoppholdsytelserEtterAvklaringOppdaterer(
            vurderingAvVilkårEtterAvklaringTjeneste,
            vilkårsavklaringGrunnlagRepository,
            inngangsvilkårVurderingRepository,
            inngangsvilkårVurderingTjeneste,
            vilkårsvurderingHistorikkinnslagTjeneste);

        fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("11223344"), new Saksnummer("LIVSOPPH1"),
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        fagsakRepository.opprettNy(fagsak);
    }

    @Test
    void lagrer_vurdering_med_årsak_fra_avklaringen() {
        var behandling = opprettFørstegangsbehandling(PERIODE_1);
        lagreForeslåttAvklaring(behandling, PERIODE_1, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER);

        var dto = dto(vurdering(PERIODE_1, false, "mottar dagpenger", "fritekst til brev"));
        utførOppdatering(behandling, dto);

        var vurdering = hentLivsoppholdvurdering(behandling, PERIODE_1);
        assertThat(vurdering.isGodkjent()).isFalse();
        assertThat(vurdering.getIkkeOppfyltÅrsak()).isEqualTo(AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER);
        assertThat(vurdering.isManuellVurdering()).isTrue();
        assertThat(historikkinnslagRepository.hent(behandling.getId()))
            .extracting(Historikkinnslag::getSkjermlenke)
            .containsExactly(SkjermlenkeType.VURDER_ANDRE_LIVSOPPHOLDSYTELSER);
    }

    @Test
    void oppfylt_vurdering_overstyrer_årsaken_fra_avklaringen() {
        var behandling = opprettFørstegangsbehandling(PERIODE_1);
        lagreForeslåttAvklaring(behandling, PERIODE_1, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_UFØRETRYGD);

        var dto = dto(vurdering(PERIODE_1, true, "uttalelsen viser at ytelsen er stanset", null));
        utførOppdatering(behandling, dto);

        var vurdering = hentLivsoppholdvurdering(behandling, PERIODE_1);
        assertThat(vurdering.isGodkjent()).isTrue();
        assertThat(vurdering.getIkkeOppfyltÅrsak()).isNull();
    }

    @Test
    void krever_fritekst_til_brev_når_årsaken_krever_det() {
        var behandling = opprettFørstegangsbehandling(PERIODE_1);
        lagreForeslåttAvklaring(behandling, PERIODE_1, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ANNEN_YTELSE);

        var dto = dto(vurdering(PERIODE_1, false, "mottar annen ytelse", null));

        assertThatThrownBy(() -> utførOppdatering(behandling, dto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fritekstVurderingBrev");
    }

    private void utførOppdatering(Behandling behandling, VurderingAvAndreLivsoppholdsytelserEtterAvklaringDto dto) {
        var param = oppdaterParameter(behandling, dto);
        oppdaterer.oppdater(dto, param);
    }

    private AksjonspunktOppdaterParameter oppdaterParameter(Behandling behandling, VurderingAvAndreLivsoppholdsytelserEtterAvklaringDto dto) {
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandling.getId()));
        return new AksjonspunktOppdaterParameter(behandling, Optional.empty(), vilkårResultatBuilder, dto);
    }

    private AndreLivsoppholdsytelserResultatPeriode hentLivsoppholdvurdering(Behandling behandling, Periode periode) {
        return inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId())
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentAndreLivsoppholdsytelserResultatPerioder)
            .orElseThrow()
            .stream()
            .filter(v -> v.getPeriode().equals(tilDatoIntervallEntitet(periode)))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Fant ingen lagret vurdering for periode " + periode));
    }

    private Behandling opprettFørstegangsbehandling(Periode... vilkårsperioder) {
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var vilkårResultatBuilder = Vilkårene.builder();
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR);
        for (var periode : vilkårsperioder) {
            vilkårBuilder.leggTil(new VilkårPeriodeBuilder()
                .medPeriode(tilDatoIntervallEntitet(periode))
                .medUtfall(Utfall.IKKE_VURDERT));
        }
        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());
        return behandling;
    }

    private void lagreForeslåttAvklaring(Behandling behandling, Periode periode, AndreLivsoppholdsytelserIkkeOppfyltÅrsak årsak) {
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, Set.of(
            new VilkårPeriodeAvklaringForeslått(
                UUID.randomUUID(),
                DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()),
                årsak.getKode(),
                "begrunnelse for avklaring",
                true,
                årsak.kreverFritekst() ? "fritekst til varsel" : null,
                null,
                AndreLivsoppholdsytelserAvklaringKildeType.NAV,
                null,
                SAKSBEHANDLER,
                LocalDateTime.now(),
                Avklaringtype.OPPHØR)));
    }

    private static VurderingAvAndreLivsoppholdsytelserEtterAvklaringDto dto(VurderingAvVilkårPeriodeEtterAvklaringDto... perioder) {
        return new VurderingAvAndreLivsoppholdsytelserEtterAvklaringDto(List.of(perioder), "begrunnelse for aksjonspunktet");
    }

    private static VurderingAvVilkårPeriodeEtterAvklaringDto vurdering(Periode periode, boolean erVilkårOppfylt, String begrunnelse, String fritekstVurderingBrev) {
        return new VurderingAvVilkårPeriodeEtterAvklaringDto(new ÅpenPeriode(periode.getFom(), periode.getTom()), erVilkårOppfylt, begrunnelse, fritekstVurderingBrev);
    }

    private static DatoIntervallEntitet tilDatoIntervallEntitet(Periode periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }
}
