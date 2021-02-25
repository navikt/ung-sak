package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

public class InputParametere {

    private Behandling behandling;
    private PersonopplysningerAggregat personopplysningerAggregat;
    private Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderteSøknadsperioder;
    private NavigableSet<DatoIntervallEntitet> perioderTilVurdering;
    private Vilkårene vilkårene;
    private UttaksPerioderGrunnlag uttaksGrunnlag;
    private PleiebehovResultat pleiebehovResultat;
    private Set<Inntektsmelding> sakInntektsmeldinger;

    public InputParametere() {
    }

    public InputParametere medBehandling(Behandling behandling) {
        this.behandling = Objects.requireNonNull(behandling);
        return this;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public InputParametere medPersonopplysninger(PersonopplysningerAggregat personopplysningerAggregat) {
        this.personopplysningerAggregat = Objects.requireNonNull(personopplysningerAggregat);
        return this;
    }

    public PersonopplysningerAggregat getPersonopplysningerAggregat() {
        return personopplysningerAggregat;
    }

    public InputParametere medVurderteSøknadsperioder(Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> vurderteSøknadsperioder) {
        this.vurderteSøknadsperioder = Objects.requireNonNull(vurderteSøknadsperioder);
        return this;
    }

    public Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> getVurderteSøknadsperioder() {
        return vurderteSøknadsperioder;
    }

    public InputParametere medPerioderTilVurdering(NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        this.perioderTilVurdering = Objects.requireNonNull(perioderTilVurdering);
        return this;
    }

    public NavigableSet<DatoIntervallEntitet> getPerioderTilVurdering() {
        return perioderTilVurdering;
    }

    public InputParametere medVilkårene(Vilkårene vilkårene) {
        this.vilkårene = Objects.requireNonNull(vilkårene);
        return this;
    }

    public Vilkårene getVilkårene() {
        return vilkårene;
    }

    public InputParametere medUttaksGrunnlag(UttaksPerioderGrunnlag uttaksPerioderGrunnlag) {
        this.uttaksGrunnlag = Objects.requireNonNull(uttaksPerioderGrunnlag);
        return this;
    }

    public UttaksPerioderGrunnlag getUttaksGrunnlag() {
        return uttaksGrunnlag;
    }

    public InputParametere medPleiebehovResultat(PleiebehovResultat pleiebehovResultat) {
        this.pleiebehovResultat = Objects.requireNonNull(pleiebehovResultat);
        return this;
    }

    public PleiebehovResultat getPleiebehovResultat() {
        return pleiebehovResultat;
    }

    public InputParametere medInntektsmeldinger(Set<Inntektsmelding> inntektsmeldinger) {
        this.sakInntektsmeldinger = Objects.requireNonNull(inntektsmeldinger);
        return this;
    }

    public Set<Inntektsmelding> getSakInntektsmeldinger() {
        return sakInntektsmeldinger;
    }
}
