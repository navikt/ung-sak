package no.nav.ung.sak.ungdomsprogram;

import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste.EndretDato;

import java.util.ArrayList;
import java.util.List;

public class EndretPeriodeOgType {
    private final EndringType endringType;
    private final List<EndretDato> endretDatoer;

    public EndretPeriodeOgType(EndringType endringType, List<EndretDato> endretDatoer) {
        this.endretDatoer = endretDatoer;
        this.endringType = endringType;
    }

    public EndretPeriodeOgType(EndringType endringType, List<EndretDato> endretDatoer1, List<EndretDato> endretDatoer2) {
        this.endringType = endringType;
        this.endretDatoer = new ArrayList<>();
        this.endretDatoer.addAll(endretDatoer1);
        this.endretDatoer.addAll(endretDatoer2);
    }

    public EndringType getEndringType() {
        return endringType;
    }

    public List<EndretDato> getEndretDatoer() {
        return endretDatoer;
    }
}
