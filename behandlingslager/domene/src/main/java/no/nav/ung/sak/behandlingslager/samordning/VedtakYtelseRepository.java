package no.nav.ung.sak.behandlingslager.samordning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Lagrer vedtak til samordning.
 * <p>
 * Lagringen er uavhengig av rekkefølge og idempotent: samme vedtaksreferanse erstatter raden,
 * og hvilket vedtak som er aktivt for saken beregnes på nytt ved hver lagring.
 * Eldre vedtak lagres som inaktive og slettes aldri.
 */
@Dependent
public class VedtakYtelseRepository {

    // Siste vedtatte er gjeldende. Ved likt vedtakstidspunkt (i praksis umulig innenfor én sak) vinner sist lagrede.
    private static final Comparator<VedtakYtelse> GJELDENDE_SIST = Comparator
        .comparing(VedtakYtelse::getVedtattTidspunkt)
        .thenComparing(VedtakYtelse::getId);

    private EntityManager entityManager;

    VedtakYtelseRepository() {
        // for CDI proxy
    }

    @Inject
    public VedtakYtelseRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void lagre(VedtakYtelse vedtak) {
        Objects.requireNonNull(vedtak, "vedtak");
        hentForVedtakReferanse(vedtak.getVedtakReferanse()).ifPresent(eksisterende -> {
            if (!eksisterende.getSaksnummer().equals(vedtak.getSaksnummer()) || eksisterende.getYtelseType() != vedtak.getYtelseType()) {
                throw new IllegalStateException("Vedtaksreferanse " + vedtak.getVedtakReferanse() + " er allerede lagret for en annen sak eller ytelse");
            }
            entityManager.remove(eksisterende);
            // Hibernate kjører insert før delete i samme flush, og referansen er unik.
            entityManager.flush();
        });
        entityManager.persist(vedtak);
        entityManager.flush();
        oppdaterAktivtVedtak(vedtak.getSaksnummer(), vedtak.getYtelseType());
    }

    public Optional<VedtakYtelse> hentForVedtakReferanse(UUID vedtakReferanse) {
        return entityManager.createQuery("from VedtakYtelse where vedtakReferanse = :vedtakReferanse", VedtakYtelse.class)
            .setParameter("vedtakReferanse", vedtakReferanse)
            .getResultStream()
            .findFirst();
    }

    public Optional<VedtakYtelse> hentAktivtVedtak(Saksnummer saksnummer, FagsakYtelseType ytelseType) {
        return hentAlleVedtak(saksnummer, ytelseType).stream()
            .filter(VedtakYtelse::erAktiv)
            .findFirst();
    }

    /**
     * @return alle lagrede vedtak for saken, både aktive og inaktive
     */
    public List<VedtakYtelse> hentAlleVedtak(Saksnummer saksnummer, FagsakYtelseType ytelseType) {
        return entityManager.createQuery("from VedtakYtelse where saksnummer = :saksnummer and ytelseType = :ytelseType", VedtakYtelse.class)
            .setParameter("saksnummer", saksnummer)
            .setParameter("ytelseType", ytelseType)
            .getResultList();
    }

    /**
     * @return aktive vedtak for aktørene der vedtaksperioden overlapper [fom, tom]. Inkluderer vedtak uten innvilget periode.
     */
    @SuppressWarnings("unchecked")
    public List<VedtakYtelse> hentAktiveVedtakSomOverlapper(Collection<AktørId> aktørIder,
                                                            Collection<FagsakYtelseType> ytelseTyper,
                                                            LocalDate fom,
                                                            LocalDate tom) {
        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");
        if (aktørIder.isEmpty() || ytelseTyper.isEmpty()) {
            return List.of();
        }
        String sql = """
            select v.* from vedtak_ytelse v
             where v.aktiv = true
               and v.aktoer_id in (:aktoerIder)
               and v.ytelse_type in (:ytelseTyper)
               and v.periode && daterange(cast(:fom as date), cast(:tom as date), '[]')
            """;
        return entityManager.createNativeQuery(sql, VedtakYtelse.class)
            .setParameter("aktoerIder", aktørIder.stream().map(AktørId::getId).toList())
            .setParameter("ytelseTyper", ytelseTyper.stream().map(FagsakYtelseType::getKode).toList())
            .setParameter("fom", fom)
            .setParameter("tom", tom)
            .getResultList();
    }

    private void oppdaterAktivtVedtak(Saksnummer saksnummer, FagsakYtelseType ytelseType) {
        var alle = hentAlleVedtak(saksnummer, ytelseType);
        var gjeldende = alle.stream().max(GJELDENDE_SIST).orElseThrow();
        // Deaktiverer før aktivering, slik at den partielle unike indeksen på aktivt vedtak ikke brytes underveis.
        alle.stream()
            .filter(v -> v != gjeldende && v.erAktiv())
            .forEach(v -> v.setAktiv(false));
        entityManager.flush();
        if (!gjeldende.erAktiv()) {
            gjeldende.setAktiv(true);
            entityManager.flush();
        }
    }
}
