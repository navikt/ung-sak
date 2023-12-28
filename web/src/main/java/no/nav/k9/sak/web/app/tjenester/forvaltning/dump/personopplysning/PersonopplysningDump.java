package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.personopplysning;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef
public class PersonopplysningDump implements DebugDumpFagsak {

    private EntityManager entityManager;

    PersonopplysningDump() {
        //
    }

    @Inject
    public PersonopplysningDump(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        String sql = "select "
            + "  b.id as behandling_id, "
            + "  preg.id as preg_id, "
            + "  preg.aktoer_id as preg_aktoer_id, "
            + "  preg.navn as preg_navn, "
            + "  preg.foedselsdato as preg_foedselsdato, "
            + "  preg.doedsdato as preg_doedsdato, "
            + "  preg.bruker_kjoenn as preg_bruker_kjoenn, "
            + "  preg.sivilstand_type as preg_sivilstand_type, "
            + "  preg.region as preg_region, "
            + "  pov.id as pov_id, "
            + "  pov.aktoer_id as pov_aktoer_id, "
            + "  pov.navn as pov_navn, "
            + "  pov.foedselsdato as pov_foedselsdato, "
            + "  pov.doedsdato as pov_doedsdato, "
            + "  pov.bruker_kjoenn as pov_bruker_kjoenn, "
            + "  pov.sivilstand_type as pov_sivilstand_type, "
            + "  pov.region as pov_region, "
            + "  replace(cast(pov.opprettet_tid as varchar), ' ', 'T') as pov_opprettet_tid "
            + " from  "
            + "  gr_personopplysning gr  "
            + "  inner join behandling b on b.id=gr.behandling_id "
            + "  inner join fagsak f on f.id=b.fagsak_id "
            + "  left outer join PO_PERSONOPPLYSNING preg on preg.po_informasjon_id=gr.registrert_informasjon_id "
            + "  left outer join PO_PERSONOPPLYSNING pov on pov.po_informasjon_id=gr.overstyrt_informasjon_id "
            + " where gr.aktiv = true "
            + "   and f.saksnummer=:saksnummer";

        Query query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("saksnummer", dumpMottaker.getFagsak().getSaksnummer().getVerdi());

        @SuppressWarnings("unchecked")
        List<Tuple> results = query.getResultList();

        Optional<String> output = CsvOutput.dumpResultSetToCsv(results);
        if (output.isPresent()) {
            dumpMottaker.newFile("personopplysning.csv");
            dumpMottaker.write(output.get());
        }
    }
}
