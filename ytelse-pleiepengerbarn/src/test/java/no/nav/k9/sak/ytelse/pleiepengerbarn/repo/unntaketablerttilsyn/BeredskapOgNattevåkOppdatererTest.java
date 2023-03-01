package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.sak.typer.AktørId;

class BeredskapOgNattevåkOppdatererTest {

    @Test
    void ingen_nye_perioder_fra_søknad() {
        var unntakEtablertTilsyn = BeredskapOgNattevåkOppdaterer.oppdaterMedPerioderFraSøknad(
            null,
            LocalDate.now(),
            AktørId.dummy(),
            123L,
            List.of(),
            List.of()
        );

        assertThat(unntakEtablertTilsyn.getPerioder()).hasSize(0);
        assertThat(unntakEtablertTilsyn.getBeskrivelser()).hasSize(0);
    }


    @Test
    void en_ny_perioder_fra_søknad() {
        var unntakEtablertTilsyn = BeredskapOgNattevåkOppdaterer.oppdaterMedPerioderFraSøknad(
            null,
            LocalDate.now(),
            AktørId.dummy(),
            123L,
            List.of(new Unntaksperiode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-31"), "Dette er en test.", Resultat.IKKE_VURDERT)),
            List.of()
        );

        assertThat(unntakEtablertTilsyn.getPerioder()).hasSize(1);
        var nyPeriode = unntakEtablertTilsyn.getPerioder().get(0);
        assertThat(nyPeriode.getBegrunnelse()).isEqualTo("");
        assertThat(nyPeriode.getResultat()).isEqualTo(Resultat.IKKE_VURDERT);
        assertThat(unntakEtablertTilsyn.getBeskrivelser()).hasSize(1);
        var nyBeskrivelse = unntakEtablertTilsyn.getBeskrivelser().get(0);
        assertThat(nyBeskrivelse.getTekst()).isEqualTo("Dette er en test.");
    }

    @Test
    void oppdatere_periode_i_aksjonspunkt() {
        setBruker("noen");

        var opprinneligUnntakEtablertTilsyn = BeredskapOgNattevåkOppdaterer.oppdaterMedPerioderFraSøknad(
            null,
            LocalDate.now(),
            AktørId.dummy(),
            123L,
            List.of(new Unntaksperiode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-31"), "Dette er en test.", Resultat.IKKE_VURDERT)),
            List.of()
        );

        assertThat(opprinneligUnntakEtablertTilsyn.getPerioder()).hasSize(1);
        var nyPeriode = opprinneligUnntakEtablertTilsyn.getPerioder().get(0);
        assertThat(nyPeriode.getBegrunnelse()).isEqualTo("");
        assertThat(nyPeriode.getResultat()).isEqualTo(Resultat.IKKE_VURDERT);
        assertThat(nyPeriode.getVurdertAv()).isEqualTo("noen");
        assertThat(nyPeriode.getVurdertTidspunkt()).isNotNull();
        assertThat(opprinneligUnntakEtablertTilsyn.getBeskrivelser()).hasSize(1);
        var nyBeskrivelse = opprinneligUnntakEtablertTilsyn.getBeskrivelser().get(0);
        assertThat(nyBeskrivelse.getTekst()).isEqualTo("Dette er en test.");

        var oppdatertUnntakEtablertTilsyn = BeredskapOgNattevåkOppdaterer.oppdaterMedPerioderFraAksjonspunkt(
            opprinneligUnntakEtablertTilsyn,
            LocalDate.now(),
            AktørId.dummy(),
            123L,
            List.of(new Unntaksperiode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-31"), "Nå er det ok.", Resultat.OPPFYLT))
        );

        assertThat(oppdatertUnntakEtablertTilsyn.getPerioder()).hasSize(1);
        var nyPeriode2 = oppdatertUnntakEtablertTilsyn.getPerioder().get(0);
        assertThat(nyPeriode2.getBegrunnelse()).isEqualTo("Nå er det ok.");
        assertThat(nyPeriode2.getResultat()).isEqualTo(Resultat.OPPFYLT);
        assertThat(nyPeriode2.getVurdertAv()).isEqualTo("noen");
        assertThat(nyPeriode2.getVurdertTidspunkt()).isNotNull();
        assertThat(oppdatertUnntakEtablertTilsyn.getBeskrivelser()).hasSize(1);
        var nyBeskrivelse2 = oppdatertUnntakEtablertTilsyn.getBeskrivelser().get(0);
        assertThat(nyBeskrivelse2.getTekst()).isEqualTo("Dette er en test.");
    }

    @Test
    void to_perioder_vurdert_av_forskjellige_brukere() {
        var opprinneligUnntakEtablertTilsyn = BeredskapOgNattevåkOppdaterer.oppdaterMedPerioderFraSøknad(
            null,
            LocalDate.now(),
            AktørId.dummy(),
            123L,
            List.of(new Unntaksperiode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-31"), "1", Resultat.IKKE_VURDERT),
                new Unntaksperiode(LocalDate.parse("2020-02-01"), LocalDate.parse("2020-02-28"), "2", Resultat.IKKE_VURDERT)),
            List.of()
        );

        setBruker("enball");

        var oppdatertUnntakEtablertTilsyn1 = BeredskapOgNattevåkOppdaterer.oppdaterMedPerioderFraAksjonspunkt(
            opprinneligUnntakEtablertTilsyn,
            LocalDate.now(),
            AktørId.dummy(),
            123L,
            List.of(new Unntaksperiode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-31"), "1", Resultat.OPPFYLT))
        );

        setBruker("toball");

        var oppdatertUnntakEtablertTilsyn2 = BeredskapOgNattevåkOppdaterer.oppdaterMedPerioderFraAksjonspunkt(
            oppdatertUnntakEtablertTilsyn1,
            LocalDate.now(),
            AktørId.dummy(),
            123L,
            List.of(new Unntaksperiode(LocalDate.parse("2020-02-01"), LocalDate.parse("2020-02-28"), "2", Resultat.OPPFYLT))
        );

        assertThat(oppdatertUnntakEtablertTilsyn2.getPerioder()).hasSize(2);
        var periode1 = oppdatertUnntakEtablertTilsyn2.getPerioder().stream().filter(periode -> periode.getPeriode().getFomDato().equals(LocalDate.parse("2020-01-01"))).findFirst().orElseThrow();
        assertThat(periode1.getVurdertAv()).isEqualTo("enball");
        assertThat(periode1.getVurdertTidspunkt()).isNotNull();
        var periode2 = oppdatertUnntakEtablertTilsyn2.getPerioder().stream().filter(periode -> periode.getPeriode().getFomDato().equals(LocalDate.parse("2020-02-01"))).findFirst().orElseThrow();
        assertThat(periode2.getVurdertAv()).isEqualTo("toball");
        assertThat(periode2.getVurdertTidspunkt()).isNotNull();
        assertThat(periode2.getVurdertTidspunkt()).isAfter(periode1.getVurdertTidspunkt());
    }

    private static void setBruker(String brukerId) {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(brukerId);
    }
}
