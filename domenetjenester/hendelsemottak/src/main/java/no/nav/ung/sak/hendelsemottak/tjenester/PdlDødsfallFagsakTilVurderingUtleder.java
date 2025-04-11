package no.nav.ung.sak.hendelsemottak.tjenester;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.*;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
@HendelseTypeRef("PDL_DØDSFALL")
public class PdlDødsfallFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {


    private static final Logger logger = LoggerFactory.getLogger(PdlDødsfallFagsakTilVurderingUtleder.class);
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private PdlKlient pdlKlient;

    public PdlDødsfallFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public PdlDødsfallFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
                                                UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste,
                                                PersonopplysningRepository personopplysningRepository,
                                                PdlKlient pdlKlient) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.finnFagsakerForAktørTjeneste = finnFagsakerForAktørTjeneste;
        this.personopplysningRepository = personopplysningRepository;
        this.pdlKlient = pdlKlient;
    }


    @Override
    public Map<Fagsak, ÅrsakOgPeriode> finnFagsakerTilVurdering(Hendelse hendelse) {
        List<AktørId> aktører = hendelse.getHendelseInfo().getAktørIder();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();
        var fagsakÅrsakMap = new HashMap<Fagsak, ÅrsakOgPeriode>();

        for (AktørId aktør : aktører) {

            var personInfo = hentPersonInformasjon(aktør);
            var aktuellDato = finnAktuellDato(personInfo);

            fagsakÅrsakMap.putAll(finnPåvirketFagsak(aktør, aktuellDato, hendelseId));
        }

        return fagsakÅrsakMap;
    }

    private HashMap<Fagsak, ÅrsakOgPeriode> finnPåvirketFagsak(AktørId aktør, LocalDate aktuellDato, String hendelseId) {
        var fagsakÅrsakMap = new HashMap<Fagsak, ÅrsakOgPeriode>();

        // Sjekker om det gjelder dødshendelse for søker
        var fagsakForAktør = finnFagsakerForAktørTjeneste.hentRelevantFagsakForAktørSomSøker(aktør, aktuellDato);
        if (fagsakForAktør.isPresent()) {
            if (deltarIProgramPåHendelsedato(fagsakForAktør.get(), aktuellDato, hendelseId) && erNyInformasjonIHendelsen(fagsakForAktør.get(), aktør, aktuellDato, hendelseId)) {
                fagsakÅrsakMap.put(fagsakForAktør.get(), new ÅrsakOgPeriode(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER, DatoIntervallEntitet.fraOgMedTilOgMed(aktuellDato, fagsakForAktør.get().getPeriode().getTomDato())));
            }
        }

        // Sjekker om det gjelder dødshendelse for barn av søker
        finnFagsakerForAktørTjeneste.hentRelevantFagsakForAktørSomBarnAvSøker(aktør, aktuellDato)
            .stream()
            .filter(f -> deltarIProgramPåHendelsedato(f, aktuellDato, hendelseId))
            .filter(f -> erNyInformasjonIHendelsen(f, aktør, aktuellDato, hendelseId))
            .forEach(f -> fagsakÅrsakMap.put(f, new ÅrsakOgPeriode(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN, DatoIntervallEntitet.fraOgMedTilOgMed(aktuellDato, f.getPeriode().getTomDato()))));

        return fagsakÅrsakMap;
    }


    private Person hentPersonInformasjon(AktørId aktør) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktør.getAktørId());
        var projection = new PersonResponseProjection()
            .doedsfall(new DoedsfallResponseProjection().doedsdato())
            .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsRolle()
                .relatertPersonsIdent().minRolleForPerson());
        return pdlKlient.hentPerson(query, projection);
    }

    private boolean deltarIProgramPåHendelsedato(Fagsak fagsak, LocalDate relevantDato, String hendelseId) {
        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
            return false;
        }

        Behandling behandling = behandlingOpt.get();
        var periodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId());
        if (periodeGrunnlag.isPresent()) {
            var harIngenPerioderEtterHendelseDato = periodeGrunnlag.get().getUngdomsprogramPerioder().getPerioder().stream().noneMatch(p -> p.getPeriode().getTomDato().isAfter(relevantDato));
            if (harIngenPerioderEtterHendelseDato) {
                logger.info("Datagrunnlag på behandling {} for {} hadde ingen perioder med ungdomsprogram etter hendelsedato. Trigget av hendelse {}.", behandling.getUuid(), fagsak.getSaksnummer(), hendelseId);
                return false;
            }
        }
        return true;
    }

    private LocalDate finnAktuellDato(Person personFraPdl) {
        return personFraPdl.getDoedsfall().stream()
            .map(Doedsfall::getDoedsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
    }


    /**
     * idempotens-sjekk for å hindre at det opprettes flere revurderinger fra samme hendelse.
     * hindrer også revurdering hvis hendelsen kommer etter at behandlingen er oppdatert med ny data.
     */
    private boolean erNyInformasjonIHendelsen(Fagsak fagsak, AktørId aktør, LocalDate dødsdato, String hendelseId) {
        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            logger.info("Det er ingen behandling på fagsak. Ignorer hendelse");
            return false;
        }

        Behandling behandling = behandlingOpt.get();
        PersonopplysningGrunnlagEntitet personopplysninger = personopplysningRepository.hentPersonopplysninger(behandling.getId());
        if (personopplysninger != null) {
            for (PersonopplysningEntitet personopplysning : personopplysninger.getGjeldendeVersjon().getPersonopplysninger()) {
                if (aktør.equals(personopplysning.getAktørId()) && Objects.equals(dødsdato, personopplysning.getDødsdato())) {
                    logger.info("Persondata på behandling {} for {} var allerede oppdatert med riktig dødsdato. Trigget av hendelse {}.", behandling.getUuid(), fagsak.getSaksnummer(), hendelseId);
                    return false;
                }
            }
        }
        return true;
    }

}
