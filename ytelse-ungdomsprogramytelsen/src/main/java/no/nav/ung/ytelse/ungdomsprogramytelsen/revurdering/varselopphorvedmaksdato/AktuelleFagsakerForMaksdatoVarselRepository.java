package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering.varselopphorvedmaksdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.MaksdatoOpphørVarslingPeriode;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository som henter ut UNGDOMSYTELSE-fagsaker der
 * {@link MaksdatoOpphørVarslingPeriode#erRelevantForVarsling} er true
 * for den siste opprettede ytelsesbehandlingen.
 */
@Dependent
public class AktuelleFagsakerForMaksdatoVarselRepository {

    private EntityManager entityManager;

    AktuelleFagsakerForMaksdatoVarselRepository() {
        // CDI proxy
    }

    @Inject
    public AktuelleFagsakerForMaksdatoVarselRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Henter UNGDOMSYTELSE-fagsaker der {@link MaksdatoOpphørVarslingPeriode#erRelevantForVarsling}
     * er true for siste opprettede ytelsesbehandling (BT-002 eller BT-004).
     *
     * <p>Betingelsene tilsvarer {@link MaksdatoOpphørVarslingPeriode#erRelevantForVarsling}:
     * <ol>
     *   <li>Siste ytelsesbehandling har et aktivt grunnlag med {@code periodeMaksDato} satt</li>
     *   <li>{@code harPassertVarseldato}: {@code periodeMaksDato} er innenfor
     *       {@value MaksdatoOpphørVarslingPeriode#VARSEL_UKER_FØR_MAKSDATO} uker fra i dag</li>
     *   <li>{@code opphørErIkkeSattTidligereEnnMaksdato}: programmets maks sluttdato er ikke
     *       satt tidligere enn {@code periodeMaksDato}</li>
     * </ol>
     *
     * I tillegg sjekkes det på om fagsaken har en tidligere behandling med prosesstrigger for RE_VARSEL_OPPHOR_VED_MAKSDATO med periode som overlapper gjeldende maksdato.
     * Dersom det finnes en slik behandling er det sendt varsel tidligere og vi skal ikke sende på nytt.
     *
     * Det ekskluderes også fagsaker som allerede har en åpen (ikke avsluttet/iverksatt) behandling med årsak RE_VARSEL_OPPHOR_VED_MAKSDATO,
     * slik at vi ikke oppretter duplikate behandlinger mens en varselbehandling er under arbeid.
     *
     */
    @SuppressWarnings("unchecked")
    public List<Fagsak> hentFagsakerRelevantForMaksdatoVarsel() {
        // harPassertVarseldato: periodeMaksDato <= now + VARSEL_UKER_FØR_MAKSDATO uker
        LocalDate grensedato = LocalDate.now().plusWeeks(MaksdatoOpphørVarslingPeriode.VARSEL_UKER_FØR_MAKSDATO);
        String ungdomsytelseKode = FagsakYtelseType.UNGDOMSYTELSE.getKode();
        String varselOpphorKode = BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO.getKode();

        // BT-002 = FØRSTEGANGSSØKNAD, BT-004 = REVURDERING (BehandlingType.getYtelseBehandlingTyper())
        String sql = """
                select f.* from fagsak f
                where f.ytelse_type = :ytelseType
                  and exists (
                    select 1
                    from behandling b
                    inner join ung_gr_ungdomsprogramperiode gr
                        on gr.behandling_id = b.id and gr.aktiv = true
                    inner join ung_ungdomsprogram_maks_periode mp
                        on mp.id = gr.ung_ungdomsprogram_maks_periode_id
                    inner join ung_ungdomsprogramperioder perioder
                        on perioder.id = gr.ung_ungdomsprogramperioder_id
                    inner join ung_ungdomsprogramperiode p
                        on p.ung_ungdomsprogramperioder_id = perioder.id
                    where b.fagsak_id = f.id
                      and b.behandling_type in ('BT-002', 'BT-004')
                      and b.id = (
                          select b2.id from behandling b2
                          where b2.fagsak_id = f.id
                            and b2.behandling_type in ('BT-002', 'BT-004')
                          order by b2.opprettet_tid desc
                          limit 1
                      )
                      and mp.periode_maks_dato is not null
                      and mp.periode_maks_dato <= cast(:grensedato as date)
                      and not exists (
                          select 1
                          from behandling b3
                          inner join prosess_triggere ptg
                              on ptg.behandling_id = b3.id and ptg.aktiv = true
                          inner join pt_trigger pt
                              on pt.triggere_id = ptg.triggere_id
                          where b3.fagsak_id = f.id
                            and pt.arsak = :varselOpphorArsak
                            and pt.periode && daterange(cast(mp.periode_maks_dato as date), cast(mp.periode_maks_dato as date), '[]')
                      )
                      and not exists (
                          select 1
                          from behandling b4
                          inner join behandling_arsak ba
                              on ba.behandling_id = b4.id
                          where b4.fagsak_id = f.id
                            and ba.behandling_arsak_type = :varselOpphorArsak
                            and b4.behandling_status not in ('AVSLU', 'IVED')
                      )
                    group by mp.periode_maks_dato
                    having max(p.tom) >= mp.periode_maks_dato
                  )
                """;

        Query query = entityManager.createNativeQuery(sql, Fagsak.class); // NOSONAR
        query.setParameter("grensedato", grensedato);
        query.setParameter("ytelseType", ungdomsytelseKode);
        query.setParameter("varselOpphorArsak", varselOpphorKode);
        return query.getResultList();
    }
}

