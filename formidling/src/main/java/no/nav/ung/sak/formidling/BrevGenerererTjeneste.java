package no.nav.ung.sak.formidling;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsPerioder;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.domene.PdlPerson;
import no.nav.ung.sak.formidling.dto.Brevbestilling;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateInput;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;
import no.nav.ung.sak.formidling.template.dto.felles.MottakerDto;
import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.GbeløpPeriodeDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.InnvilgelseDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.ResultatFlaggDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.SatserDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.TilkjentPeriodeDto;
import no.nav.ung.sak.typer.AktørId;

@ApplicationScoped
public class BrevGenerererTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BrevGenerererTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private PersonBasisTjeneste personBasisTjeneste;
    private AktørTjeneste aktørTjeneste;
    private PdfGenKlient pdfGen;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public BrevGenerererTjeneste(
        BehandlingRepository behandlingRepository,
        PersonBasisTjeneste personBasisTjeneste,
        AktørTjeneste aktørTjeneste,
        PdfGenKlient pdfGen,
        UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.behandlingRepository = behandlingRepository;
        this.personBasisTjeneste = personBasisTjeneste;
        this.aktørTjeneste = aktørTjeneste;
        this.pdfGen = pdfGen;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    public BrevGenerererTjeneste() {
    }

    public GenerertBrev genererVedtaksbrev(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (!behandling.erAvsluttet()) {
            throw new IllegalStateException("Behandling må være avsluttet for å kunne bestille vedtaksbrev");
        }
        //TODO utled innvilgelse basert på tilkjent ytelse
        if (behandling.getBehandlingResultatType() != BehandlingResultatType.INNVILGET) {
            LOG.warn("Støtter ikke vedtaksbrev for resultatype={}", behandling.getBehandlingResultatType());
            return null;
        }

        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler grunnlag"));
        UngdomsytelseSatsPerioder satsPerioder = ungdomsytelseGrunnlag.getSatsPerioder();



        return null;
    }

    public GenerertBrev generer(Brevbestilling brevbestilling) {
        Behandling behandling = behandlingRepository.hentBehandling(brevbestilling.behandlingId());
        var pdlMottaker = hentMottaker(behandling);

        // valider mal via regel hvis vedtaksbrev

        // lag brev json via datasamler og velg templateData
        PeriodeDto periode = new PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(260));
        var input = new TemplateInput(TemplateType.INNVILGELSE,
            new InnvilgelseDto(
                FellesDto.automatisk(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr())),
                new ResultatFlaggDto(true,
                    true,
                    true,
                    false,
                    false,
                    false),
                LocalDate.now(),
                500,
                Set.of(
                    new TilkjentPeriodeDto(
                        periode,
                        400,
                        BigDecimal.valueOf(4.22),
                        200000,
                        100000
                    )
                ),
                Set.of(
                    new GbeløpPeriodeDto(periode, 200000)
                ),
                new SatserDto(30, 20, 10, 20)));


        // konverter til pdf fra templateData
        PdfGenDokument dokument = pdfGen.lagDokument(input);

        return new GenerertBrev(
            dokument,
            pdlMottaker,
            pdlMottaker,
            brevbestilling.malType(),
            input.templateType()
        );
    }

    private PdlPerson hentMottaker(Behandling behandling) {
        AktørId aktørId = behandling.getFagsak().getAktørId();
        var personIdent = aktørTjeneste.hentPersonIdentForAktørId(aktørId)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke person med aktørid"));
        PersoninfoBasis personinfoBasis = personBasisTjeneste.hentBasisPersoninfo(aktørId, personIdent);

        String fnr = personIdent.getIdent();
        Objects.requireNonNull(fnr);
        return new PdlPerson(fnr, aktørId, personinfoBasis.getNavn());
    }


}

