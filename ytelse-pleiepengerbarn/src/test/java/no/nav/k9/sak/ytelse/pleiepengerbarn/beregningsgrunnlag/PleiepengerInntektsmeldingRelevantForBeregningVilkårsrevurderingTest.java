package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

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
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.ReferanseType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.RelevantPeriodeUtleder;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Saksnummer;


@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class PleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurderingTest {


    public static final LocalDate STP = LocalDate.of(2022, 2, 1);
    @Inject
    private EntityManager entityManager;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private VilkårResultatRepository vilkårResultatRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private PleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering pleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering;
    private Fagsak fagsak;
    private Behandling behandling;
    private OpptjeningRepository opptjeningRepository;

    @BeforeEach
    void setUp() {
        var relevantPeriodeUtleder = new RelevantPeriodeUtleder(vilkårResultatRepository, fagsakRepository);
        var pleiepengerInntektsmeldingerRelevantForBeregning = new PleiepengerInntektsmeldingerRelevantForBeregning(relevantPeriodeUtleder);
        opptjeningRepository = new OpptjeningRepository(entityManager, behandlingRepository, vilkårResultatRepository);
        pleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering = new PleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering(
            new UnitTestLookupInstanceImpl(pleiepengerInntektsmeldingerRelevantForBeregning),
            iayTjeneste,
            opptjeningRepository,
            vilkårResultatRepository,
            true
        );

        fagsak = Fagsak.opprettNy(FagsakYtelseType.DAGPENGER, new AktørId(123L), new Saksnummer("987"), STP, STP.plusDays(10));
        fagsakRepository.opprettNy(fagsak);

        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

    }

    @Test
    void skal_ikke_filtrere_bort_dersom_godkjent_opptjening() {
        var stp = LocalDate.now();
        var orgnr = "000000000";
        DatoIntervallEntitet vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(stp, stp.plusDays(10));

        lagIAY(orgnr, stp);
        lagOpptjening(stp, orgnr, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);

        var inntektsmeldings = pleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering.begrensInntektsmeldinger(BehandlingReferanse.fra(behandling), List.of(lagInntektsmelding(orgnr, InternArbeidsforholdRef.nullRef(), stp)), vilkårsperiode);

        assertThat(inntektsmeldings.size()).isEqualTo(1);
    }


    @Test
    void skal_returnere_tom_liste_dersom_opptjening_ikke_er_vurdert_for_gitt_periode() {
        var stp = LocalDate.now();
        var stp2 = stp.plusDays(20);
        var orgnr = "000000000";
        DatoIntervallEntitet vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(stp, stp.plusDays(10));

        lagIAY(orgnr, stp);
        lagOpptjening(stp2, orgnr, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);

        var inntektsmeldings = pleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering.begrensInntektsmeldinger(BehandlingReferanse.fra(behandling), List.of(lagInntektsmelding(orgnr, InternArbeidsforholdRef.nullRef(), stp)), vilkårsperiode);

        assertThat(inntektsmeldings.size()).isEqualTo(0);
    }

    @Test
    void skal_filtrere_bort_IM_dersom_aktivitet_er_avslått_i_opptjening() {
        var stp = LocalDate.now();
        var orgnr = "000000000";
        DatoIntervallEntitet vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(stp, stp.plusDays(10));

        lagIAY(orgnr, stp);
        lagOpptjening(stp, orgnr, OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST);

        var inntektsmeldings = pleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering.begrensInntektsmeldinger(BehandlingReferanse.fra(behandling), List.of(lagInntektsmelding(orgnr, InternArbeidsforholdRef.nullRef(), stp)), vilkårsperiode);

        assertThat(inntektsmeldings.size()).isEqualTo(0);
    }
    @Test
    void skal_filtrere_bort_IM_dersom_en_annen_arbeidsaktivitet_er_godkjent_i_opptjening() {
        var stp = LocalDate.now();
        var orgnr = "000000000";
        var orgnr2 = "000000001";
        DatoIntervallEntitet vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(stp, stp.plusDays(10));

        lagIAY(List.of(orgnr, orgnr2), List.of(InternArbeidsforholdRef.nyRef(), InternArbeidsforholdRef.nyRef()), DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusYears(1), stp.plusDays(10)));
        lagOpptjening(stp, orgnr2, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);

        var inntektsmeldings = pleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering.begrensInntektsmeldinger(BehandlingReferanse.fra(behandling), List.of(lagInntektsmelding(orgnr, InternArbeidsforholdRef.nullRef(), stp)), vilkårsperiode);

        assertThat(inntektsmeldings.size()).isEqualTo(0);
    }

    @Test
    void skal_ikke_filtrere_bort_IM_dersom_minst_ett_godkjent_arbeidsforhold_for_IM_er_godkjent() {
        var stp = LocalDate.now();
        var orgnr = "000000000";
        DatoIntervallEntitet vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(stp, stp.plusDays(10));

        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();
        lagIAY(List.of(orgnr, orgnr), List.of(ref1, ref2), DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusYears(1), stp.plusDays(10)));
        lagOpptjening(stp, orgnr, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);

        var inntektsmeldings = pleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering.begrensInntektsmeldinger(BehandlingReferanse.fra(behandling), List.of(lagInntektsmelding(orgnr, InternArbeidsforholdRef.nullRef(), stp)), vilkårsperiode);

        assertThat(inntektsmeldings.size()).isEqualTo(1);
    }

    @Test
    void skal_ikke_filtrere_bort_IM_dersom_bruker_er_midlertidig_inaktiv_og_tilkommer_på_stp() {
        var stp = LocalDate.now();
        var orgnr = "000000000";
        DatoIntervallEntitet vilkårsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(stp, stp.plusDays(10));

        var ref1 = InternArbeidsforholdRef.nyRef();
        lagIAY(List.of(orgnr), List.of(ref1), DatoIntervallEntitet.fraOgMedTilOgMed(stp, stp.plusDays(10)));
        lagOpptjeningMidlertidigInaktiv(stp, orgnr, OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT);

        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET);
        var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(vilkårsperiode);
        vilkårPeriodeBuilder.medUtfall(Utfall.OPPFYLT);
        vilkårPeriodeBuilder.medMerknad(VilkårUtfallMerknad.VM_7847_B);
        vilkårBuilder.leggTil(vilkårPeriodeBuilder);
        builder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), builder.build());

        var inntektsmeldings = pleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering.begrensInntektsmeldinger(BehandlingReferanse.fra(behandling), List.of(lagInntektsmelding(orgnr, InternArbeidsforholdRef.nullRef(), stp)), vilkårsperiode);

        assertThat(inntektsmeldings.size()).isEqualTo(1);
    }

    private void lagOpptjening(LocalDate stp, String orgnr, OpptjeningAktivitetKlassifisering opptjeningAktivitetKlassifisering) {
        var opptjeningFom = stp.minusDays(29);
        var opptjeningTom = stp.minusDays(1);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, opptjeningFom, opptjeningTom, true);
        opptjeningRepository.lagreOpptjeningResultat(behandling, stp, Period.of(0, 0, 28),
            List.of(new OpptjeningAktivitet(opptjeningFom, opptjeningTom, OpptjeningAktivitetType.ARBEID, opptjeningAktivitetKlassifisering, orgnr, ReferanseType.ORG_NR)));
    }

    private void lagOpptjeningMidlertidigInaktiv(LocalDate stp, String orgnr, OpptjeningAktivitetKlassifisering opptjeningAktivitetKlassifisering) {
        var opptjeningFom = stp.minusDays(29);
        var opptjeningTom = stp.minusDays(1);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, opptjeningFom, opptjeningTom, true);
        opptjeningRepository.lagreOpptjeningResultat(behandling, stp, Period.of(0, 0, 0),
            List.of(new OpptjeningAktivitet(stp, stp.plusDays(10), OpptjeningAktivitetType.ARBEID, opptjeningAktivitetKlassifisering, orgnr, ReferanseType.ORG_NR)));
    }

    private void lagIAY(String orgnr, LocalDate stp) {
        lagIAY(List.of(orgnr), List.of(InternArbeidsforholdRef.nyRef()), DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusYears(1), stp.plusDays(10)));
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

        iayTjeneste.lagreIayAggregat(behandling.getId(), InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(aktørArbeidBuilder));
    }



    private void lagIAY(List<String> orgnr, List<InternArbeidsforholdRef> arbeidsforholdrefs, List<DatoIntervallEntitet> periode) {
        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty());
        aktørArbeidBuilder.medAktørId(behandling.getAktørId());
        var index = 0;
        for (String it : orgnr) {
            aktørArbeidBuilder.leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet(it))
                .medArbeidsforholdId(arbeidsforholdrefs.get(index))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                    .medPeriode(periode.get(index))));
            index++;
        }

        iayTjeneste.lagreIayAggregat(behandling.getId(), InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(aktørArbeidBuilder));
    }


    private static Inntektsmelding lagInntektsmelding(String orgnr, InternArbeidsforholdRef arbeidsforholdId, LocalDate startdato) {
        return InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medArbeidsforholdId(arbeidsforholdId)
            .medBeløp(BigDecimal.TEN)
            .medStartDatoPermisjon(startdato)
            .medKanalreferanse("KANALREFERANSE")
            .build();
    }

}
