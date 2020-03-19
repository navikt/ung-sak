package no.nav.k9.sak.domene.uttak.input;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiebehov.Pleieperioder;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
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
    private Ferie ferie;

    private final InntektArbeidYtelseGrunnlag iayGrunnlag;
    private Pleieperioder pleieperioder;

    private Person pleietrengende;

    /** Map av relaterte saker i sakskomplekset. */
    private Map<AktørId, Saksnummer> relaterteSaker = Collections.emptyMap();

    private Person søker;
    private LocalDate søknadMottattDato;
    private Søknadsperioder søknadsperioder;
    private OppgittTilsynsordning tilsynsordning;
    private Collection<UttakAktivitetPeriode> uttakAktivitetPerioder = Collections.emptyList();
    private VurdertMedlemskapPeriodeEntitet medlemskap;

    public UttakInput(BehandlingReferanse behandlingReferanse,
                      InntektArbeidYtelseGrunnlag iayGrunnlag) {
        this.behandlingReferanse = Objects.requireNonNull(behandlingReferanse, "behandlingReferanse");
        this.iayGrunnlag = iayGrunnlag;
    }

    private UttakInput(UttakInput input) {
        this(input.getBehandlingReferanse(), input.getIayGrunnlag());
        this.uttakAktivitetPerioder = List.copyOf(input.uttakAktivitetPerioder);
        this.søknadMottattDato = input.søknadMottattDato;
        this.pleietrengende = input.pleietrengende;
        this.søker = input.søker;
        this.søknadsperioder = input.søknadsperioder;
        this.ferie = input.ferie;
        this.tilsynsordning = input.tilsynsordning;
        this.relaterteSaker = input.relaterteSaker;
        this.pleieperioder = input.pleieperioder;
        this.medlemskap= input.medlemskap;
    }

    public AktørId getAktørId() {
        return behandlingReferanse.getAktørId();
    }

    public BehandlingReferanse getBehandlingReferanse() {
        return behandlingReferanse;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return behandlingReferanse.getFagsakYtelseType();
    }

    public Ferie getFerie() {
        return ferie;
    }

    public InntektArbeidYtelseGrunnlag getIayGrunnlag() {
        return iayGrunnlag;
    }
    
    public VurdertMedlemskapPeriodeEntitet getMedlemskap() {
        return medlemskap;
    }

    public Pleieperioder getPleieperioder() {
        return pleieperioder;
    }

    public Person getPleietrengende() {
        return pleietrengende;
    }

    public Map<AktørId, Saksnummer> getRelaterteSaker() {
        return relaterteSaker;
    }

    public Skjæringstidspunkt getSkjæringstidspunkt() {
        return behandlingReferanse.getSkjæringstidspunkt();
    }

    public Person getSøker() {
        return søker;
    }

    public LocalDate getSøknadMottattDato() {
        return søknadMottattDato;
    }

    public Søknadsperioder getSøknadsperioder() {
        return søknadsperioder;
    }

    public OppgittTilsynsordning getTilsynsordning() {
        return tilsynsordning;
    }

    public Collection<UttakAktivitetPeriode> getUttakAktivitetPerioder() {
        return uttakAktivitetPerioder;
    }

    public UttakYrkesaktiviteter getYrkesaktiviteter() {
        return new UttakYrkesaktiviteter(this);
    }

    public boolean harAktørArbeid() {
        return iayGrunnlag != null && iayGrunnlag.getAktørArbeidFraRegister(getAktørId()).isPresent();
    }

    public UttakInput medFerie(Ferie ferie) {
        var newInput = new UttakInput(this);
        newInput.ferie = ferie;
        return newInput;
    }

    public UttakInput medPleieperioder(Pleieperioder pleieperioder) {
        var newInput = new UttakInput(this);
        newInput.pleieperioder = pleieperioder;
        return newInput;
    }

    public UttakInput medPleietrengende(Person pleietrengende) {
        var newInput = new UttakInput(this);
        newInput.pleietrengende = pleietrengende;
        return newInput;
    }

    public UttakInput medSøker(Person søker) {
        var newInput = new UttakInput(this);
        newInput.søker = søker;
        return newInput;
    }

    public UttakInput medSøknadMottattDato(LocalDate mottattDato) {
        var newInput = new UttakInput(this);
        newInput.søknadMottattDato = mottattDato;
        return newInput;
    }

    public UttakInput medSøknadsperioder(Søknadsperioder søknadsperioder) {
        var newInput = new UttakInput(this);
        newInput.søknadsperioder = søknadsperioder;
        return newInput;
    }

    public UttakInput medTilsynsordning(OppgittTilsynsordning tilsynsordning) {
        var newInput = new UttakInput(this);
        newInput.tilsynsordning = tilsynsordning;
        return newInput;
    }

    public UttakInput medUttakAktivitetPerioder(Collection<UttakAktivitetPeriode> statusPerioder) {
        var newInput = new UttakInput(this);
        newInput.uttakAktivitetPerioder = List.copyOf(statusPerioder);
        return newInput;
    }

    public UttakInput medMedlemskap(VurdertMedlemskapPeriodeEntitet medlemskap) {
        var newInput = new UttakInput(this);
        newInput.medlemskap = medlemskap;
        return newInput;
    }
}
