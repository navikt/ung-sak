package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BistandsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.VurderingAvBistandsvilkårEtterAvklaringDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderingAvVilkårPeriodeEtterAvklaringDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
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

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderingAvBistandsvilkårEtterAvklaringOppdatererTest {

    private static final String SAKSBEHANDLER = "A111111";

    private static final Periode PERIODE_1 = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private VurderingAvBistandsvilkårEtterAvklaringOppdaterer oppdaterer;

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
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        fagsakRepository = repositoryProvider.getFagsakRepository();
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        vilkårsavklaringGrunnlagRepository = new VilkårsavklaringGrunnlagRepository(entityManager);
        var inngangsvilkårVurderingTjeneste = new InngangsvilkårVurderingTjeneste(inngangsvilkårVurderingRepository, behandlingRepository, vilkårResultatRepository);
        var vurderingAvVilkårEtterAvklaringTjeneste = new VurderingAvVilkårEtterAvklaringTjeneste(vilkårResultatRepository);
        historikkinnslagRepository = new HistorikkinnslagRepository(entityManager);

        oppdaterer = new VurderingAvBistandsvilkårEtterAvklaringOppdaterer(
            vurderingAvVilkårEtterAvklaringTjeneste,
            vilkårsavklaringGrunnlagRepository,
            inngangsvilkårVurderingRepository,
            inngangsvilkårVurderingTjeneste,
            behandlingRepository,
            historikkinnslagRepository);

        fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("11223344"), new Saksnummer("BISTANDOPP1"),
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        fagsakRepository.opprettNy(fagsak);
    }

    @Test
    void lagrer_vurdering_med_årsak_fra_avklaringen() {
        var behandling = opprettFørstegangsbehandling(PERIODE_1);
        lagreForeslåttAvklaring(behandling, PERIODE_1, BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK);

        var dto = dto(vurdering(PERIODE_1, false, "mangler 14a-vedtak", "fritekst til brev"));
        utførOppdatering(behandling, dto);

        var vurdering = hentBistandvurdering(behandling, PERIODE_1);
        assertThat(vurdering.isGodkjent()).isFalse();
        assertThat(vurdering.getIkkeOppfyltÅrsak()).isEqualTo(BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK);
        assertThat(historikkinnslagRepository.hent(behandling.getId()))
            .extracting(Historikkinnslag::getSkjermlenke)
            .containsExactly(SkjermlenkeType.BISTANDSVILKÅR);
    }

    private void utførOppdatering(Behandling behandling, VurderingAvBistandsvilkårEtterAvklaringDto dto) {
        var param = oppdaterParameter(behandling, dto);
        oppdaterer.oppdater(dto, param);
    }

    private AksjonspunktOppdaterParameter oppdaterParameter(Behandling behandling, VurderingAvBistandsvilkårEtterAvklaringDto dto) {
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandling.getId()));
        return new AksjonspunktOppdaterParameter(behandling, Optional.empty(), vilkårResultatBuilder, dto);
    }

    private BistandsvilkårResultatPeriode hentBistandvurdering(Behandling behandling, Periode periode) {
        return inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId())
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBistandsvilkårResultatPerioder)
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
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.BISTANDSVILKÅR);
        for (var periode : vilkårsperioder) {
            vilkårBuilder.leggTil(new VilkårPeriodeBuilder()
                .medPeriode(tilDatoIntervallEntitet(periode))
                .medUtfall(Utfall.IKKE_VURDERT));
        }
        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());
        return behandling;
    }

    private void lagreForeslåttAvklaring(Behandling behandling, Periode periode, BistandsvilkårIkkeOppfyltÅrsak årsak) {
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BISTANDSVILKÅR, Set.of(
            new VilkårPeriodeAvklaringForeslått(
                UUID.randomUUID(),
                DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()),
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

    private static VurderingAvBistandsvilkårEtterAvklaringDto dto(VurderingAvVilkårPeriodeEtterAvklaringDto... perioder) {
        return new VurderingAvBistandsvilkårEtterAvklaringDto(List.of(perioder), "begrunnelse for aksjonspunktet");
    }

    private static VurderingAvVilkårPeriodeEtterAvklaringDto vurdering(Periode periode, boolean erVilkårOppfylt, String begrunnelse, String fritekstVurderingBrev) {
        return new VurderingAvVilkårPeriodeEtterAvklaringDto(new ÅpenPeriode(periode.getFom(), periode.getTom()), erVilkårOppfylt, begrunnelse, fritekstVurderingBrev);
    }

    private static DatoIntervallEntitet tilDatoIntervallEntitet(Periode periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }
}
