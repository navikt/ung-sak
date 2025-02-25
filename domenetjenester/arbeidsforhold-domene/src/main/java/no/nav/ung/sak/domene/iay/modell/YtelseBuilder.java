package no.nav.ung.sak.domene.iay.modell;

import java.util.Optional;

import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.arbeidsforhold.RelatertYtelseTilstand;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Saksnummer;

public class YtelseBuilder {

    private final Ytelse ytelse;
    private final boolean oppdaterer;

    private YtelseBuilder(Ytelse ytelse, boolean oppdaterer) {
        this.ytelse = ytelse;
        this.oppdaterer = oppdaterer;
    }

    private static YtelseBuilder ny() {
        return new YtelseBuilder(new Ytelse(), false);
    }

    private static YtelseBuilder oppdatere(Ytelse oppdatere) {
        return new YtelseBuilder(oppdatere, true);
    }

    public static YtelseBuilder oppdatere(Optional<Ytelse> oppdatere) {
        return oppdatere.map(YtelseBuilder::oppdatere).orElseGet(YtelseBuilder::ny);
    }

    public YtelseBuilder medYtelseType(FagsakYtelseType relatertYtelseType) {
        ytelse.setYtelseType(relatertYtelseType);
        return this;
    }

    public YtelseBuilder medStatus(RelatertYtelseTilstand relatertYtelseTilstand) {
        ytelse.setStatus(relatertYtelseTilstand);
        return this;
    }

    public YtelseBuilder medPeriode(DatoIntervallEntitet intervallEntitet) {
        ytelse.setPeriode(intervallEntitet);
        return this;
    }

    public YtelseBuilder medSaksnummer(Saksnummer sakId) {
        ytelse.medSakId(sakId);
        return this;
    }

    public YtelseBuilder medKilde(Fagsystem kilde) {
        ytelse.setKilde(kilde);
        return this;
    }

    public YtelseBuilder medYtelseGrunnlag(YtelseGrunnlag ytelseGrunnlag) {
        ytelse.setYtelseGrunnlag(ytelseGrunnlag);
        return this;
    }

    public YtelseBuilder medYtelseAnvist(YtelseAnvist ytelseAnvist) {
        ytelse.leggTilYtelseAnvist(ytelseAnvist);
        return this;
    }

    public DatoIntervallEntitet getPeriode() {
        return ytelse.getPeriode();
    }

    boolean getErOppdatering() {
        return this.oppdaterer;
    }

    public Ytelse build() {
        return ytelse;
    }

    public YtelseAnvistBuilder getAnvistBuilder() {
        return YtelseAnvistBuilder.ny();
    }

    public void tilbakestillAnvisteYtelser() {
        ytelse.tilbakestillAnvisteYtelser();
    }

    public YtelseGrunnlagBuilder getGrunnlagBuilder() {
        return YtelseGrunnlagBuilder.ny();
    }


}
