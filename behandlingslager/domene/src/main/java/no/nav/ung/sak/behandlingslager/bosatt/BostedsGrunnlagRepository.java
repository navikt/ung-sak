package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
     * Lagrer saksbehandlers foreslåtte bostedsavklaringer for en behandling (én per skjæringstidspunkt).
     * Fastsatt holder settes alltid til null – bruk {@link #fastsettForeslåtteAvklaringer} for å fastsette.
     *
     * @return Grunnlagsreferansen (ny ved endring, eksisterende ved uendret foreslått-holder)
     */
    public UUID lagreAvklaringer(Long behandlingId, Map<LocalDate, Boolean> avklaringerPerSkjæringstidspunkt) {
        var eksisterende = hentGrunnlagHvisEksisterer(behandlingId);

        var nyForeslåttHolder = new BostedsAvklaringHolder();
        for (var entry : avklaringerPerSkjæringstidspunkt.entrySet()) {
            nyForeslåttHolder.leggTilAvklaring(new BostedsAvklaring(entry.getKey(), entry.getValue()));
        }

        if (eksisterende.isPresent() && eksisterende.get().getForeslåttHolder().equals(nyForeslåttHolder)) {
            return eksisterende.get().getGrunnlagsreferanse();
        }

        var nyttGrunnlag = new BostedsGrunnlag(behandlingId, nyForeslåttHolder, null, UUID.randomUUID());
        eksisterende.ifPresent(this::deaktiverEksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
        return nyttGrunnlag.getGrunnlagsreferanse();
    }

    /**
     * Fastsetter avklaringer direkte for angitte skjæringstidspunkter med gitte verdier.
     * Beholder foreslåttHolder og grunnlagsreferanse uendret.
     * Brukes av FASTSETT_BOSTED-aksjonspunktet der saksbehandler kan overstyre foreslått verdi.
     */
    public void fastsettAvklaringerDirekte(Long behandlingId, Map<LocalDate, Boolean> avklaringer) {
        var eksisterende = hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer bostedsgrunnlag ved fastsetting, behandlingId=" + behandlingId));

        var nyFastsattHolder = new BostedsAvklaringHolder();
        avklaringer.forEach((fomDato, erBosattITrondheim) ->
            nyFastsattHolder.leggTilAvklaring(new BostedsAvklaring(fomDato, erBosattITrondheim)));

        var nyttGrunnlag = new BostedsGrunnlag(behandlingId, eksisterende.getForeslåttHolder(), nyFastsattHolder, eksisterende.getGrunnlagsreferanse());
        deaktiverEksisterende(eksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    /**
     * Fastsetter foreslåtte avklaringer for angitte perioder.
     * Kopierer alle avklaringer med fomDato innenfor en av de angitte periodene fra foreslåttHolder til en ny fastsattHolder.
     * Grunnlagsreferansen beholdes slik at eksisterende etterlysningslenker er intakte.
     */
    public void fastsettForeslåtteAvklaringer(Long behandlingId, Set<DatoIntervallEntitet> perioder) {
        var eksisterende = hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer bostedsgrunnlag ved fastsetting, behandlingId=" + behandlingId));

        var nyFastsattHolder = byggFastsattHolder(eksisterende, perioder);
        var nyttGrunnlag = new BostedsGrunnlag(behandlingId, eksisterende.getForeslåttHolder(), nyFastsattHolder, eksisterende.getGrunnlagsreferanse());
        deaktiverEksisterende(eksisterende);
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private BostedsAvklaringHolder byggFastsattHolder(BostedsGrunnlag eksisterende, Set<DatoIntervallEntitet> perioder) {
        var eksisterendeFastsatt = eksisterende.getFastsattHolder();
        var nyHolder = eksisterendeFastsatt != null ? new BostedsAvklaringHolder(eksisterendeFastsatt) : new BostedsAvklaringHolder();

        eksisterende.getForeslåttHolder().getAvklaringer().stream()
            .filter(a -> perioder.stream().anyMatch(p ->
                !a.getFomDato().isBefore(p.getFomDato()) && !a.getFomDato().isAfter(p.getTomDato())))
            .map(a -> new BostedsAvklaring(a.getFomDato(), a.erBosattITrondheim()))
            .forEach(nyHolder::leggTilAvklaring);

        return nyHolder;
    }

    /**
     * Kopierer grunnlag fra en eksisterende behandling til en ny behandling.
     * Begge holders (foreslått og fastsatt) refereres – ingen kopiering av data.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        hentGrunnlagHvisEksisterer(gammelBehandlingId).ifPresent(eksisterende -> {
            var nyttGrunnlag = new BostedsGrunnlag(nyBehandlingId, eksisterende.getForeslåttHolder(), eksisterende.getFastsattHolder(), eksisterende.getGrunnlagsreferanse());
            entityManager.persist(nyttGrunnlag);
            entityManager.flush();
        });
    }

    private void deaktiverEksisterende(BostedsGrunnlag gr) {
        gr.deaktiver();
        entityManager.persist(gr);
        entityManager.flush();
    }
}
