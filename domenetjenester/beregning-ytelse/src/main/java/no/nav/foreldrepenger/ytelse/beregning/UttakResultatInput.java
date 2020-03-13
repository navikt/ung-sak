package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.Uttaksplan;

public class UttakResultatInput {
    private Uttaksplan uttaksplan;
    private FagsakYtelseType ytelseType;

    public UttakResultatInput(FagsakYtelseType ytelseType, Uttaksplan uttaksplan) {
        this.ytelseType = ytelseType;
        this.uttaksplan = uttaksplan;
    }

    public Uttaksplan getUttaksplan() {
        return uttaksplan;
    }
    
    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }
}
