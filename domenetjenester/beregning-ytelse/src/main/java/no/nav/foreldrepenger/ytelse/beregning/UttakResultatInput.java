package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt.Uttaksplan;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

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
