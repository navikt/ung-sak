package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;

@Entity(name = "Behandlingsresultat")
@Table(name = "BEHANDLING_RESULTAT")
public class Behandlingsresultat extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_RESULTAT")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne
    @JoinColumn(name = "inngangsvilkar_resultat_id"
    /* , updatable = false // får ikke satt denne til false, men skal aldri kunne endres dersom satt tidligere */
    /* , nullable=false // kan være null, men når den er satt kan ikke oppdateres */
    )
    private VilkårResultat vilkårResultat;

    /* bruker @ManyToOne siden JPA ikke støtter OneToOne join på non-PK column. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "behandlingsresultat")
    private BehandlingVedtak behandlingVedtak;

    @Convert(converter = BehandlingResultatType.KodeverdiConverter.class)
    @Column(name = "behandling_resultat_type", nullable = false)
    private BehandlingResultatType behandlingResultatType = BehandlingResultatType.IKKE_FASTSATT;

    @Convert(converter = Avslagsårsak.KodeverdiConverter.class)
    @Column(name = "avslag_arsak", nullable = false)
    private Avslagsårsak avslagsårsak = Avslagsårsak.UDEFINERT;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BehandlingsresultatKonsekvensForYtelsen> konsekvenserForYtelsen = new ArrayList<>();

    @Convert(converter = Vedtaksbrev.KodeverdiConverter.class)
    @Column(name = "vedtaksbrev", nullable = false)
    private Vedtaksbrev vedtaksbrev = Vedtaksbrev.UDEFINERT;

    @Column(name = "avslag_arsak_fritekst")
    private String avslagarsakFritekst;

    @Column(name = "overskrift")
    private String overskrift;

    @Lob
    @Column(name = "fritekstbrev")
    private String fritekstbrev;

    protected Behandlingsresultat() {
        // for hibernate
    }

    public VilkårResultat medOppdatertVilkårResultat(VilkårResultat nyttResultat) {
        if (nyttResultat != null && vilkårResultat != null && nyttResultat != vilkårResultat) {
            if (!nyttResultat.erLik(vilkårResultat)) {
                this.vilkårResultat = nyttResultat;
            }
        } else {
            this.vilkårResultat = nyttResultat;
        }
        return this.vilkårResultat;
    }

    public Long getId() {
        return id;
    }

    /**
     * @deprecated Ikke hent behandling herfra - bruk {@link #getBehandlingId()}
     */
    @Deprecated
    public Behandling getBehandling() {
        return behandling;
    }

    public Long getBehandlingId() {
        return behandling.getId();
    }

    public VilkårResultat getVilkårResultat() {
        return vilkårResultat;
    }

    /**
     * NB: ikke eksponer settere fra modellen. Skal ha package-scope.
     */
    void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public BehandlingVedtak getBehandlingVedtak() {
        return behandlingVedtak;
    }

    public BehandlingResultatType getBehandlingResultatType() {
        return behandlingResultatType;
    }

    public Avslagsårsak getAvslagsårsak() {
        return Objects.equals(avslagsårsak, Avslagsårsak.UDEFINERT) ? null : avslagsårsak;
    }

    public void setAvslagsårsak(Avslagsårsak avslagsårsak) {
        this.avslagsårsak = Optional.ofNullable(avslagsårsak).orElse(Avslagsårsak.UDEFINERT);
    }

    public String getAvslagarsakFritekst() {
        return avslagarsakFritekst;
    }

    public void setAvslagarsakFritekst(String avslagarsakFritekst) {
        this.avslagarsakFritekst = avslagarsakFritekst;
    }

    public String getOverskrift() {
        return overskrift;
    }

    public String getFritekstbrev() {
        return fritekstbrev;
    }

    public List<KonsekvensForYtelsen> getKonsekvenserForYtelsen() {
        return konsekvenserForYtelsen.stream().map(BehandlingsresultatKonsekvensForYtelsen::getKonsekvensForYtelsen).collect(Collectors.toList());
    }

    public Vedtaksbrev getVedtaksbrev() {
        return vedtaksbrev;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<>";
    }

    public static Behandlingsresultat opprettFor(Behandling behandling) {
        return builder().buildFor(behandling);
    }

    public static Builder builderForInngangsvilkår() {
        return new Builder(VilkårResultat.builder());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builderFraEksisterende(Behandlingsresultat behandlingsresultat) {
        return new Builder(behandlingsresultat, false);
    }

    public static Builder builderEndreEksisterende(Behandlingsresultat behandlingsresultat) {
        return new Builder(behandlingsresultat, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Behandlingsresultat)) {
            return false;
        }
        Behandlingsresultat that = (Behandlingsresultat) o;
        // Behandlingsresultat skal p.t. kun eksisterere dersom parent Behandling allerede er persistert.
        // Det syntaktisk korrekte vil derfor være at subaggregat Behandlingsresultat med 1:1-forhold til parent
        // Behandling har også sin id knyttet opp mot Behandling alene.
        return getBehandling().equals(that.getBehandling());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBehandling());
    }

    public static class Builder {

        private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        private VilkårResultat.Builder vilkårResultatBuilder;
        private boolean built;

        Builder(VilkårResultat.Builder builder) {
            this.vilkårResultatBuilder = builder;
        }

        Builder(Behandlingsresultat gammeltResultat, boolean endreEksisterende) {
            if (endreEksisterende) {
                behandlingsresultat = gammeltResultat;
            }
            if (gammeltResultat != null && gammeltResultat.getVilkårResultat() != null) {
                this.vilkårResultatBuilder = VilkårResultat
                    .builderFraEksisterende(gammeltResultat.getVilkårResultat());
            }
        }

        public Builder() {
            // empty builder
        }

        private void validerKanModifisere() {
            if (built)
                throw new IllegalStateException("Kan ikke bygge to ganger med samme builder");
        }

        public Builder medBehandlingResultatType(BehandlingResultatType behandlingResultatType) {
            validerKanModifisere();
            this.behandlingsresultat.behandlingResultatType = behandlingResultatType;
            return this;
        }

        public Builder leggTilKonsekvensForYtelsen(KonsekvensForYtelsen konsekvensForYtelsen) {
            validerKanModifisere();
            BehandlingsresultatKonsekvensForYtelsen behandlingsresultatKonsekvensForYtelsen = BehandlingsresultatKonsekvensForYtelsen.builder()
                .medKonsekvensForYtelsen(konsekvensForYtelsen).build(behandlingsresultat);
            this.behandlingsresultat.konsekvenserForYtelsen.add(behandlingsresultatKonsekvensForYtelsen);
            return this;
        }

        public Builder fjernKonsekvenserForYtelsen() {
            validerKanModifisere();
            this.behandlingsresultat.konsekvenserForYtelsen.clear();
            return this;
        }

        public Builder medVedtaksbrev(Vedtaksbrev vedtaksbrev) {
            validerKanModifisere();
            this.behandlingsresultat.vedtaksbrev = vedtaksbrev;
            return this;
        }

        public Builder medAvslagsårsak(Avslagsårsak avslagsårsak) {
            validerKanModifisere();
            this.behandlingsresultat.avslagsårsak = Optional.ofNullable(avslagsårsak).orElse(Avslagsårsak.UDEFINERT);
            return this;
        }

        public Builder medAvslagarsakFritekst(String avslagarsakFritekst) {
            validerKanModifisere();
            this.behandlingsresultat.avslagarsakFritekst = avslagarsakFritekst;
            return this;
        }

        public Builder medOverskrift(String overskrift) {
            validerKanModifisere();
            this.behandlingsresultat.overskrift = overskrift;
            return this;
        }

        public Builder medFritekstbrev(String fritekstbrev) {
            validerKanModifisere();
            this.behandlingsresultat.fritekstbrev = fritekstbrev;
            return this;
        }

        public Behandlingsresultat build() {
            if (vilkårResultatBuilder != null) {
                VilkårResultat vilkårResultat = vilkårResultatBuilder.buildFor(behandlingsresultat);
                behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
            }
            built = true;
            return behandlingsresultat;
        }

        /**
         * @deprecated bruk #build() og lagre gjennom BehandlingsresultatRepository i stedet
         */
        @Deprecated
        public Behandlingsresultat buildFor(Behandling behandling) {
            behandling.setBehandlingresultat(behandlingsresultat);
            if (vilkårResultatBuilder != null) {
                VilkårResultat vilkårResultat = vilkårResultatBuilder.buildFor(behandlingsresultat);
                behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
            }
            built = true;
            return behandlingsresultat;
        }
    }

    public boolean isBehandlingHenlagt() {
        return BehandlingResultatType.getAlleHenleggelseskoder().contains(behandlingResultatType);
    }

    public boolean isBehandlingsresultatAvslåttOrOpphørt() {
        return BehandlingResultatType.AVSLÅTT.equals(behandlingResultatType)
            || BehandlingResultatType.OPPHØR.equals(behandlingResultatType);
    }

    public boolean isBehandlingsresultatAvslått() {
        return BehandlingResultatType.AVSLÅTT.equals(behandlingResultatType);
    }

    public boolean isBehandlingsresultatOpphørt() {
        return BehandlingResultatType.OPPHØR.equals(behandlingResultatType);
    }

    public boolean isBehandlingsresultatInnvilget() {
        return BehandlingResultatType.INNVILGET.equals(behandlingResultatType);
    }

    public boolean isBehandlingsresultatForeldrepengerEndret() {
        return BehandlingResultatType.INNVILGET_ENDRING.equals(behandlingResultatType);
    }

    public boolean isBehandlingsresultatIkkeEndret() {
        return BehandlingResultatType.INGEN_ENDRING.equals(behandlingResultatType);
    }

    public boolean isVilkårAvslått() {
        return VilkårResultatType.AVSLÅTT.equals(vilkårResultat.getVilkårResultatType());
    }

    public boolean isBehandlingsresultatHenlagt() {
        return BehandlingResultatType.getHenleggelseskoderForSøknad().contains(behandlingResultatType);
    }
}
