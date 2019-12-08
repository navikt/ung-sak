package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Grunnbeløp;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;

public abstract class GrunnbeløpTjeneste {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    public GrunnbeløpTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public abstract Integer finnAntallGrunnbeløpMilitærHarKravPå();
    
    protected List<Grunnbeløp> mapGrunnbeløpSatser() {
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
        BeregningSats g = beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, dato);
        BeregningSats gSnitt = beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GSNITT, g.getPeriode().getFomDato());
        return new Grunnbeløp(g.getPeriode().getFomDato(), g.getPeriode().getTomDato(), g.getVerdi(), gSnitt.getVerdi());
    }

}
