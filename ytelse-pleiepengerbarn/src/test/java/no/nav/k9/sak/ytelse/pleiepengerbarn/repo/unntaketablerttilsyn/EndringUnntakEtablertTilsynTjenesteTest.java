package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.AktørId;

public class EndringUnntakEtablertTilsynTjenesteTest {

    @Test
    public void utledEndringerSkalHåndtereIngenEndring() {
        final EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste = new EndringUnntakEtablertTilsynTjeneste(null);
        final AktørId pleietrengende = AktørId.dummy();
        
        final UnntakEtablertTilsynForPleietrengende eksisterendeGrunnlag = new UnntakEtablertTilsynForPleietrengende(
            pleietrengende,
            new UnntakEtablertTilsyn(List.of(
                new UnntakEtablertTilsynPeriode(
                    DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 8)),
                    "Fordi forda",
                    Resultat.OPPFYLT,
                    AktørId.dummy(),
                    1L
                )
            ), List.of()),
            new UnntakEtablertTilsyn(List.of(), List.of())
        );
        
        final UnntakEtablertTilsynForPleietrengende nyttGrunnlag = new UnntakEtablertTilsynForPleietrengende(
                pleietrengende,
                new UnntakEtablertTilsyn(List.of(
                    new UnntakEtablertTilsynPeriode(
                        DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 8)),
                        "Ny grunn",
                        Resultat.OPPFYLT,
                        AktørId.dummy(),
                        2L
                    )
                ), List.of()),
                new UnntakEtablertTilsyn(List.of(), List.of())
            );
        
        final List<DatoIntervallEntitet> endringer = endringUnntakEtablertTilsynTjeneste.utledEndringer(Optional.of(eksisterendeGrunnlag), Optional.of(nyttGrunnlag));
        assertThat(endringer).isEmpty();
    }
    
    @Test
    public void utledEndringerSkalHåndtereTomEksisterende() {
        final EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste = new EndringUnntakEtablertTilsynTjeneste(null);
        final AktørId pleietrengende = AktørId.dummy();

        final UnntakEtablertTilsynForPleietrengende nyttGrunnlag = new UnntakEtablertTilsynForPleietrengende(
                pleietrengende,
                new UnntakEtablertTilsyn(List.of(
                    new UnntakEtablertTilsynPeriode(
                        DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 8)),
                        "Ny grunn",
                        Resultat.OPPFYLT,
                        AktørId.dummy(),
                        2L
                    )
                ), List.of()),
                new UnntakEtablertTilsyn(List.of(), List.of())
            );
        
        final List<DatoIntervallEntitet> endringer = endringUnntakEtablertTilsynTjeneste.utledEndringer(Optional.empty(), Optional.of(nyttGrunnlag));
        assertThat(endringer).isNotEmpty();
    }
    
    @Test
    public void utledEndringerSkalHåndtereEndretResultat() {
        final EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste = new EndringUnntakEtablertTilsynTjeneste(null);
        final AktørId pleietrengende = AktørId.dummy();
        
        final UnntakEtablertTilsynForPleietrengende eksisterendeGrunnlag = new UnntakEtablertTilsynForPleietrengende(
            pleietrengende,
            new UnntakEtablertTilsyn(List.of(
                new UnntakEtablertTilsynPeriode(
                    DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 8)),
                    "Fordi forda",
                    Resultat.IKKE_OPPFYLT,
                    AktørId.dummy(),
                    1L
                )
            ), List.of()),
            new UnntakEtablertTilsyn(List.of(), List.of())
        );
        
        final UnntakEtablertTilsynForPleietrengende nyttGrunnlag = new UnntakEtablertTilsynForPleietrengende(
                pleietrengende,
                new UnntakEtablertTilsyn(List.of(
                    new UnntakEtablertTilsynPeriode(
                        DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 8)),
                        "Ny grunn",
                        Resultat.OPPFYLT,
                        AktørId.dummy(),
                        2L
                    )
                ), List.of()),
                new UnntakEtablertTilsyn(List.of(), List.of())
            );
        
        final List<DatoIntervallEntitet> endringer = endringUnntakEtablertTilsynTjeneste.utledEndringer(Optional.of(eksisterendeGrunnlag), Optional.of(nyttGrunnlag));
        assertThat(endringer).isNotEmpty();
    }
}
