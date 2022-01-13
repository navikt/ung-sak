package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomResultatTypeConverter;
import no.nav.k9.sak.kontrakt.omsorg.BarnRelasjon;

@Entity(name = "OmsorgenForPeriode")
@Table(name = "OMSORGEN_FOR_PERIODE")
public class OmsorgenForPeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OMSORGEN_FOR_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Column(name = "relasjon")
    @Convert(converter = OmsorgRelasjonTypeConverter.class)
    private BarnRelasjon relasjon;

    @ChangeTracked
    @Column(name = "relasjonsbeskrivelse")
    private String relasjonsbeskrivelse;

    @ChangeTracked
    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Column(name = "resultat", nullable = false)
    @Convert(converter = SykdomResultatTypeConverter.class)
    private Resultat resultat;

    @ManyToOne
    @JoinColumn(name = "omsorgen_for_id", nullable = false, updatable = false, unique = true)
    private OmsorgenFor omsorgenFor;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;


    public OmsorgenForPeriode() {
    }

    public OmsorgenForPeriode(DatoIntervallEntitet periode, BarnRelasjon relasjon, String relasjonsbeskrivelse,
            String begrunnelse, Resultat resultat) {
        this.periode = periode;
        this.relasjon = relasjon;
        this.relasjonsbeskrivelse = relasjonsbeskrivelse;
        this.begrunnelse = begrunnelse;
        this.resultat = resultat;
    }

    OmsorgenForPeriode(OmsorgenForPeriode omsorgenForPeriode) {
        this(omsorgenForPeriode.periode, omsorgenForPeriode.relasjon, omsorgenForPeriode.relasjonsbeskrivelse, omsorgenForPeriode.begrunnelse, omsorgenForPeriode.resultat);
    }

    OmsorgenForPeriode(OmsorgenForPeriode omsorgenForPeriode, DatoIntervallEntitet periode) {
        this(periode, omsorgenForPeriode.relasjon, omsorgenForPeriode.relasjonsbeskrivelse, omsorgenForPeriode.begrunnelse, omsorgenForPeriode.resultat);
    }

    OmsorgenForPeriode(OmsorgenForPeriode omsorgenForPeriode, DatoIntervallEntitet periode, OmsorgenForSaksbehandlervurdering vurdering) {
        this(periode, omsorgenForPeriode.relasjon, omsorgenForPeriode.relasjonsbeskrivelse, vurdering.getBegrunnelse(), vurdering.getResultat());
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(periode);
    }

    void setOmsorgenFor(OmsorgenFor omsorgenFor) {
        this.omsorgenFor = omsorgenFor;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Long getId() {
        return id;
    }

    public BarnRelasjon getRelasjon() {
        return relasjon;
    }

    public String getRelasjonsbeskrivelse() {
        return relasjonsbeskrivelse;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public OmsorgenFor getOmsorgenFor() {
        return omsorgenFor;
    }

    public long getVersjon() {
        return versjon;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((begrunnelse == null) ? 0 : begrunnelse.hashCode());
        result = prime * result + ((periode == null) ? 0 : periode.hashCode());
        result = prime * result + ((relasjon == null) ? 0 : relasjon.hashCode());
        result = prime * result + ((relasjonsbeskrivelse == null) ? 0 : relasjonsbeskrivelse.hashCode());
        result = prime * result + ((resultat == null) ? 0 : resultat.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OmsorgenForPeriode other = (OmsorgenForPeriode) obj;
        if (begrunnelse == null) {
            if (other.begrunnelse != null)
                return false;
        } else if (!begrunnelse.equals(other.begrunnelse))
            return false;
        if (periode == null) {
            if (other.periode != null)
                return false;
        } else if (!periode.equals(other.periode))
            return false;
        if (relasjon == null) {
            if (other.relasjon != null)
                return false;
        } else if (!relasjon.equals(other.relasjon))
            return false;
        if (relasjonsbeskrivelse == null) {
            if (other.relasjonsbeskrivelse != null)
                return false;
        } else if (!relasjonsbeskrivelse.equals(other.relasjonsbeskrivelse))
            return false;
        if (resultat != other.resultat)
            return false;
        return true;
    }

    public static final OmsorgenForPeriode nyPeriodeFraSøker(DatoIntervallEntitet periode, BarnRelasjon relasjon, String relasjonsbeskrivelse) {
    	return new OmsorgenForPeriode(periode, relasjon, relasjonsbeskrivelse, null, Resultat.IKKE_VURDERT);

    }
}
