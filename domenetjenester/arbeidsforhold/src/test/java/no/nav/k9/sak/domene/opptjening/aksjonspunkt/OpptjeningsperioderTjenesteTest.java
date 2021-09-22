package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.organisasjon.Organisasjonstype;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurderingOpptjeningsvilkår;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.Stillingsprosent;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class OpptjeningsperioderTjenesteTest {

    private static final String ORG_NUMMER = "974760673";
    private InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef();
    private AktørId AKTØRID = AktørId.dummy();
    private LocalDate skjæringstidspunkt = LocalDate.now();

    @Inject
    private EntityManager entityManager;

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private IAYRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private VirksomhetTjeneste virksomhetTjeneste = Mockito.mock(VirksomhetTjeneste.class);
    private FagsakRepository fagsakRepository;
    private OpptjeningRepository opptjeningRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider = Mockito.mock(OppgittOpptjeningFilterProvider.class);
    private OpptjeningsperioderTjeneste forSaksbehandlingTjeneste;
    private OpptjeningAktivitetVurderingOpptjeningsvilkår vurderForVilkår;

    @BeforeEach
    public void setUp() {
        repositoryProvider = new IAYRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        fagsakRepository = new FagsakRepository(entityManager);
        opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        vurderForVilkår = new OpptjeningAktivitetVurderingOpptjeningsvilkår();
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);

        forSaksbehandlingTjeneste = new OpptjeningsperioderTjeneste(repositoryProvider.getOpptjeningRepository(), oppgittOpptjeningFilterProvider);
        when(oppgittOpptjeningFilterProvider.finnOpptjeningFilter(Mockito.anyLong())).thenReturn(new OppgittOpptjeningFilter() {
        });

        Virksomhet virksomhet = new Virksomhet.Builder().medOrgnr(ORG_NUMMER)
            .medOppstart(LocalDate.now())
            .medOrganisasjonstype(Organisasjonstype.VIRKSOMHET)
            .medRegistrert(LocalDate.now())
            .build();
        when(virksomhetTjeneste.hentOrganisasjon(ORG_NUMMER)).thenReturn(virksomhet);
    }

    @Test
    void skal_returnere_en_periode_med_fiktivt_bekreftet_arbeidsforhold() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        LocalDate fraOgMed = LocalDate.of(2015, 1, 4);
        LocalDate tilOgMed = skjæringstidspunkt.plusMonths(2);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);
        var saksbehandlet = lagFiktivtArbeidsforholdSaksbehandlet(periode);
        var behandlingId = behandling.getId();
        iayTjeneste.lagreIayAggregat(behandlingId, saksbehandlet);
        ArbeidsforholdInformasjonBuilder informasjon = lagFiktivtArbeidsforholdOverstyring(fraOgMed, tilOgMed);
        iayTjeneste.lagreArbeidsforhold(behandlingId, AKTØRID, informasjon);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        // Act
        //List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, iayGrunnlag, opptjening, skjæringstidspunkt);
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.mapPerioderForSaksbehandling(ref, iayGrunnlag, vurderForVilkår, opptjening.getOpptjeningPeriode());

        // Assert
        assertThat(perioder.size()).isEqualTo(1);
    }

    @Test
    void skal_kunne_bygge_opptjeninsperiode_basert_på_arbeidsforhold_lagt_til_avsaksbehandler() {
        var behandling = opprettBehandling(skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        LocalDate start = LocalDate.now().minusMonths(5);

        var arbeidsforholdInformasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        var virksomhet = Arbeidsgiver.virksomhet("912471691");

        var arbeidsforholdOverstyringBuilder = arbeidsforholdInformasjonBuilder.getOverstyringBuilderFor(virksomhet, null);
        arbeidsforholdOverstyringBuilder.medHandling(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING);
        arbeidsforholdOverstyringBuilder.leggTilOverstyrtPeriode(start, LocalDate.MAX);
        arbeidsforholdOverstyringBuilder.medAngittStillingsprosent(Stillingsprosent.ZERO);
        arbeidsforholdInformasjonBuilder.leggTil(arbeidsforholdOverstyringBuilder);

        var behandlingId = behandling.getId();
        iayTjeneste.lagreArbeidsforhold(behandling.getId(), AKTØRID, arbeidsforholdInformasjonBuilder);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);

        // Act
        @SuppressWarnings("unused")
        // List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, iayGrunnlag, opptjening, skjæringstidspunkt);
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.mapPerioderForSaksbehandling(ref, iayGrunnlag, vurderForVilkår, opptjening.getOpptjeningPeriode());

    }

    private ArbeidsforholdInformasjonBuilder lagFiktivtArbeidsforholdOverstyring(LocalDate fraOgMed, LocalDate tilOgMed) {
        // TODO: Erstatte med hvordan de faktisk legges til i 5080
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        return ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty())
            .leggTil(ArbeidsforholdOverstyringBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medHandling(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER)
                .leggTilOverstyrtPeriode(fraOgMed, tilOgMed)
                .medAngittStillingsprosent(new Stillingsprosent(BigDecimal.valueOf(100))));
    }

    private InntektArbeidYtelseAggregatBuilder lagFiktivtArbeidsforholdSaksbehandlet(DatoIntervallEntitet periode) {
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.SAKSBEHANDLET);
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);
        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(null, OrgNummer.KUNSTIG_ORG, null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        var aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medPeriode(periode)
            .medProsentsats(BigDecimal.valueOf(100))
            .medBeskrivelse("Ser greit ut");
        var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(periode);
        yrkesaktivitetBuilder
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(null)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode);
        var aktørArbeid = aktørArbeidBuilder
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        builder.leggTilAktørArbeid(aktørArbeid);
        return builder;
    }


    private Behandling opprettBehandling(@SuppressWarnings("unused") LocalDate skjæringstidspunkt) {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, AKTØRID);
        @SuppressWarnings("unused")
        Long fagsakId = fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        var nyttResultat = Vilkårene.builder().build();

        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        vilkårResultatRepository.lagre(behandling.getId(), nyttResultat);
        return behandling;
    }

}
