package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;

import java.time.LocalDate;

public class AktivitetspengerEndringBarnetilleggScenarioer {

    /**
     * Fødselshendelse som gir barnetillegg. Bruker lav sats, får ett barn på fødselsdato.
     */
    public static AktivitetspengerTestScenario fødselMedEttBarn(LocalDate fom) {
        return fødselMedBarn(fom, 1);
    }

    /**
     * Fødselshendelse som gir barnetillegg for to barn.
     */
    public static AktivitetspengerTestScenario fødselMedToBarn(LocalDate fom) {
        return fødselMedBarn(fom, 2);
    }

    private static AktivitetspengerTestScenario fødselMedBarn(LocalDate fom, int antallBarn) {
        LocalDate fødselsdatoBarn = fom.plusDays(15);
        var tom = fom.plusWeeks(52).minusDays(1);

        return AktivitetspengerTestScenario.builder(fom)
            .medSatsGrunnlagTidslinjeLavMedBarn(fødselsdatoBarn, antallBarn)
            .medStandardBeregningsgrunnlag()
            .medTrigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, fødselsdatoBarn, tom)
            .build();
    }
}
