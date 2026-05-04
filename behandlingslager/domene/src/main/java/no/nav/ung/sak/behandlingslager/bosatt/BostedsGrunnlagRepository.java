package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

import no.nav.ung.kodeverk.bosatt.Kilde;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Dependent
public class BostedsGrunnlagRepository {

    private final EntityManager entityManager;

    @Inject
    public BostedsGrunnlagRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<BostedsGrunnlag> hentGrunnlagHvisEksisterer(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT g FROM BostedsGrunnlag g " +
                "WHERE g.behandlingId = :behandlingId AND g.aktiv = true",
            BostedsGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Lagrer saksbehandlers bostedsavklaringer for en behandling (kilde=SAKSBEHANDLER).
     * Nøkkel er skjæringstidspunkt (= vilkårsperiode fom); verdi er {@link BostedAvklaringData}.
     *
     * @return Map fra skjæringstidspunkt til periodeAvklaring.referanse
     */
    public Map<LocalDate, UUID> lagreAvklaringer(Long behandlingId,
                                                  Map<LocalDate, BostedAvklaringData> avklaringerPerSkjæringstidspunkt) {
        var eksisterende = hentGrunnlagHvisEksisterer(behandlingId);

        var nyHolder = byggHolder(avklaringerPerSkjæringstidspunkt);

        if (eksisterende.isPresent() && eksisterende.get().getHolder().equals(nyHolder)) {
            return eksisterende.get().getHolder().getPeriodeAvklaringer().stream()
                .collect(Collectors.toMap(
                    BostedsPeriodeAvklaring::getSkjæringstidspunkt,
                    BostedsPeriodeAvklaring::getReferanse));
        }

        var nyttGrunnlag = new BostedsGrunnlag(behandlingId, nyHolder);
        eksisterende.ifPresent(this::deaktiverEksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();

        return nyHolder.getPeriodeAvklaringer().stream()
            .collect(Collectors.toMap(
                BostedsPeriodeAvklaring::getSkjæringstidspunkt,
                BostedsPeriodeAvklaring::getReferanse));
    }

    /**
     * Lagrer automatisk fastsatte avklaringer basert på søknadsdata (kilde=SØKNAD).
     * Avklaringene har erBosattITrondheim fra søknaden, fraflyttingsDato=null, fraflyttingsÅrsak=null.
     *
     * @return Map fra skjæringstidspunkt til periodeAvklaring.referanse
     */
    public Map<LocalDate, UUID> lagreAvklaringerFraSøknad(Long behandlingId,
                                                            Map<LocalDate, Boolean> søknadErBosattPerFom) {
        var eksisterende = hentGrunnlagHvisEksisterer(behandlingId);

        var holder = new BostedsAvklaringHolder();
        for (var entry : søknadErBosattPerFom.entrySet()) {
            holder.leggTilPeriodeAvklaring(new BostedsPeriodeAvklaring(
                entry.getKey(), entry.getValue(), null, null, Kilde.SØKNAD));
        }

        if (eksisterende.isPresent() && eksisterende.get().getHolder().equals(holder)) {
            return eksisterende.get().getHolder().getPeriodeAvklaringer().stream()
                .collect(Collectors.toMap(
                    BostedsPeriodeAvklaring::getSkjæringstidspunkt,
                    BostedsPeriodeAvklaring::getReferanse));
        }

        var nyttGrunnlag = new BostedsGrunnlag(behandlingId, holder);
        eksisterende.ifPresent(this::deaktiverEksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();

        return holder.getPeriodeAvklaringer().stream()
            .collect(Collectors.toMap(
                BostedsPeriodeAvklaring::getSkjæringstidspunkt,
                BostedsPeriodeAvklaring::getReferanse));
    }

    /**
     * Kopierer grunnlag fra en eksisterende behandling til en ny behandling.
     * Holder refereres — ingen kopiering av data.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        hentGrunnlagHvisEksisterer(gammelBehandlingId).ifPresent(eksisterende -> {
            var nyttGrunnlag = new BostedsGrunnlag(nyBehandlingId, eksisterende.getHolder());
            entityManager.persist(nyttGrunnlag);
            entityManager.flush();
        });
    }

    public Optional<BostedsPeriodeAvklaring> hentPeriodeAvklaringFraReferanse(UUID referanse) {
        var query = entityManager.createQuery(
            "SELECT p FROM BostedsPeriodeAvklaring p WHERE p.referanse = :referanse",
            BostedsPeriodeAvklaring.class);
        query.setParameter("referanse", referanse);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private static BostedsAvklaringHolder byggHolder(Map<LocalDate, BostedAvklaringData> avklaringerPerSkjæringstidspunkt) {
        var holder = new BostedsAvklaringHolder();
        for (var entry : avklaringerPerSkjæringstidspunkt.entrySet()) {
            var skjæringstidspunkt = entry.getKey();
            var data = entry.getValue();
            holder.leggTilPeriodeAvklaring(new BostedsPeriodeAvklaring(
                skjæringstidspunkt, data.erBosattITrondheim(), data.fraflyttingsDato(), data.fraflyttingsÅrsak(), data.kilde()));
        }
        return holder;
    }

    private void deaktiverEksisterende(BostedsGrunnlag gr) {
        gr.deaktiver();
        entityManager.persist(gr);
        entityManager.flush();
    }
}
