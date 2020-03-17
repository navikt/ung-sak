package no.nav.k9.sak.domene.uttak.input;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.uttak.repo.Ferie;
import no.nav.k9.sak.domene.uttak.repo.OppgittTilsynsordning;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.Person;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

/** Inputstruktur for uttak tjenester. */
public class UttakInput {

    private final BehandlingReferanse behandlingReferanse;
    private final InntektArbeidYtelseGrunnlag iayGrunnlag;

    private Collection<UttakAktivitetPeriode> UttakAktivitetPerioder = Collections.emptyList();
    private LocalDate søknadMottattDato;

    private Person pleietrengende;

    /** Map av relaterte saker i sakskomplekset. */
    private Map<AktørId, Saksnummer> relaterteSaker = Collections.emptyMap();

    private Person søker;
    private Søknadsperioder søknadsperioder;
    private Ferie ferie;
    private OppgittTilsynsordning tilsynsordning;

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

    public Map<AktørId, Saksnummer> getRelaterteSaker() {
        return relaterteSaker;
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

    public UttakInput medPleietrengende(Person pleietrengende) {
        this.pleietrengende = pleietrengende;
        return this;
    }

    public UttakInput medSøker(Person søker) {
        this.søker = søker;
        return this;
    }

    public Person getPleietrengende() {
        return pleietrengende;
    }

    public Søknadsperioder getSøknadsperioder() {
        return søknadsperioder;
    }

    public Ferie getFerie() {
        return ferie;
    }

    public OppgittTilsynsordning getTilsynsordning() {
        return tilsynsordning;
    }

    public Person getSøker() {
        return søker;
    }

    public UttakInput medSøknadsperioder(Søknadsperioder søknadsperioder) {
        this.søknadsperioder = søknadsperioder;
        return this;
    }

    public UttakInput medFerie(Ferie ferie) {
        this.ferie = ferie;
        return this;
    }

    public UttakInput medTilsynsordning(OppgittTilsynsordning tilsynsordning) {
        this.tilsynsordning = tilsynsordning;
        return this;
    }
}
