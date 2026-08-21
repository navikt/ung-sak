package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.maksdato;

import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.EtterlysningOgGrunnlag;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.EtterlysningStatusOgType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EtterlysningForOpphørVedMaksdatoResultatUtlederTest {

    // Maksdato innenfor varslingsvindut (< 3 uker frem) — harPassertVarseldato() returnerer true
    private static final LocalDate MAKSDATO_INNENFOR_VARSLINGSVINDU = LocalDate.now().plusWeeks(2);
    // Maksdato utenfor varslingsvindut (> 3 uker frem) — harPassertVarseldato() returnerer false
    private static final LocalDate MAKSDATO_UTENFOR_VARSLINGSVINDU = LocalDate.now().plusWeeks(4);
    // TOM-dato lik maksdato — opphør ved maksdato
    private static final LocalDate TOM_VED_MAKSDATO = MAKSDATO_INNENFOR_VARSLINGSVINDU;
    // TOM-dato BEFORE maksdato — opphør skjer FØR maksdato, håndteres via vanlig opphørsvarsling
    private static final LocalDate TOM_FØR_MAKSDATO = MAKSDATO_INNENFOR_VARSLINGSVINDU.minusDays(1);

    @Test
    void skalOppretteEtterlysning_nårIngenEksisterende_ogMaksdatoInnenforVarslingsvindu() {
        var input = new EtterlysningForOpphørVedMaksdatoResultatUtleder.EtterlysningForMaksdatoInput(
            TOM_VED_MAKSDATO, MAKSDATO_INNENFOR_VARSLINGSVINDU, LocalDate.now(), null);

        var resultat = EtterlysningForOpphørVedMaksdatoResultatUtleder.utledResultat(input);

        assertThat(resultat).isEqualTo(EtterlysningForOpphørVedMaksdatoResultatUtleder.ResultatType.OPPRETT_ETTERLYSNING);
    }

    @Test
    void skalIkkeOppretteEtterlysning_nårMaksdatoUtenforVarslingsvindu_ogIngenEksisterende() {
        var input = new EtterlysningForOpphørVedMaksdatoResultatUtleder.EtterlysningForMaksdatoInput(
            MAKSDATO_UTENFOR_VARSLINGSVINDU, MAKSDATO_UTENFOR_VARSLINGSVINDU, LocalDate.now(), null);

        var resultat = EtterlysningForOpphørVedMaksdatoResultatUtleder.utledResultat(input);

        assertThat(resultat).isEqualTo(EtterlysningForOpphørVedMaksdatoResultatUtleder.ResultatType.INGEN_ENDRING);
    }

    @Test
    void skalIngenEndring_nårTomDatoFørMaksdato_ogIngenEksisterende() {
        var input = new EtterlysningForOpphørVedMaksdatoResultatUtleder.EtterlysningForMaksdatoInput(
            TOM_FØR_MAKSDATO, MAKSDATO_INNENFOR_VARSLINGSVINDU, LocalDate.now(), null);

        var resultat = EtterlysningForOpphørVedMaksdatoResultatUtleder.utledResultat(input);

        assertThat(resultat).isEqualTo(EtterlysningForOpphørVedMaksdatoResultatUtleder.ResultatType.INGEN_ENDRING);
    }

    @Test
    void skalAvbryteEtterlysning_nårTomDatoFørMaksdato_ogEksisterendeVenter() {
        var eksisterende = lagEtterlysningOgGrunnlag(EtterlysningStatus.VENTER, MAKSDATO_INNENFOR_VARSLINGSVINDU);
        var input = new EtterlysningForOpphørVedMaksdatoResultatUtleder.EtterlysningForMaksdatoInput(
            TOM_FØR_MAKSDATO, MAKSDATO_INNENFOR_VARSLINGSVINDU, LocalDate.now(), eksisterende);

        var resultat = EtterlysningForOpphørVedMaksdatoResultatUtleder.utledResultat(input);

        assertThat(resultat).isEqualTo(EtterlysningForOpphørVedMaksdatoResultatUtleder.ResultatType.AVBRYT_ETTERLYSNING);
    }

    @Test
    void skalIngenEndring_nårTomDatoFørMaksdato_ogEksisterendeIkkeVenter() {
        var eksisterende = lagEtterlysningOgGrunnlag(EtterlysningStatus.MOTTATT_SVAR, MAKSDATO_INNENFOR_VARSLINGSVINDU);
        var input = new EtterlysningForOpphørVedMaksdatoResultatUtleder.EtterlysningForMaksdatoInput(
            TOM_FØR_MAKSDATO, MAKSDATO_INNENFOR_VARSLINGSVINDU, LocalDate.now(), eksisterende);

        var resultat = EtterlysningForOpphørVedMaksdatoResultatUtleder.utledResultat(input);

        assertThat(resultat).isEqualTo(EtterlysningForOpphørVedMaksdatoResultatUtleder.ResultatType.INGEN_ENDRING);
    }

    @Test
    void skalAvbryteEtterlysning_nårMaksdatoUtenforVarslingsvindu_ogEksisterendeVenter() {
        var eksisterende = lagEtterlysningOgGrunnlag(EtterlysningStatus.VENTER, MAKSDATO_UTENFOR_VARSLINGSVINDU);
        var input = new EtterlysningForOpphørVedMaksdatoResultatUtleder.EtterlysningForMaksdatoInput(
            MAKSDATO_UTENFOR_VARSLINGSVINDU, MAKSDATO_UTENFOR_VARSLINGSVINDU, LocalDate.now(), eksisterende);

        var resultat = EtterlysningForOpphørVedMaksdatoResultatUtleder.utledResultat(input);

        assertThat(resultat).isEqualTo(EtterlysningForOpphørVedMaksdatoResultatUtleder.ResultatType.AVBRYT_ETTERLYSNING);
    }

    @Test
    void skalIngenEndring_nårEksisterendeVenter_ogSammeMaksdato() {
        var eksisterende = lagEtterlysningOgGrunnlag(EtterlysningStatus.VENTER, MAKSDATO_INNENFOR_VARSLINGSVINDU);
        var input = new EtterlysningForOpphørVedMaksdatoResultatUtleder.EtterlysningForMaksdatoInput(
            TOM_VED_MAKSDATO, MAKSDATO_INNENFOR_VARSLINGSVINDU, LocalDate.now(), eksisterende);

        var resultat = EtterlysningForOpphørVedMaksdatoResultatUtleder.utledResultat(input);

        assertThat(resultat).isEqualTo(EtterlysningForOpphørVedMaksdatoResultatUtleder.ResultatType.INGEN_ENDRING);
    }

    @Test
    void skalErstatteEksisterende_nårEksisterendeVenter_ogUlikMaksdato() {
        var gammeltMaksdato = MAKSDATO_INNENFOR_VARSLINGSVINDU.minusWeeks(1);
        var eksisterende = lagEtterlysningOgGrunnlag(EtterlysningStatus.VENTER, gammeltMaksdato);
        var input = new EtterlysningForOpphørVedMaksdatoResultatUtleder.EtterlysningForMaksdatoInput(
            TOM_VED_MAKSDATO, MAKSDATO_INNENFOR_VARSLINGSVINDU, LocalDate.now(), eksisterende);

        var resultat = EtterlysningForOpphørVedMaksdatoResultatUtleder.utledResultat(input);

        assertThat(resultat).isEqualTo(EtterlysningForOpphørVedMaksdatoResultatUtleder.ResultatType.ERSTATT_EKSISTERENDE);
    }

    @Test
    void skalErstatteEksisterende_nårEksisterendeOpprettet_ogUlikMaksdato() {
        var gammeltMaksdato = MAKSDATO_INNENFOR_VARSLINGSVINDU.minusWeeks(1);
        var eksisterende = lagEtterlysningOgGrunnlag(EtterlysningStatus.OPPRETTET, gammeltMaksdato);
        var input = new EtterlysningForOpphørVedMaksdatoResultatUtleder.EtterlysningForMaksdatoInput(
            TOM_VED_MAKSDATO, MAKSDATO_INNENFOR_VARSLINGSVINDU, LocalDate.now(), eksisterende);

        var resultat = EtterlysningForOpphørVedMaksdatoResultatUtleder.utledResultat(input);

        assertThat(resultat).isEqualTo(EtterlysningForOpphørVedMaksdatoResultatUtleder.ResultatType.ERSTATT_EKSISTERENDE);
    }

    @Test
    void skalOppretteNy_nårEksisterendeMottattSvar_ogUlikMaksdato() {
        var gammeltMaksdato = MAKSDATO_INNENFOR_VARSLINGSVINDU.minusWeeks(1);
        var eksisterende = lagEtterlysningOgGrunnlag(EtterlysningStatus.MOTTATT_SVAR, gammeltMaksdato);
        var input = new EtterlysningForOpphørVedMaksdatoResultatUtleder.EtterlysningForMaksdatoInput(
            TOM_VED_MAKSDATO, MAKSDATO_INNENFOR_VARSLINGSVINDU, LocalDate.now(), eksisterende);

        var resultat = EtterlysningForOpphørVedMaksdatoResultatUtleder.utledResultat(input);

        assertThat(resultat).isEqualTo(EtterlysningForOpphørVedMaksdatoResultatUtleder.ResultatType.OPPRETT_ETTERLYSNING);
    }

    @Test
    void skalOppretteNy_nårEksisterendeUtløpt_ogUlikMaksdato() {
        var gammeltMaksdato = MAKSDATO_INNENFOR_VARSLINGSVINDU.minusWeeks(1);
        var eksisterende = lagEtterlysningOgGrunnlag(EtterlysningStatus.UTLØPT, gammeltMaksdato);
        var input = new EtterlysningForOpphørVedMaksdatoResultatUtleder.EtterlysningForMaksdatoInput(
            TOM_VED_MAKSDATO, MAKSDATO_INNENFOR_VARSLINGSVINDU, LocalDate.now(), eksisterende);

        var resultat = EtterlysningForOpphørVedMaksdatoResultatUtleder.utledResultat(input);

        assertThat(resultat).isEqualTo(EtterlysningForOpphørVedMaksdatoResultatUtleder.ResultatType.OPPRETT_ETTERLYSNING);
    }

    private static EtterlysningOgGrunnlag lagEtterlysningOgGrunnlag(EtterlysningStatus status, LocalDate maksdato) {
        var grunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag.getPeriodeMaksDato()).thenReturn(Optional.of(maksdato));
        return new EtterlysningOgGrunnlag(
            new EtterlysningStatusOgType(status, EtterlysningType.UTTALELSE_OPPHOR_VED_MAKSDATO),
            grunnlag
        );
    }
}
