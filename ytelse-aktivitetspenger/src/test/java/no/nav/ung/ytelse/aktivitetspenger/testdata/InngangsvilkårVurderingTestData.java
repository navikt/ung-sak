package no.nav.ung.ytelse.aktivitetspenger.testdata;

import no.nav.ung.kodeverk.vilkår.AktivitetsvilkåretIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AndreLivsoppholdsytelserResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Testdatastruktur som samler vilkårsvurderinger for de ulike inngangsvilkårene i aktivitetspenger.
 * Brukes av {@link AktivitetspengerTestScenarioBuilder} for å lagre vurderingsresultatene ved bygging av behandlingen.
 */
public record InngangsvilkårVurderingTestData(
    List<BostedsvilkårResultatPeriode> bostedsvilkårResultater,
    List<BistandsvilkårResultatPeriode> bistandsvilkårResultater,
    List<AndreLivsoppholdsytelserResultatPeriode> andreYtelserResultater,
    List<AktivitetsvilkårResultatPeriode> aktivitetsvilkårResultater) {

    private static final String DEFAULT_BEGRUNNELSE = "Begrunnelse fra testscenario";
    private static final String DEFAULT_VURDERT_AV = "A111111";

    public InngangsvilkårVurderingTestData {
        bostedsvilkårResultater = List.copyOf(bostedsvilkårResultater);
        bistandsvilkårResultater = List.copyOf(bistandsvilkårResultater);
        andreYtelserResultater = List.copyOf(andreYtelserResultater);
        aktivitetsvilkårResultater = List.copyOf(aktivitetsvilkårResultater);
    }

    public static InngangsvilkårVurderingTestData tom() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private record BostedVurderingInput(Periode periode, boolean oppfylt, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
    }

    private record BistandVurderingInput(Periode periode, boolean oppfylt, BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
    }

    private record AndreYtelserVurderingInput(Periode periode, boolean oppfylt, AndreLivsoppholdsytelserIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
    }

    private record AktivitetVurderingInput(Periode periode, boolean oppfylt, AktivitetsvilkåretIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
    }

    public static class Builder {
        private final List<BostedVurderingInput> bostedsvilkårInput = new ArrayList<>();
        private final List<BistandVurderingInput> bistandsvilkårInput = new ArrayList<>();
        private final List<AndreYtelserVurderingInput> andreYtelserInput = new ArrayList<>();
        private final List<AktivitetVurderingInput> aktivitetsvilkårInput = new ArrayList<>();
        private Periode periode;

        public Builder medPeriode(Periode periode) {
            this.periode = periode;
            return this;
        }

        public Builder medBostedsvilkårResultat(boolean oppfylt, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
            return medBostedsvilkårResultat(null, oppfylt, ikkeOppfyltÅrsak, fritekstTilBrev);
        }

        public Builder medBostedsvilkårResultat(Periode periode, boolean oppfylt, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
            bostedsvilkårInput.add(new BostedVurderingInput(periode, oppfylt, ikkeOppfyltÅrsak, fritekstTilBrev));
            return this;
        }

        public Builder medBistandsvilkårResultat(boolean oppfylt, BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
            return medBistandsvilkårResultat(null, oppfylt, ikkeOppfyltÅrsak, fritekstTilBrev);
        }

        public Builder medBistandsvilkårResultat(Periode periode, boolean oppfylt, BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
            bistandsvilkårInput.add(new BistandVurderingInput(periode, oppfylt, ikkeOppfyltÅrsak, fritekstTilBrev));
            return this;
        }

        public Builder medAndreYtelser(boolean oppfylt, AndreLivsoppholdsytelserIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
            return medAndreYtelser(null, oppfylt, ikkeOppfyltÅrsak, fritekstTilBrev);
        }

        public Builder medAndreYtelser(Periode periode, boolean oppfylt, AndreLivsoppholdsytelserIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
            andreYtelserInput.add(new AndreYtelserVurderingInput(periode, oppfylt, ikkeOppfyltÅrsak, fritekstTilBrev));
            return this;
        }

        public Builder medAktivitetsvilkårResultat(boolean oppfylt, AktivitetsvilkåretIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
            return medAktivitetsvilkårResultat(null, oppfylt, ikkeOppfyltÅrsak, fritekstTilBrev);
        }

        public Builder medAktivitetsvilkårResultat(Periode periode, boolean oppfylt, AktivitetsvilkåretIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
            aktivitetsvilkårInput.add(new AktivitetVurderingInput(periode, oppfylt, ikkeOppfyltÅrsak, fritekstTilBrev));
            return this;
        }

        public InngangsvilkårVurderingTestData build() {
            List<BostedsvilkårResultatPeriode> bostedsvilkårResultater = bostedsvilkårInput.stream()
                .map(input -> new BostedsvilkårResultatPeriode(
                    tilDatoIntervall(resolverPeriode(input.periode(), periode)),
                    input.oppfylt(),
                    input.ikkeOppfyltÅrsak(),
                    true,
                    DEFAULT_BEGRUNNELSE,
                    input.fritekstTilBrev(),
                    DEFAULT_VURDERT_AV,
                    LocalDateTime.now()))
                .toList();

            List<BistandsvilkårResultatPeriode> bistandsvilkårResultater = bistandsvilkårInput.stream()
                .map(input -> new BistandsvilkårResultatPeriode(
                    tilDatoIntervall(resolverPeriode(input.periode(), periode)),
                    input.oppfylt(),
                    input.ikkeOppfyltÅrsak(),
                    true,
                    DEFAULT_BEGRUNNELSE,
                    input.fritekstTilBrev(),
                    DEFAULT_VURDERT_AV,
                    LocalDateTime.now()))
                .toList();

            List<AndreLivsoppholdsytelserResultatPeriode> andreYtelserResultater = andreYtelserInput.stream()
                .map(input -> new AndreLivsoppholdsytelserResultatPeriode(
                    tilDatoIntervall(resolverPeriode(input.periode(), periode)),
                    input.oppfylt(),
                    input.ikkeOppfyltÅrsak(),
                    true,
                    DEFAULT_BEGRUNNELSE,
                    input.fritekstTilBrev(),
                    DEFAULT_VURDERT_AV,
                    LocalDateTime.now()))
                .toList();

            List<AktivitetsvilkårResultatPeriode> aktivitetsvilkårResultater = aktivitetsvilkårInput.stream()
                .map(input -> new AktivitetsvilkårResultatPeriode(
                    tilDatoIntervall(resolverPeriode(input.periode(), periode)),
                    input.oppfylt(),
                    input.ikkeOppfyltÅrsak(),
                    true,
                    DEFAULT_BEGRUNNELSE,
                    input.fritekstTilBrev(),
                    DEFAULT_VURDERT_AV,
                    LocalDateTime.now()))
                .toList();

            return new InngangsvilkårVurderingTestData(bostedsvilkårResultater, bistandsvilkårResultater, andreYtelserResultater, aktivitetsvilkårResultater);
        }

        private static Periode resolverPeriode(Periode periodePåVurdering, Periode fellesPeriode) {
            if (periodePåVurdering != null && fellesPeriode != null) {
                throw new IllegalStateException("Periode er både satt direkte på en vurdering og felles for builderen - oppgi kun én av delene");
            }
            if (periodePåVurdering == null && fellesPeriode == null) {
                throw new IllegalStateException("Periode må enten oppgis direkte på vurderingen eller settes felles via medPeriode(...)");
            }
            return periodePåVurdering != null ? periodePåVurdering : fellesPeriode;
        }

        private static DatoIntervallEntitet tilDatoIntervall(Periode periode) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
        }
    }
}
