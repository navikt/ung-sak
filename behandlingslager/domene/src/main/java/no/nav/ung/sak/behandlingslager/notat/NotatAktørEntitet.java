package no.nav.ung.sak.behandlingslager.notat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Where;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.kodeverk.FagsakYtelseTypeKodeverdiConverter;
import no.nav.ung.sak.typer.AktørId;


/**
 * Notater som gjelder en person uavhengig av fagsak. F.eks. pleietrengende som deles på
 * flere fagsaker.
 */
@Entity(name = "NotatAktørEntitet")
@Table(name = "notat_aktoer")
public class NotatAktørEntitet extends NotatEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_notat_aktoer")
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "notat_id", nullable = false)
    @Where(clause = "aktiv = true")
    private List<NotatAktørTekst> notatAktørTekst;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", nullable = false, updatable = false)))
    private AktørId aktørId;

    @Convert(converter = FagsakYtelseTypeKodeverdiConverter.class)
    @Column(name = "ytelse_type", nullable = false, updatable = false)
    private FagsakYtelseType ytelseType;

    NotatAktørEntitet(AktørId aktørId, FagsakYtelseType ytelseType, String notatTekst, boolean skjult) {
        super(skjult);
        this.notatAktørTekst = new ArrayList<>();
        this.notatAktørTekst.add(new NotatAktørTekst(notatTekst, 0));
        this.aktørId = aktørId;
        this.ytelseType = ytelseType;
    }

    NotatAktørEntitet() { }

    public AktørId getAktørId() {
        return aktørId;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    @Override
    public void nyTekst(String tekst) {
        var aktørTekst = this.finnNotatTekst();
        aktørTekst.deaktiver();
        this.notatAktørTekst.add(new NotatAktørTekst(tekst, aktørTekst.getVersjon() + 1));
    }


    @Override
    List<NotatAktørTekst> getNotatTekstEntiteter() {
        return notatAktørTekst;
    }
}
