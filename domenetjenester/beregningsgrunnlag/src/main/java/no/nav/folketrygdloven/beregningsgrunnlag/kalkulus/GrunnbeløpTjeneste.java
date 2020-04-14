package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.Grunnbeløp;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningSatsType;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;

@Dependent
public class GrunnbeløpTjeneste {

    private BeregningsresultatRepository repository;

    public GrunnbeløpTjeneste() {
        // For CDI
    }

    @Inject
    public GrunnbeløpTjeneste(BeregningsresultatRepository repository) {
        this.repository = repository;
    }

    public List<Grunnbeløp> mapGrunnbeløpSatser() {
        List<Grunnbeløp> grunnbeløpListe = new ArrayList<>();
        int iår = LocalDate.now().getYear();
        for (int år = 2000; år <= iår; år++) {
            // Den vil ikke plukke opp alle grunnbeløp hvis det blir endret f.eks to ganger i året .
            LocalDate dato = LocalDate.now().withYear(år);
            Grunnbeløp grunnbeløp = grunnbeløpOgSnittFor(dato);
            grunnbeløpListe.add(grunnbeløp);
        }
        return grunnbeløpListe;
    }

    private Grunnbeløp grunnbeløpOgSnittFor(LocalDate dato) {
        BeregningSats g = repository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, dato);
        BeregningSats gSnitt = repository.finnEksaktSats(BeregningSatsType.GSNITT, g.getPeriode().getFomDato());
        return new Grunnbeløp(g.getPeriode().getFomDato(), g.getPeriode().getTomDato(), g.getVerdi(), gSnitt.getVerdi());
    }
}
