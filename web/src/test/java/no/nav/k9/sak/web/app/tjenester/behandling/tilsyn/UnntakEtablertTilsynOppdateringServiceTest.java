package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt.VurderingDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynBeskrivelse;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynForPleietrengende;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynPeriode;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class UnntakEtablertTilsynOppdateringServiceTest {

    @Inject
    private UnntakEtablertTilsynOppdateringService service;

    @Inject
    private UnntakEtablertTilsynGrunnlagRepository repo;

    private static final DatoIntervallEntitet PERIODE1 =  DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.parse("2021-01-01"), LocalDate.parse("2021-01-31"));
    private static final Long BEHANDLING1 = 123L;
    private static final AktørId søker = AktørId.dummy();
    private static final AktørId pleietrengende = AktørId.dummy();
    private static final String BESKRIVELSE1 = "Jeg trenger beredskap.";
    private static final String BEGRUNNELSE1 = "Alt skal være ok.";

    @Test
    public void beredskap_skal_oppdateres() {
        opprettGrunnlag(BEHANDLING1);
        var opprinneligGrunnlag = repo.hentHvisEksistererUnntakPleietrengende(pleietrengende).get();
        sjekkUnntakEtablertTilsyn(opprinneligGrunnlag.getBeredskap(), "", Resultat.IKKE_VURDERT);
        assertThat(opprinneligGrunnlag.getNattevåk()).isNull();

        godkjennBeredskap(BEHANDLING1);
        var oppdaterGrunnlag = repo.hentHvisEksistererUnntakPleietrengende(pleietrengende).get();
        sjekkUnntakEtablertTilsyn(oppdaterGrunnlag.getBeredskap(), BEGRUNNELSE1, Resultat.OPPFYLT);
        assertThat(oppdaterGrunnlag.getNattevåk()).isNull();
    }


    private void sjekkUnntakEtablertTilsyn(UnntakEtablertTilsyn unntakEtablertTilsyn, String begrunnelse, Resultat resultat) {
        assertThat(unntakEtablertTilsyn).isNotNull();
        assertThat(unntakEtablertTilsyn.getBeskrivelser()).hasSize(1);
        assertThat(unntakEtablertTilsyn.getBeskrivelser().get(0).getPeriode()).isEqualTo(PERIODE1);
        assertThat(unntakEtablertTilsyn.getBeskrivelser().get(0).getTekst()).isEqualTo(BESKRIVELSE1);
        assertThat(unntakEtablertTilsyn.getPerioder()).hasSize(1);
        assertThat(unntakEtablertTilsyn.getPerioder().get(0).getPeriode()).isEqualTo(PERIODE1);
        assertThat(unntakEtablertTilsyn.getPerioder().get(0).getBegrunnelse()).isEqualTo(begrunnelse);
        assertThat(unntakEtablertTilsyn.getPerioder().get(0).getResultat()).isEqualTo(resultat);
    }


    private void opprettGrunnlag(Long behandlingId) {
        var beredskap = new UnntakEtablertTilsyn(
            List.of(
                new UnntakEtablertTilsynPeriode(
                    PERIODE1,
                    "",
                    Resultat.IKKE_VURDERT,
                    pleietrengende,
                    123L
                )
            ),
            List.of(
                new UnntakEtablertTilsynBeskrivelse(
                    PERIODE1,
                    LocalDate.now(),
                    BESKRIVELSE1,
                    pleietrengende,
                    123L
                )
            )
        );
        var unntakForPleietrengende = new UnntakEtablertTilsynForPleietrengende(pleietrengende, beredskap, null);
        repo.lagre(behandlingId, unntakForPleietrengende);
    }

    private void godkjennBeredskap(Long behandlingId) {
        var vurderinger = List.of(new VurderingDto("Alt skal være ok.", Resultat.OPPFYLT, PERIODE1.tilPeriode()));
        service.oppdater(vurderinger, Vurderingstype.BEREDSKAP, behandlingId, søker, pleietrengende);
    }


}
