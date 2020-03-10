package no.nav.foreldrepenger.domene.uttak.input;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.AktørId;

/** Inputstruktur for uttak tjenester. */
public class UttakInput {

    private BehandlingReferanse behandlingReferanse;
    private Collection<BeregningsgrunnlagStatusPeriode> beregningsgrunnlagStatusPerioder = Collections.emptyList();
    private final InntektArbeidYtelseGrunnlag iayGrunnlag;
    private LocalDate søknadMottattDato;
    private LocalDate medlemskapOpphørsdato;
    private Set<BehandlingÅrsakType> behandlingÅrsaker = Collections.emptySet();
    private boolean behandlingManueltOpprettet;
    private boolean opplysningerOmDødEndret;
    
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
        this.beregningsgrunnlagStatusPerioder = List.copyOf(input.beregningsgrunnlagStatusPerioder);
        this.søknadMottattDato = input.søknadMottattDato;
        this.medlemskapOpphørsdato = input.medlemskapOpphørsdato;
        this.behandlingÅrsaker = input.behandlingÅrsaker;
        this.behandlingManueltOpprettet = input.behandlingManueltOpprettet;
        this.opplysningerOmDødEndret = input.opplysningerOmDødEndret;
        this.pleietrengende = input.pleietrengende;
    }

    public AktørId getAktørId() {
        return behandlingReferanse.getAktørId();
    }

    public BehandlingReferanse getBehandlingReferanse() {
        return behandlingReferanse;
    }

    public Collection<BeregningsgrunnlagStatusPeriode> getBeregningsgrunnlagStatusPerioder() {
        return beregningsgrunnlagStatusPerioder;
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

    public Optional<LocalDate> getMedlemskapOpphørsdato() {
        return Optional.ofNullable(medlemskapOpphørsdato);
    }

    public boolean harBehandlingÅrsak(BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingÅrsaker.stream().anyMatch(årsak -> årsak.equals(behandlingÅrsakType));
    }

    public boolean isBehandlingManueltOpprettet() {
        return behandlingManueltOpprettet;
    }

    public boolean isOpplysningerOmDødEndret() {
        return opplysningerOmDødEndret;
    }

    public UttakInput medBeregningsgrunnlagPerioder(Collection<BeregningsgrunnlagStatusPeriode> statusPerioder) {
        var newInput = new UttakInput(this);
        newInput.beregningsgrunnlagStatusPerioder = List.copyOf(statusPerioder);
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

    public UttakInput medMedlemskapOpphørsdato(LocalDate opphørsdato) {
        var newInput = new UttakInput(this);
        newInput.medlemskapOpphørsdato = opphørsdato;
        return newInput;
    }

    public UttakInput medBehandlingÅrsaker(Set<BehandlingÅrsakType> behandlingÅrsaker) {
        var newInput = new UttakInput(this);
        newInput.behandlingÅrsaker = behandlingÅrsaker;
        return newInput;
    }

    public UttakInput medBehandlingManueltOpprettet(boolean behandlingManueltOpprettet) {
        var newInput = new UttakInput(this);
        newInput.behandlingManueltOpprettet = behandlingManueltOpprettet;
        return newInput;
    }

    public UttakInput medErOpplysningerOmDødEndret(boolean opplysningerOmDødEndret) {
        var newInput = new UttakInput(this);
        newInput.opplysningerOmDødEndret = opplysningerOmDødEndret;
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
