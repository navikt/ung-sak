package no.nav.ung.ytelse.aktivitetspenger.mottak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.medlemskap.Medlemskap;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapPeriode;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittUtenlandsopphold;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class AktivitetspengerSøknadPersisterer {

    private final SøknadRepository søknadRepository;
    private final FagsakRepository fagsakRepository;
    private final OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;
    private final StartdatoRepository startdatoRepository;
    private final BostedsGrunnlagRepository bostedsGrunnlagRepository;


    @Inject
    public AktivitetspengerSøknadPersisterer(BehandlingRepositoryProvider repositoryProvider,
                                             FagsakRepository fagsakRepository,
                                             OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository,
                                             BostedsGrunnlagRepository bostedsGrunnlagRepository,
                                             StartdatoRepository startdatoRepository) {
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.fagsakRepository = fagsakRepository;
        this.forutgåendeMedlemskapRepository = forutgåendeMedlemskapRepository;
        this.startdatoRepository = startdatoRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
    }


    public void lagreSøknadEntitet(Søknad søknad, JournalpostId journalpostId, Long behandlingId, LocalDate startdato, LocalDate mottattDato) {
        var søknadBuilder = new SøknadEntitet.Builder()
            .medElektroniskRegistrert(true)
            .medMottattDato(mottattDato)
            .medJournalpostId(journalpostId)
            .medSøknadId(søknad.getSøknadId() == null ? null : søknad.getSøknadId().getId())
            .medStartdato(startdato)
            .medSpråkkode(getSpraakValg(søknad.getSpråk()));
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);
    }

    public void lagreVirkningsdato(LocalDate virkningsdato, JournalpostId journalpostId, Long behandlingId, Boolean erBosattITrondheim) {
        startdatoRepository.lagre(behandlingId, List.of(new SøktStartdato(virkningsdato, journalpostId)));
        if (erBosattITrondheim != null) {
            bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandlingId, journalpostId.getVerdi(), virkningsdato, erBosattITrondheim);
        }
    }

    public void oppdaterFagsakperiode(Periode utvidelsesperiode, Behandling behandling) {
        fagsakRepository.utvidPeriode(behandling.getFagsakId(), utvidelsesperiode.getFom(), utvidelsesperiode.getTom());
    }

    private Språkkode getSpraakValg(Språk spraak) {
        if (spraak != null) {
            return Språkkode.fraKode(spraak.getKode().toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }

    public void lagreMedlemskapGrunnlag(Medlemskap medlemskap, LocalDate virkningstidspunkt, JournalpostId journalpostId, Long behandlingId) {
        LocalDate forutgåendeFom = virkningstidspunkt.minusYears(5);
        LocalDate forutgåendeTom = virkningstidspunkt.minusDays(1);

        Set<OppgittUtenlandsopphold> oppgitteUtenlandsopphold = medlemskap.utenlandsopphold().perioder().entrySet().stream()
            .map(entry -> new OppgittUtenlandsopphold(
                entry.getKey().getFraOgMed(),
                entry.getKey().getTilOgMed(),
                entry.getValue().land().getLandkode()))
            .collect(Collectors.toSet());

        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandlingId, OppgittForutgåendeMedlemskapPeriode.builder()
            .medFom(forutgåendeFom)
            .medTom(forutgåendeTom)
            .medJournalpostId(journalpostId)
            .medHarBoddINorge(medlemskap.harBoddINorge())
            .medHarJobbetINorge(medlemskap.harJobbetINorge())
            .medHarJobbetUtenforNorge(medlemskap.harJobbetUtenforNorge())
            .medUtenlandsopphold(oppgitteUtenlandsopphold)
            .build());
    }
}
