package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import java.util.List;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.Oppfylt;
import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.SequenceSpecification;
import no.nav.fpsak.nare.specification.Specification;

/**
 * Regeltjeneste for vurdering av OpptjeningsVilkåret tilpasset Svangerskapspenger
 * <p>
 * Dette vurderes som følger:
 *
 * Perioden i arbeidsm må være på eller mer 28 dager, får antatt godkjent i perioden siden innteket ikke har tilkommet på tidspunktet.
 *
 * <p>
 * Aktiviteter som inngår er:
 * <ul>
 * <li>Arbeid - registrert arbeidsforhold i AA-registeret</li>
 * <li>Næring - Registrert i Enhetsregisteret som selvstendig næringsdrivende</li>
 * <li>Ytelser - Dagpenger, Foreldrepenger, Sykepenger, Svangerskapspenger, Opplæringspenger,
 * Omsorgspenger og Pleiepenger</li>
 * <li>Pensjonsgivende inntekt som likestilles med yrkesaktivitet = Lønn fra arbeidsgiver i fbm videre- og
 * etterutdanning, Ventelønn, Vartpenger, Etterlønn/sluttvederlag fra arbeidsgiver, Avtjening av militær- eller
 * siviltjeneste eller obligatorisk sivilforsvarstjeneste.</li>
 * </ul>
 */
@RuleDocumentation(value = Opptjeningsvilkår.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=174836170", legalReference = "FP_VK 23 § 14-6")
public class Opptjeningsvilkår implements RuleService<Opptjeningsgrunnlag> {

    public static final String ID = "FP_VK_23";

    /** Konstant for Aareg arbeid. */
    public static final String ARBEID = "ARBEID";

    public static final String UTLAND = "UTENLANDSK_ARBEIDSFORHOLD";

    /**
     * Evaluation Property: Antatt opptjening uttrykt som en Period (ISO 8601). eks. P4M22D = 4 måneder + 22 dager. Settes dersom Antatt
     * opptjening ikke er nok.
     */
    public static final String EVAL_RESULT_ANTATT_AKTIVITET_TIDSLINJE = "antattOpptjeningAktivitetTidslinje";

    /** Evaluation Property: Antatt godkjente perioder med arbeid. */
    public static final String EVAL_RESULT_ANTATT_GODKJENT = "antattGodkjentArbeid";

    /** Evaluation property: Frist for innsending av opptjeningopplysninger (eks. Inntekt). */
    public static final String EVAL_RESULT_FRIST_FOR_OPPTJENING_OPPLYSNINGER = "fristInnsendingOpptjeningopplysninger";

    /**
     * Evaluation Property: Perioder underkjennes dersom de ikke aksepteres av
     * {@link SjekkInntektSamsvarerMedArbeidAktivitet}.
     */
    public static final String EVAL_RESULT_UNDERKJENTE_PERIODER = "underkjentePerioder";

    /** Evaluation Property: Bekreftet opptjening aktivitet tidslinje bestemmes av {@link BeregnOpptjening}. */
    public static final String EVAL_RESULT_BEKREFTET_AKTIVITET_TIDSLINJE = "bekreftetOpptjeningAktivitetTidslinje";

    /** Evaluation Property: Bekreftet opptjening uttrykt som en Period (ISO 8601). eks. P4M22D = 4 måneder + 22 dager. */
    public static final String EVAL_RESULT_BEKREFTET_OPPTJENING = "bekreftetOpptjening";

    @Override
    public Evaluation evaluer(Opptjeningsgrunnlag grunnlag, Object output) {
        MellomregningOpptjeningsvilkårData grunnlagOgMellomregning = new MellomregningOpptjeningsvilkårData(grunnlag);
        Evaluation evaluation = getSpecification().evaluate(grunnlagOgMellomregning);

        // kopier ut resultater og sett resultater
        grunnlagOgMellomregning.oppdaterOutputResultat((OpptjeningsvilkårResultat) output);

        return evaluation;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<MellomregningOpptjeningsvilkårData> getSpecification() {
        Ruleset<MellomregningOpptjeningsvilkårData> rs = new Ruleset<>();

        Specification<MellomregningOpptjeningsvilkårData> sjekkOpptjeningsvilkåret = rs.hvisRegel("FP_VK 23.2", "Hvis tilstrekkelig opptjening")
            .hvis(new SjekkTilstrekkeligOpptjening(), new Oppfylt())
            .ellers(new SjekkTilstrekkeligOpptjeningInklAntatt());

        return new SequenceSpecification<>("FP_VK 23.1",
            "Sammenstill Arbeid aktivitet med Inntekt",
            List.of(new SjekkInntektSamsvarerMedArbeidAktivitet(), new BeregnOpptjening(), sjekkOpptjeningsvilkåret));
    }
}
