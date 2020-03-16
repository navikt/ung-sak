package no.nav.k9.sak.domene.uttak.input;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.typer.AktørId;

/** Inputstruktur for uttak tjenester. */
public class UttakInput {

    private final BehandlingReferanse behandlingReferanse;
    private final InntektArbeidYtelseGrunnlag iayGrunnlag;

    private Collection<UttakAktivitetPeriode> UttakAktivitetPerioder = Collections.emptyList();
    private LocalDate søknadMottattDato;

    private UttakPersonInfo pleietrengende;

    private Map<AktørId, UUID> relaterteBehandlinger = Collections.emptyMap();

    private UttakPersonInfo søker;

    public UttakInput(BehandlingReferanse behandlingReferanse,
                      InntektArbeidYtelseGrunnlag iayGrunnlag) {
        this.behandlingReferanse = Objects.requireNonNull(behandlingReferanse, "behandlingReferanse");
        this.iayGrunnlag = iayGrunnlag;
    }

    private UttakInput(UttakInput input) {
        this(input.getBehandlingReferanse(), input.getIayGrunnlag());
        this.UttakAktivitetPerioder = List.copyOf(input.UttakAktivitetPerioder);
        this.søknadMottattDato = input.søknadMottattDato;
        this.pleietrengende = input.pleietrengende;
        this.søker = input.søker;
    }

    public AktørId getAktørId() {
        return behandlingReferanse.getAktørId();
    }

    public BehandlingReferanse getBehandlingReferanse() {
        return behandlingReferanse;
    }

    public Collection<UttakAktivitetPeriode> getUttakAktivitetPerioder() {
        return UttakAktivitetPerioder;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return behandlingReferanse.getFagsakYtelseType();
    }

    public InntektArbeidYtelseGrunnlag getIayGrunnlag() {
        return iayGrunnlag;
    }

    public Skjæringstidspunkt getSkjæringstidspunkt() {
        return behandlingReferanse.getSkjæringstidspunkt();
    }

    public LocalDate getSøknadMottattDato() {
        return søknadMottattDato;
    }

    public UttakYrkesaktiviteter getYrkesaktiviteter() {
        return new UttakYrkesaktiviteter(this);
    }

    public UttakInput medUttakAktivitetPerioder(Collection<UttakAktivitetPeriode> statusPerioder) {
        var newInput = new UttakInput(this);
        newInput.UttakAktivitetPerioder = List.copyOf(statusPerioder);
        return newInput;
    }

    public boolean harAktørArbeid() {
        return iayGrunnlag != null && iayGrunnlag.getAktørArbeidFraRegister(getAktørId()).isPresent();
    }

    public UttakInput medSøknadMottattDato(LocalDate mottattDato) {
        var newInput = new UttakInput(this);
        newInput.søknadMottattDato = mottattDato;
        return newInput;
    }

    public UttakInput medPleietrengende(UttakPersonInfo pleietrengende) {
        this.pleietrengende = pleietrengende;
        return this;
    }

    public UttakInput medSøker(UttakPersonInfo søker) {
        this.søker = søker;
        return this;
    }

    public UttakPersonInfo getPleietrengende() {
        return pleietrengende;
    }

    public UttakPersonInfo getSøker() {
        return søker;
    }
}
