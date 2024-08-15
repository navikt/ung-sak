package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.FiltrerInntektsmeldingForBeregningInputOverstyring;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.ReferanseType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.RelevantPeriodeUtleder;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput.BeregningInputHistorikkTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput.BeregningInputLagreTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.PSBOpptjeningForBeregningTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.PleiepengerInntektsmeldingerRelevantForBeregning;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class ForvaltningOverstyrInputBeregningTest {


    private static final LocalDate STP = LocalDate.now().minusDays(10);
    public static final String ORG_NUMMER = "972674818";

    @Inject
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    @Inject
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    @Inject
    private BehandlingModellRepository behandlingModellRepository;
    @Inject
    private EntityManager entityManager;
    @Inject
    private VilkårResultatRepository vilkårResultatRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private @FagsakYtelseTypeRef(value = FagsakYtelseType.PLEIEPENGER_SYKT_BARN) PSBOpptjeningForBeregningTjeneste psBOpptjeningForBeregningTjeneste;


    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private OpptjeningRepository opptjeningRepository;
    private final VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);

    private AbakusInMemoryInntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    private ForvaltningOverstyrInputBeregning forvaltningOverstyrInputBeregning;
    private Fagsak fagsak;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        beregningPerioderGrunnlagRepository = new BeregningPerioderGrunnlagRepository(entityManager, vilkårResultatRepository);
        opptjeningRepository = new OpptjeningRepository(entityManager, behandlingRepository, vilkårResultatRepository);
        inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();


        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(Virksomhet.getBuilder().medNavn("Virksomheten").medOrgnr(ORG_NUMMER).medAvsluttet(LocalDate.now().minusDays(1)).build());
        ArbeidsgiverTjeneste arbeidsgiverTjeneste = new ArbeidsgiverTjeneste(null, virksomhetTjeneste);
        var arbeidsgiverHistorikkinnslagTjeneste = new ArbeidsgiverHistorikkinnslag(arbeidsgiverTjeneste);

        var pleiepengerInntektsmeldingerRelevantForBeregning = new PleiepengerInntektsmeldingerRelevantForBeregning(new RelevantPeriodeUtleder(vilkårResultatRepository, fagsakRepository));
        var inntektsmeldingerRelevantForBeregning = new UnitTestLookupInstanceImpl<InntektsmeldingerRelevantForBeregning>(pleiepengerInntektsmeldingerRelevantForBeregning);
        var filtrerInntektsmeldinger = new FiltrerInntektsmeldingForBeregningInputOverstyring(inntektsmeldingerRelevantForBeregning, inntektArbeidYtelseTjeneste);
        forvaltningOverstyrInputBeregning = new ForvaltningOverstyrInputBeregning(
            historikkTjenesteAdapter,
            new BeregningInputHistorikkTjeneste(beregningPerioderGrunnlagRepository, arbeidsgiverHistorikkinnslagTjeneste, historikkTjenesteAdapter),
            new BeregningInputLagreTjeneste(beregningPerioderGrunnlagRepository,
                new UnitTestLookupInstanceImpl<>(psBOpptjeningForBeregningTjeneste),
                filtrerInntektsmeldinger),
            fagsakProsessTaskRepository,
            behandlingModellRepository,
            beregningPerioderGrunnlagRepository,
            inntektsmeldingerRelevantForBeregning,
            inntektArbeidYtelseTjeneste,
            virksomhetTjeneste
        );

        fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, new AktørId(123L), new Saksnummer("987"), STP, STP.plusDays(10));
        fagsakRepository.opprettNy(fagsak);

        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

    }

    @Test
    void skal_kunne_overstyre_refusjon_opphør() {
        var vilkårsperiode = DatoIntervallEntitet.fra(STP, STP.plusDays(10));

        initVilkår(vilkårsperiode);
        var opphørRefusjon = STP.plusDays(2);
        lagIAY(ORG_NUMMER, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusYears(1), opphørRefusjon));
        lagOpptjening(STP, ORG_NUMMER, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        lagreInntektsmelding(ORG_NUMMER);


        overstyrOpphørsdatoRefusjon(vilkårsperiode, opphørRefusjon);


        var beregningsgrunnlagPerioderGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(behandling.getId());
        var inputOverstyring = beregningsgrunnlagPerioderGrunnlag
            .stream()
            .flatMap(gr -> gr.getInputOverstyringPerioder().stream())
            .findFirst()
            .orElseThrow();

        assertThat(inputOverstyring.getSkjæringstidspunkt()).isEqualTo(STP);
        assertThat(inputOverstyring.getAktivitetOverstyringer().size()).isEqualTo(1);
        assertThat(inputOverstyring.getAktivitetOverstyringer().getFirst().getInntektPrÅr()).isNull();
        assertThat(inputOverstyring.getAktivitetOverstyringer().getFirst().getArbeidsgiver().getIdentifikator()).isEqualTo(ORG_NUMMER);
        assertThat(inputOverstyring.getAktivitetOverstyringer().getFirst().getOpphørRefusjon()).isEqualTo(opphørRefusjon);
    }

    @Test
    void skal_ikke_kunne_overstyre_refusjon_opphør_dersom_virksomheten_ikke_er_avsluttet() {
        var vilkårsperiode = DatoIntervallEntitet.fra(STP, STP.plusDays(10));

        initVilkår(vilkårsperiode);
        var opphørRefusjon = STP.plusDays(2);
        lagIAY(ORG_NUMMER, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusYears(1), LocalDate.now().minusDays(1)));
        lagOpptjening(STP, ORG_NUMMER, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);
        lagreInntektsmelding(ORG_NUMMER);
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(Virksomhet.getBuilder().medNavn("Virksomheten").medOrgnr(ORG_NUMMER).build());

        var exception = assertThrows(IllegalArgumentException.class,
            () -> overstyrOpphørsdatoRefusjon(vilkårsperiode, opphørRefusjon));

        assertThat(exception.getMessage()).isEqualTo("Kan ikke opphøre refusjon for en virksomhet som ikke er avsluttet.");
    }

    @Test
    void skal_ikke_kunne_overstyre_refusjon_opphør_uten_inntektsmelding() {
        var vilkårsperiode = DatoIntervallEntitet.fra(STP, STP.plusDays(10));

        initVilkår(vilkårsperiode);
        var opphørRefusjon = STP.plusDays(2);
        lagIAY(ORG_NUMMER, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusYears(1), opphørRefusjon.minusDays(1)));
        lagOpptjening(STP, ORG_NUMMER, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);


        var exception = assertThrows(IllegalArgumentException.class,
            () -> overstyrOpphørsdatoRefusjon(vilkårsperiode, opphørRefusjon));

        assertThat(exception.getMessage()).isEqualTo("Overstyrt arbeidsgiver hadde ikke inntektsmelding til bruk for periode " + vilkårsperiode);
    }

    private void overstyrOpphørsdatoRefusjon(DatoIntervallEntitet vilkårsperiode, LocalDate opphørRefusjon) {
        forvaltningOverstyrInputBeregning.overstyrOpphørRefusjon(
            behandling,
            vilkårsperiode,
            Arbeidsgiver.virksomhet(ORG_NUMMER),
            opphørRefusjon,
            "begrunnelse"
        );
    }

    private void lagreInntektsmelding(String orgNummer) {
        inntektArbeidYtelseTjeneste.lagreInntektsmeldinger(fagsak.getSaksnummer(), behandling.getId(), List.of(
            InntektsmeldingBuilder.builder()
                .medYtelse(FagsakYtelseType.PSB)
                .medStartDatoPermisjon(STP)
                .medBeløp(BigDecimal.ONE)
                .medKanalreferanse("ref")
                .medJournalpostId("id")
                .medArbeidsgiver(Arbeidsgiver.virksomhet(orgNummer))));
    }

    private void initVilkår(DatoIntervallEntitet vilkårsperiode) {
        var opptjening = new VilkårBuilder(VilkårType.OPPTJENINGSPERIODEVILKÅR)
            .leggTil(new VilkårPeriodeBuilder().medPeriode(vilkårsperiode).medUtfall(Utfall.OPPFYLT));
        var beregning = new VilkårBuilder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .leggTil(new VilkårPeriodeBuilder().medPeriode(vilkårsperiode).medUtfall(Utfall.IKKE_VURDERT));
        Vilkårene nyttResultat = Vilkårene.builder()
            .leggTil(opptjening)
            .leggTil(beregning)
            .build();

        vilkårResultatRepository.lagre(behandling.getId(), nyttResultat);
    }


    private void lagOpptjening(LocalDate stp, String orgnr, OpptjeningAktivitetKlassifisering opptjeningAktivitetKlassifisering) {
        var opptjeningFom = stp.minusDays(29);
        var opptjeningTom = stp.minusDays(1);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, opptjeningFom, opptjeningTom, true);
        opptjeningRepository.lagreOpptjeningResultat(behandling, stp, Period.of(0, 0, 28),
            List.of(new OpptjeningAktivitet(opptjeningFom, opptjeningTom, OpptjeningAktivitetType.ARBEID, opptjeningAktivitetKlassifisering, orgnr, ReferanseType.ORG_NR)));
    }

    private void lagIAY(String orgnr, DatoIntervallEntitet periode) {
        lagIAY(List.of(orgnr), List.of(InternArbeidsforholdRef.nyRef()), periode);
    }

    private void lagIAY(List<String> orgnr, List<InternArbeidsforholdRef> arbeidsforholdrefs, DatoIntervallEntitet periode) {
        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty());
        aktørArbeidBuilder.medAktørId(behandling.getAktørId());
        var index = 0;
        for (String it : orgnr) {
            aktørArbeidBuilder.leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet(it))
                .medArbeidsforholdId(arbeidsforholdrefs.get(index))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(periode)));
            index++;
        }

        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(aktørArbeidBuilder));
    }


}
