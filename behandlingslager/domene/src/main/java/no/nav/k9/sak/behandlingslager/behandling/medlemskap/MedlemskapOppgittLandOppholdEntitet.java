package no.nav.k9.sak.behandlingslager.behandling.medlemskap;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.behandlingslager.kodeverk.LandkoderKodeverdiConverter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Entitetsklasse for opphold.
 */
@Entity(name = "OppgittLandOpphold")
@Table(name = "MEDLEMSKAP_OPPG_LAND")
public class MedlemskapOppgittLandOppholdEntitet extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MEDLEMSKAP_OPPG_LAND")
    private Long id;

    @Convert(converter = LandkoderKodeverdiConverter.class)
    @Column(name="land", nullable = false)
    private Landkoder land = Landkoder.UDEFINERT;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "periode_fom")),
        @AttributeOverride(name = "tomDato", column = @Column(name = "periode_tom"))
    })
    private DatoIntervallEntitet periode;


    /** @deprecated overlater til skjermbilde og saksbehandler å vurdere periodens relasjon til medlemskapsvilkåret */
    @Deprecated(forRemoval = true)
    @Column(name = "tidligere_opphold", nullable = false)
    private boolean tidligereOpphold;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medlemskap_oppg_tilknyt_id", nullable = false, updatable = false)
    private MedlemskapOppgittTilknytningEntitet oppgittTilknytning;

    MedlemskapOppgittLandOppholdEntitet() {
        // Hibernate
    }

    MedlemskapOppgittLandOppholdEntitet(MedlemskapOppgittLandOppholdEntitet utlandsopphold) {
        this.setLand(utlandsopphold.getLand());
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(
            utlandsopphold.getPeriodeFom(),
            utlandsopphold.getPeriodeTom()
                );

        // kopier ikke oppgitt tilknytning. Det settes p.t. separat i builder (setOppgittTilknytning) for å knytte til OppgittTilknytningEntitet
    }


    @Override
    public String getIndexKey() {
        Object[] keyParts = { this.land, periode };
        return IndexKeyComposer.createKey(keyParts);
    }


    public Landkoder getLand() {
        return Objects.equals(Landkoder.UDEFINERT, land) ? null : land;
    }


    public LocalDate getPeriodeFom() {
        return periode != null ? periode.getFomDato() : null;
    }


    public LocalDate getPeriodeTom() {
        return periode != null ? periode.getTomDato() : null;
    }

    void setLand(Landkoder land) {
        this.land = land == null ? Landkoder.UDEFINERT : land;
    }

    void setPeriode(LocalDate periodeFom, LocalDate periodeTom) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(periodeFom, periodeTom);
    }

    void setOppgittTilknytning(MedlemskapOppgittTilknytningEntitet oppgittTilknytning) {
        this.oppgittTilknytning = oppgittTilknytning;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof MedlemskapOppgittLandOppholdEntitet)) {
            return false;
        }
        MedlemskapOppgittLandOppholdEntitet other = (MedlemskapOppgittLandOppholdEntitet) obj;
        return Objects.equals(this.getLand(), other.getLand())
                && Objects.equals(this.periode, other.periode)
                && Objects.equals(this.tidligereOpphold, other.tidligereOpphold);
    }


    @Override
    public int hashCode() {
        return Objects.hash(getLand(), periode, tidligereOpphold);
    }

    public static class Builder {
        private MedlemskapOppgittLandOppholdEntitet oppholdMal;

        public Builder() {
            oppholdMal = new MedlemskapOppgittLandOppholdEntitet();
        }

        public Builder(MedlemskapOppgittLandOppholdEntitet utlandsopphold) {
            if (utlandsopphold != null) {
                oppholdMal = new MedlemskapOppgittLandOppholdEntitet(utlandsopphold);
            } else {
                oppholdMal = new MedlemskapOppgittLandOppholdEntitet();
            }
        }

        public Builder medLand(Landkoder land) {
            oppholdMal.setLand(land);
            return this;
        }

        public Builder medPeriode(LocalDate periodeStartdato, LocalDate periodeSluttdato) {
            oppholdMal.periode = DatoIntervallEntitet.fraOgMedTilOgMed(periodeStartdato, periodeSluttdato);
            return this;
        }

        public MedlemskapOppgittLandOppholdEntitet build() {
            return oppholdMal;
        }
    }
}
