package no.nav.ung.sak.vedtattytelse;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.kodeverk.FagsakYtelseTypeKodeverdiConverter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.PostgreSQLRangeType;
import no.nav.ung.sak.domene.typer.tid.Range;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Iverksatt vedtak om ungdomsprogramytelse eller aktivitetspenger, som andre systemer i Nav kan hente, f.eks. til samordning.
 * <p>
 * Følger vedtak_ytelse-modellen i abakus. Et vedtak uten anvisninger betyr at ingenting er innvilget
 * (fullt avslag eller opphør fra start). Det lagres likevel, slik at et tidligere innvilget vedtak deaktiveres.
 */
@Entity(name = "VedtakYtelse")
@Table(name = "vedtak_ytelse")
public class VedtakYtelse extends BaseEntitet {

    @Id
    @SequenceGenerator(name = "seq_vedtak_ytelse", sequenceName = "seq_vedtak_ytelse")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_vedtak_ytelse")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", nullable = false, updatable = false)))
    private AktørId aktørId;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "saksnummer", column = @Column(name = "saksnummer", nullable = false, updatable = false)))
    private Saksnummer saksnummer;

    @Convert(converter = FagsakYtelseTypeKodeverdiConverter.class)
    @Column(name = "ytelse_type", nullable = false, updatable = false)
    private FagsakYtelseType ytelseType;

    @Column(name = "vedtak_referanse", nullable = false, updatable = false)
    private UUID vedtakReferanse;

    @Column(name = "vedtatt_tidspunkt", nullable = false, updatable = false)
    private LocalDateTime vedtattTidspunkt;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange", nullable = false, updatable = false)
    private Range<LocalDate> periode;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = false;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToMany(mappedBy = "vedtakYtelse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<YtelseAnvist> anvisninger = new ArrayList<>();

    VedtakYtelse() {
        // for hibernate
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    public UUID getVedtakReferanse() {
        return vedtakReferanse;
    }

    public LocalDateTime getVedtattTidspunkt() {
        return vedtattTidspunkt;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public boolean erAktiv() {
        return aktiv;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    /**
     * @return innvilgede delperioder sortert på fom. Tom liste betyr at ingenting er innvilget.
     */
    public List<YtelseAnvist> getAnvisninger() {
        return anvisninger.stream()
            .sorted(Comparator.comparing(a -> a.getPeriode().getFomDato()))
            .toList();
    }

    public boolean harInnvilgetPeriode() {
        return !anvisninger.isEmpty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<id=" + id
            + ", ytelseType=" + ytelseType
            + ", vedtakReferanse=" + vedtakReferanse
            + ", vedtattTidspunkt=" + vedtattTidspunkt
            + ", periode=" + getPeriode()
            + ", aktiv=" + aktiv
            + ">";
    }

    public static class Builder {
        private final VedtakYtelse kladd = new VedtakYtelse();

        private Builder() {
        }

        public Builder medAktørId(AktørId aktørId) {
            kladd.aktørId = aktørId;
            return this;
        }

        public Builder medSaksnummer(Saksnummer saksnummer) {
            kladd.saksnummer = saksnummer;
            return this;
        }

        public Builder medYtelseType(FagsakYtelseType ytelseType) {
            kladd.ytelseType = ytelseType;
            return this;
        }

        public Builder medVedtakReferanse(UUID vedtakReferanse) {
            kladd.vedtakReferanse = vedtakReferanse;
            return this;
        }

        public Builder medVedtattTidspunkt(LocalDateTime vedtattTidspunkt) {
            kladd.vedtattTidspunkt = vedtattTidspunkt;
            return this;
        }

        public Builder medPeriode(DatoIntervallEntitet periode) {
            kladd.periode = Objects.requireNonNull(periode, "periode").toRange();
            return this;
        }

        public Builder leggTilAnvisning(YtelseAnvist anvisning) {
            Objects.requireNonNull(anvisning, "anvisning");
            anvisning.setVedtakYtelse(kladd);
            kladd.anvisninger.add(anvisning);
            return this;
        }

        public VedtakYtelse build() {
            Objects.requireNonNull(kladd.aktørId, "aktørId");
            Objects.requireNonNull(kladd.saksnummer, "saksnummer");
            Objects.requireNonNull(kladd.ytelseType, "ytelseType");
            Objects.requireNonNull(kladd.vedtakReferanse, "vedtakReferanse");
            Objects.requireNonNull(kladd.vedtattTidspunkt, "vedtattTidspunkt");
            Objects.requireNonNull(kladd.periode, "periode");
            validerAnvisninger();
            return kladd;
        }

        private void validerAnvisninger() {
            var vedtaksperiode = kladd.getPeriode();
            var sortert = kladd.getAnvisninger();
            for (int i = 0; i < sortert.size(); i++) {
                var periode = sortert.get(i).getPeriode();
                if (periode.getFomDato().isBefore(vedtaksperiode.getFomDato()) || periode.getTomDato().isAfter(vedtaksperiode.getTomDato())) {
                    throw new IllegalStateException("Anvisning " + periode + " ligger utenfor vedtaksperioden " + vedtaksperiode);
                }
                if (i > 0 && !periode.getFomDato().isAfter(sortert.get(i - 1).getPeriode().getTomDato())) {
                    throw new IllegalStateException("Anvisninger overlapper: " + sortert.get(i - 1).getPeriode() + " og " + periode);
                }
            }
        }
    }
}
