package no.nav.ung.sak.behandlingslager.behandling.historikk;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.NaturalId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Entity(name = "Historikkinnslag")
@Table(name = "HISTORIKKINNSLAG")
public class Historikkinnslag extends BaseEntitet {

    public static final String BOLD_MARKØR = "__";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_HISTORIKKINNSLAG")
    private Long id;

    /**
     * UUID for å kunne identifisere historikkinnslaget unikt i frontend
     */
    @NaturalId
    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "fagsak_id", nullable = false)
    private Long fagsakId;

    @Column(name = "behandling_id")
    private Long behandlingId;

    @Convert(converter = HistorikkAktørKodeverdiConverter.class)
    @Column(name = "aktoer", nullable = false)
    private HistorikkAktør aktør;

    @Convert(converter = SkjermlenkeType.KodeverdiConverter.class)
    @Column(name = "skjermlenke")
    private SkjermlenkeType skjermlenke;

    @OneToMany(mappedBy = "historikkinnslag")
    private List<HistorikkinnslagLinje> linjer = new ArrayList<>();

    @OneToMany(mappedBy = "historikkinnslag")
    private List<HistorikkinnslagDokumentLink> dokumentLinker = new ArrayList<>();

    @Column(name = "tittel")
    private String tittel;

    protected Historikkinnslag() {
    }

    public UUID getUuid() {
        return uuid;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public HistorikkAktør getAktør() {
        return aktør;
    }

    public SkjermlenkeType getSkjermlenke() {
        return skjermlenke;
    }

    public List<HistorikkinnslagLinje> getLinjer() {
        return linjer;
    }

    public List<String> getTekstLinjer() {
        return linjer.stream().map(HistorikkinnslagLinje::getTekst).toList();
    }

    public List<HistorikkinnslagDokumentLink> getDokumentLinker() {
        return dokumentLinker;
    }

    public String getTittel() {
        return tittel;
    }

    @Override
    public String toString() {
        return "Historikkinnslag{" + "fagsakId=" + fagsakId + ", behandlingId=" + behandlingId + ", aktør=" + aktør + ", skjermlenkeType="
            + skjermlenke + ", linjer=" + linjer + ", tittel='" + tittel + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Historikkinnslag that)) {
            return false;
        }
        return Objects.equals(behandlingId, that.behandlingId) && Objects.equals(fagsakId, that.fagsakId) && Objects.equals(tittel, that.tittel)
            && Objects.equals(dokumentLinker, that.dokumentLinker) && Objects.equals(linjer, that.linjer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, fagsakId, tittel, dokumentLinker, linjer);
    }

    public static class Builder {

        private Historikkinnslag kladd = new Historikkinnslag();
        private List<HistorikkinnslagLinjeBuilder> internLinjer = new ArrayList<>();

        public Builder medFagsakId(Long fagsakId) {
            kladd.fagsakId = fagsakId;
            return this;
        }

        public Builder medBehandlingId(Long behandlingId) {
            kladd.behandlingId = behandlingId;
            return this;
        }

        public Builder medAktør(HistorikkAktør aktør) {
            kladd.aktør = aktør;
            return this;
        }

        public Builder medTittel(SkjermlenkeType skjermlenke) {
            kladd.skjermlenke = skjermlenke;
            return this;
        }

        public Builder medTittel(String tittel) {
            kladd.tittel = tittel;
            return this;
        }

        public Builder medLinjer(List<HistorikkinnslagLinjeBuilder> linjer) {
            internLinjer = new ArrayList<>(linjer.stream().filter(Objects::nonNull).toList()); // fraTilEquals kan legger til null objekter
            return this;
        }

        public Builder addLinje(HistorikkinnslagLinjeBuilder historikkinnslagLinjeBuilder) {
            if (historikkinnslagLinjeBuilder != null) {
                internLinjer.add(historikkinnslagLinjeBuilder);
            }
            return this;
        }

        public Builder addLinje(String tekst) {
            if (tekst != null) {
                internLinjer.add(new HistorikkinnslagLinjeBuilder().tekst(tekst));
            }
            return this;
        }

        public Builder medDokumenter(List<HistorikkinnslagDokumentLink> dokumenter) {
            kladd.dokumentLinker = dokumenter;
            for (var historikkinnslagDokumentLink : dokumenter) {
                historikkinnslagDokumentLink.setHistorikkinnslag(kladd);
            }
            return this;
        }

        public Historikkinnslag build() {
            Objects.requireNonNull(kladd.fagsakId);
            Objects.requireNonNull(kladd.aktør);
            if (kladd.tittel == null && kladd.skjermlenke == null) {
                throw new NullPointerException("Forventer å enten ha tittel eller skjermlenke");
            }
            if (bareLinjeskift(internLinjer)) {
                throw new IllegalStateException("Historikkinnslag inneholder bare linjeskift");
            }
            fjernLeadingOgTrailingLinjeskift();

            for (var i = 0; i < internLinjer.size(); i++) {
                var type = internLinjer.get(i).getType();
                var linje = type == HistorikkinnslagLinjeType.TEKST ? HistorikkinnslagLinje.tekst(sluttMedPunktum(internLinjer.get(i).tilTekst()),
                    i) : HistorikkinnslagLinje.linjeskift(i);
                kladd.linjer.add(linje);
                linje.setHistorikkinnslag(kladd);
            }

            kladd.uuid = UUID.randomUUID();

            var t = kladd;
            kladd = null;
            return t;
        }

        private static boolean bareLinjeskift(List<HistorikkinnslagLinjeBuilder> linjer) {
            return !linjer.isEmpty() && linjer.stream().allMatch(l -> l.getType() == HistorikkinnslagLinjeType.LINJESKIFT);
        }

        private void fjernLeadingOgTrailingLinjeskift() {
            if (!internLinjer.isEmpty()) {
                if (internLinjer.getFirst().getType() == HistorikkinnslagLinjeType.LINJESKIFT) {
                    internLinjer.removeFirst();
                }
                if (internLinjer.getLast().getType() == HistorikkinnslagLinjeType.LINJESKIFT) {
                    internLinjer.removeLast();
                }
            }
        }

        private String sluttMedPunktum(String tekst) {
            if (tekst.isEmpty()) {
                return tekst;
            }
            var sisteTegn = finnSisteTegn(tekst);
            return List.of(':', '.', '?', '!').contains(sisteTegn) ? tekst : tekst + ".";
        }

        private static char finnSisteTegn(String tekst) {
            if (tekst.endsWith(BOLD_MARKØR)) {
                return tekst.charAt(tekst.length() - 1 - BOLD_MARKØR.length());
            }
            return tekst.charAt(tekst.length() - 1);
        }
    }
}
