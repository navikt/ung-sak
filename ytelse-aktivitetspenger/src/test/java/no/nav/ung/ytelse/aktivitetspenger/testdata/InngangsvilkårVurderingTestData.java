package no.nav.ung.ytelse.aktivitetspenger.testdata;

import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Testdatastruktur som samler vilkårsvurderinger (resultatperioder fra faktaavklaring) for de ulike
 * inngangsvilkårene i aktivitetspenger. Brukes av {@link AktivitetspengerTestScenarioBuilder} for å lagre
 * vurderingsresultatene ved bygging av behandlingen.
 * <p>
 * Bruk {@link Builder} for å legge til kun resultatperioder for de vilkårene som er relevante for testen,
 * uten å måtte ta stilling til de andre. Builderen setter selv rimelige testverdier (begrunnelse, vurdertAv,
 * vurdertTidspunkt, manuell vurdering) - kall-stedet trenger kun oppgi periode, om vilkåret er oppfylt,
 * ikke-oppfylt-årsak og evt. fritekst til brev.
 */
public record InngangsvilkårVurderingTestData(
    List<BostedsvilkårResultatPeriode> bostedsvilkårResultater,
    List<BistandsvilkårResultatPeriode> bistandsvilkårResultater) {

    private static final String DEFAULT_BEGRUNNELSE = "Begrunnelse fra testscenario";
    private static final String DEFAULT_VURDERT_AV = "A111111";

    public InngangsvilkårVurderingTestData {
        bostedsvilkårResultater = List.copyOf(bostedsvilkårResultater);
        bistandsvilkårResultater = List.copyOf(bistandsvilkårResultater);
    }

    public static InngangsvilkårVurderingTestData tom() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Midlertidig internt holdeobjekt for en vurdering før perioden er endelig avklart.
     * Perioden kan enten oppgis direkte på vurderingen, eller settes felles for alle vurderinger
     * via {@link Builder#medPeriode(Periode)} - se {@link #resolverPeriode(Periode, Periode)}.
     */
    private record BostedVurderingInput(Periode periode, boolean oppfylt, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
    }

    private record BistandVurderingInput(Periode periode, boolean oppfylt, BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
    }

    public static class Builder {
        private final List<BostedVurderingInput> bostedsvilkårInput = new ArrayList<>();
        private final List<BistandVurderingInput> bistandsvilkårInput = new ArrayList<>();
        private Periode periode;

        /**
         * Setter en felles periode som brukes for alle vurderinger som ikke har fått oppgitt egen periode.
         * Kan ikke kombineres med å oppgi periode direkte på en enkelt vurdering.
         */
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

            return new InngangsvilkårVurderingTestData(bostedsvilkårResultater, bistandsvilkårResultater);
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
