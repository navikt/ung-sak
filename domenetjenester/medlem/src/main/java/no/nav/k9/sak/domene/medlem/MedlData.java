package no.nav.k9.sak.domene.medlem;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapKildeType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

public class MedlData {

    @ChangeTracked
    private LocalDate beslutningsdato;

    @ChangeTracked
    private boolean erMedlem;

    @ChangeTracked
    private Landkoder lovvalgLand = Landkoder.UDEFINERT;

    @ChangeTracked
    private Landkoder studieLand = Landkoder.UDEFINERT;

    private MedlemskapType medlemskapType = MedlemskapType.UDEFINERT;

    @ChangeTracked
    private MedlemskapDekningType dekningType = MedlemskapDekningType.UDEFINERT;

    @ChangeTracked
    private MedlemskapKildeType kildeType = MedlemskapKildeType.UDEFINERT;

    private Long medlId;

    public MedlData(MedlemskapPerioderEntitet entitet) {
        this.beslutningsdato = entitet.getBeslutningsdato();
        this.erMedlem = entitet.getErMedlem();
        this.lovvalgLand = entitet.getLovvalgLand();
        this.studieLand = entitet.getStudieland();
        this.medlemskapType = entitet.getMedlemskapType();
        this.dekningType = entitet.getDekningType();
        this.kildeType = entitet.getKildeType();
        this.medlId = entitet.getMedlId();
    }

    public MedlData(MedlData forfeder) {
        this.beslutningsdato = forfeder.beslutningsdato;
        this.erMedlem = forfeder.erMedlem;
        this.lovvalgLand = forfeder.lovvalgLand;
        this.studieLand = forfeder.studieLand;
        this.medlemskapType = forfeder.medlemskapType;
        this.dekningType = forfeder.dekningType;
        this.kildeType = forfeder.kildeType;
        this.medlId = forfeder.medlId;
    }

    public LocalDate getBeslutningsdato() {
        return beslutningsdato;
    }

    public boolean getErMedlem() {
        return erMedlem;
    }

    public Landkoder getLovvalgLand() {
        return lovvalgLand;
    }

    public Landkoder getStudieLand() {
        return studieLand;
    }

    public MedlemskapType getMedlemskapType() {
        return medlemskapType;
    }

    public MedlemskapDekningType getDekningType() {
        return dekningType;
    }

    public MedlemskapKildeType getKildeType() {
        return kildeType;
    }

    public Long getMedlId() {
        return medlId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MedlData medlData = (MedlData) o;
        return erMedlem == medlData.erMedlem && Objects.equals(beslutningsdato, medlData.beslutningsdato) && Objects.equals(lovvalgLand, medlData.lovvalgLand) && Objects.equals(studieLand, medlData.studieLand) && medlemskapType == medlData.medlemskapType && dekningType == medlData.dekningType && kildeType == medlData.kildeType && Objects.equals(medlId, medlData.medlId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beslutningsdato, erMedlem, lovvalgLand, studieLand, medlemskapType, dekningType, kildeType, medlId);
    }

    @Override
    public String toString() {
        return "MedlData{" +
            "beslutningsdato=" + beslutningsdato +
            ", erMedlem=" + erMedlem +
            ", lovvalgLand=" + lovvalgLand +
            ", studieLand=" + studieLand +
            ", medlemskapType=" + medlemskapType +
            ", dekningType=" + dekningType +
            ", kildeType=" + kildeType +
            ", medlId=" + medlId +
            '}';
    }
}
